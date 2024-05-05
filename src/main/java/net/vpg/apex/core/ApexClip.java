package net.vpg.apex.core;

import net.vpg.apex.Util;

import javax.sound.sampled.*;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.ScheduledThreadPoolExecutor;

public class ApexClip {
    private final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(2, new ApexThreadFactory("Player"));
    private SourceDataLine sourceDataLine;
    private AudioFormat format;
    private AudioInputStream stream;
    private byte[] data;
    private int framePosition;
    private int loopStart;
    private int loopEnd;
    private int loopCount;
    private boolean playing;
    private boolean stopped = true;

    public void open(Track track, AudioFormat format) throws UnsupportedAudioFileException, IOException, LineUnavailableException {
        this.stream = AudioSystem.getAudioInputStream(format, AudioSystem.getAudioInputStream(track.getFile()));
        data = null;
        playing = true;
        stopped = false;
        if (this.format != format) {
            this.format = format;
            if (sourceDataLine != null)
                sourceDataLine.close();
            sourceDataLine = AudioSystem.getSourceDataLine(format);
            sourceDataLine.open();
        }
        loopStart = track.getLoopStart();
        loopEnd = track.getLoopEnd();
        loopCount = Clip.LOOP_CONTINUOUSLY;
    }

    public void togglePlayPause() {
        playing = !playing;
        if (playing) {
            stopped = false;
            sourceDataLine.start();
            executor.execute(this::playAudio);
        } else {
            sourceDataLine.stop();
        }
    }

    public void stop() {
        if (stopped)
            return;
        playing = false;
        stopped = true;
        sourceDataLine.stop();
    }

    public boolean isPlaying() {
        return playing;
    }

    public boolean isStopped() {
        return stopped;
    }

    public int getLoopCount() {
        return loopCount;
    }

    public void setLoopCount(int count) {
        loopCount = count;
    }

    public int getFrameLength() {
        return data.length / format.getFrameSize();
    }

    public long getMicrosecondLength() {
        return (long) (getFrameLength() * 1000000.0 / format.getSampleRate());
    }

    public int getBufferSize() {
        return data.length;
    }

    public AudioFormat getFormat() {
        return format;
    }

    public int getFramePosition() {
        return framePosition;
    }

    public void setFramePosition(int frames) {
        framePosition = frames;
    }

    public long getLongFramePosition() {
        return framePosition;
    }

    public long getMicrosecondPosition() {
        return (long) (framePosition / format.getSampleRate() * 1000000);
    }

    public void setMicrosecondPosition(long microseconds) {
        setFramePosition((int) (microseconds * format.getSampleRate() / 1000000));
    }

    private void playAudio() {
        int frameRate = (int) format.getFrameRate();
        int frameSize = format.getFrameSize();
        while (playing) {
            readAudio(frameRate * frameSize);
            int frameLength = getFrameLength();
            int limit = loopEnd > frameLength || loopEnd == -1 || loopCount == 0 ? frameLength : loopEnd;
            int len = Math.min(limit - framePosition, frameRate / 20); // push at most 50 ms of audio
            sourceDataLine.write(data, framePosition * frameSize, len * frameSize);
            framePosition += len;
            if (framePosition == limit) {
                if (loopCount != 0) {
                    if (framePosition != loopEnd && stream != null) {
                        continue;
                    }
                    framePosition = loopStart;
                    if (loopCount != Clip.LOOP_CONTINUOUSLY)
                        loopCount--;
                    continue;
                }
                if (stream == null) {
                    break;
                }
            }
        }
    }

    private void readAudio(int bytes) {
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
            data = Arrays.copyOf(data, data.length + totalRead);
            System.arraycopy(buffer, 0, data, data.length - totalRead, totalRead);
        });
    }
}
