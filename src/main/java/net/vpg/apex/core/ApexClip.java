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

    public void play(Track track) throws UnsupportedAudioFileException, IOException, LineUnavailableException {
        AudioInputStream sourceStream = AudioSystem.getAudioInputStream(track.getFile());
        AudioFormat sourceFormat = sourceStream.getFormat();
        AudioFormat targetFormat = new AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED,
            sourceFormat.getSampleRate(),
            16,
            sourceFormat.getChannels(),
            4,
            sourceFormat.getSampleRate(), // Note: Keep Sample Rate = Frame Rate
            sourceFormat.isBigEndian()
        );
        stream = AudioSystem.getAudioInputStream(targetFormat, sourceStream);
        data = null;
        playing = true;
        stopped = false;
        framePosition = 0;
        if (format != targetFormat) {
            format = targetFormat;
            if (sourceDataLine != null)
                sourceDataLine.close();
            sourceDataLine = AudioSystem.getSourceDataLine(targetFormat);
            sourceDataLine.open();
        }
        loopStart = track.loopStart();
        loopEnd = track.loopEnd();
        loopCount = Clip.LOOP_CONTINUOUSLY;
        executor.execute(this::playAudio);
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
        framePosition = 0;
        sourceDataLine.flush();
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
