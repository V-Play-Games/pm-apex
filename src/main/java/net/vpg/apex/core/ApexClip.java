package net.vpg.apex.core;

import net.vpg.apex.Util;

import javax.sound.sampled.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;

public class ApexClip implements Clip {
    private final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(2);
    private final Object control_mutex = new Object();
    private final List<LineListener> listeners = new ArrayList<>();
    protected SourceDataLine sourceDataLine;
    private AudioFormat format;
    private AudioInputStream stream;
    private byte[] data;
    private int framePosition = 0;
    private int loopStart = 0;
    private int loopEnd = -1;
    private int loopCount = 0;
    private int frameSize;
    private boolean open = false;
    private boolean active = false;

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
    public void open(AudioInputStream stream) {
        this.stream = stream;
        data = null;
        open0(stream.getFormat());
    }

    public void open(TrackInfo track, AudioFormat format) {
        Util.run(() -> {
            open(AudioSystem.getAudioInputStream(format, AudioSystem.getAudioInputStream(track.getFile())));
            setLoopPoints(track.getLoopStart(), track.getLoopEnd());
            loop(Clip.LOOP_CONTINUOUSLY);
        });
    }

    @Override
    public void open(AudioFormat format, byte[] data, int offset, int bufferSize) {
        synchronized (control_mutex) {
            if (bufferSize % format.getFrameSize() != 0)
                throw new IllegalArgumentException(String.format("Buffer size (%d) does not represent an integral number of sample frames (%d)", bufferSize, frameSize));
            this.data = Arrays.copyOfRange(data, offset, offset + bufferSize);
            open0(format);
        }
    }

    private void open0(AudioFormat format) {
        open = true;
        reset();
        if (this.format != format) {
            this.format = format;
            frameSize = format.getFrameSize();
        }
        openSourceDataLine();
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
            if (end != AudioSystem.NOT_SPECIFIED && end < start)
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
            executor.execute(this::push);
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
        return new DataLine.Info(ApexClip.class, format);
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
        while (active) {
            ensureRead(frameRate * frameSize);
            int frameLength = getFrameLength();
            int limit = loopEnd > frameLength || loopEnd == -1 || loopCount == 0 ? frameLength : loopEnd;
            int len = Math.min(limit - framePosition, frameRate / 2);
            sourceDataLine.write(data, framePosition * frameSize, len * frameSize);
            framePosition += len;
            if (framePosition == limit) {
                if (loopCount != 0) {
                    if (framePosition != loopEnd && stream != null) {
                        continue;
                    }
                    framePosition = loopStart;
                    if (loopCount != LOOP_CONTINUOUSLY)
                        loopCount--;
                    continue;
                }
                if (stream == null) {
                    break;
                }
            }
        }
    }

    private void ensureRead(int bytes) {
        if (stream == null) return;
        Util.run(() -> {
            byte[] buffer = new byte[bytes];
            int totalRead = 0;
            while (totalRead < bytes) {
                int read = stream.read(buffer, totalRead, bytes - totalRead);
                if (read == -1) {
                    stream = null;
                    break;
                }
                totalRead += read;
            }
            if (data == null) {
                data = buffer;
                return;
            }
            byte[] merged = new byte[data.length + totalRead];
            System.arraycopy(data, 0, merged, 0, data.length);
            System.arraycopy(buffer, 0, merged, data.length, totalRead);
            data = merged;
        });
    }
}
