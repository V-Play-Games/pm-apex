package net.vpg.apex.clip;

import net.vpg.apex.Util;

import javax.sound.sampled.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SoftMixingClip implements Clip {
    private final Object control_mutex = new Object();
    private final List<LineListener> listeners = new ArrayList<>();
    protected SourceDataLine sourceDataLine;
    private AudioFormat format;
    private byte[] data;
    private int framePosition = 0;
    private int loopStart = 0;
    private int loopEnd = -1;
    private int loopCount = 0;
    private int frameSize;
    private boolean open = false;
    private boolean active = false;
    private Thread thread;

    private void sendEvent(LineEvent event) {
        listeners.forEach(listener -> listener.update(event));
    }

    @Override
    public void addLineListener(LineListener listener) {
        synchronized (control_mutex) {
            listeners.add(listener);
        }
    }

    @Override
    public void removeLineListener(LineListener listener) {
        synchronized (control_mutex) {
            listeners.remove(listener);
        }
    }

    @Override
    public Control getControl(Control.Type control) {
        return null;
    }

    @Override
    public Control[] getControls() {
        return null;
    }

    @Override
    public boolean isControlSupported(Control.Type control) {
        return false;
    }

    @Override
    public int getFrameLength() {
        return data.length / frameSize;
    }

    @Override
    public long getMicrosecondLength() {
        return (long) (getFrameLength() * 1000000.0 / getFormat().getSampleRate());
    }

    @Override
    public void loop(int count) {
        synchronized (control_mutex) {
            loopCount = count;
        }
    }

    @Override
    public void open(AudioInputStream stream) throws IOException {
        byte[] cached = Util.cache(stream);
        open(stream.getFormat(), cached, 0, cached.length);
    }

    @Override
    public void open(AudioFormat format, byte[] data, int offset, int bufferSize) {
        synchronized (control_mutex) {
            if (bufferSize % format.getFrameSize() != 0)
                throw new IllegalArgumentException(String.format("Buffer size (%d) does not represent an integral number of sample frames (%d)", bufferSize, frameSize));
            this.data = Arrays.copyOfRange(data, offset, offset + bufferSize);
            open = true;
            reset();
            if (this.format != format) {
                this.format = format;
                frameSize = format.getFrameSize();
            }
            openSourceDataLine();
        }
    }

    private void reset() {
        loopStart = 0;
        loopEnd = -1;
        framePosition = 0;
        loopCount = 0;
    }

    @Override
    public void setLoopPoints(int start, int end) {
        synchronized (control_mutex) {
            int frameLength = getFrameLength();
            if (end == AudioSystem.NOT_SPECIFIED || end > frameLength)
                end = frameLength;
            if (end < start)
                throw new IllegalArgumentException("Invalid loop points: " + start + " - " + end);
            loopStart = start;
            loopEnd = end;
        }
    }

    @Override
    public int available() {
        return 0;
    }

    @Override
    public void drain() {
    }

    @Override
    public void flush() {
    }

    @Override
    public int getBufferSize() {
        return data.length;
    }

    @Override
    public AudioFormat getFormat() {
        return format;
    }

    @Override
    public int getFramePosition() {
        return framePosition;
    }

    @Override
    public void setFramePosition(int frames) {
        synchronized (control_mutex) {
            framePosition = frames;
        }
    }

    @Override
    public float getLevel() {
        return AudioSystem.NOT_SPECIFIED;
    }

    @Override
    public long getLongFramePosition() {
        return getFramePosition();
    }

    @Override
    public long getMicrosecondPosition() {
        return (long) (getFramePosition() * 1000000.0 / getFormat().getSampleRate());
    }

    @Override
    public void setMicrosecondPosition(long microseconds) {
        setFramePosition((int) (microseconds * getFormat().getSampleRate() / 1000000.0));
    }

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public boolean isRunning() {
        return active;
    }

    @Override
    public void start() {
        if (!open || active)
            return;
        synchronized (control_mutex) {
            active = true;
            thread = new Thread(this::push, "AudioPusher");
            thread.setDaemon(true);
            thread.setPriority(Thread.MAX_PRIORITY);
            thread.start();
        }
        sendEvent(new LineEvent(this, LineEvent.Type.START, framePosition));
    }

    @Override
    public void stop() {
        if (!active)
            return;
        synchronized (control_mutex) {
            active = false;
            sourceDataLine.drain();
            Util.run(thread::join);
            thread = null;
        }
        sendEvent(new LineEvent(this, LineEvent.Type.STOP, framePosition));
    }

    @Override
    public void close() {
        if (!open)
            return;
        long pos = framePosition;
        synchronized (control_mutex) {
            data = null;
            format = null;
            open = false;
            active = false;
            frameSize = 0;
            reset();
            Util.run(thread::join);
            thread = null;
            sourceDataLine.drain();
            sourceDataLine.close();
        }
        sendEvent(new LineEvent(this, LineEvent.Type.CLOSE, pos));
    }

    @Override
    public boolean isOpen() {
        return open;
    }

    @Override
    public Line.Info getLineInfo() {
        return new DataLine.Info(SoftMixingClip.class, format);
    }

    @Override
    public void open() {
        if (data == null) {
            throw new IllegalArgumentException("Illegal call to open() in interface Clip");
        }
        open(format, data, 0, data.length);
    }

    private void openSourceDataLine() {
        if (sourceDataLine == null) {
            // Search for suitable line
            Mixer defaultMixer = AudioSystem.getMixer(null);
            sourceDataLine = Arrays.stream(defaultMixer.getSourceLineInfo())
                .filter(lineInfo -> lineInfo.getLineClass() == SourceDataLine.class)
                .map(SourceDataLine.Info.class::cast)
                .map(info -> Util.get(() -> (SourceDataLine) defaultMixer.getLine(info)))
                .findFirst()
                .orElseGet(() -> Util.get(() -> AudioSystem.getSourceDataLine(format)));
        }
        if (!sourceDataLine.isOpen()) {
            int bufferSize = (int) (format.getFrameSize() * format.getFrameRate() * 0.1);
            Util.run(() -> sourceDataLine.open(format, bufferSize));
        }
        if (!sourceDataLine.isActive()) {
            sourceDataLine.start();
        }
    }

    private void push() {
        int frameRate = (int) format.getFrameRate();
        int frameLength = getFrameLength();
        int limit = loopEnd != -1 ? loopEnd : frameLength;
        while (active) {
            int len = Math.min(limit - framePosition, frameRate);
            sourceDataLine.write(data, framePosition * frameSize, len * frameSize);
            framePosition += len;
            if (framePosition == loopEnd) {
                framePosition = loopStart;
                switch (loopCount) {
                    case 1:
                        limit = frameLength; // fall through
                    default:
                        loopCount--; // fall through
                    case LOOP_CONTINUOUSLY:
                        continue;
                }
            }
            if (framePosition == frameLength) {
                break;
            }
        }
    }
}
