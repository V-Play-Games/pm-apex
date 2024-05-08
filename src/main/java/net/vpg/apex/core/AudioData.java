package net.vpg.apex.core;

import net.vpg.apex.Apex;
import net.vpg.apex.Util;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;

public class AudioData {
    private final int frameLength;
    private final AudioFormat format;
    private final byte[] data;
    private AudioInputStream stream;
    private int readPos;
    private int cachedPos;
    private boolean caching;

    public AudioData(File file, int frameLength) throws UnsupportedAudioFileException, IOException {
        this(AudioSystem.getAudioInputStream(file), frameLength);
    }

    public AudioData(URL url, int frameLength) throws UnsupportedAudioFileException, IOException {
        this(AudioSystem.getAudioInputStream(url), frameLength);
    }

    public AudioData(AudioInputStream sourceStream, int frameLength) {
        AudioFormat sourceFormat = sourceStream.getFormat();
        format = new AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED,
            sourceFormat.getSampleRate(),
            16,
            sourceFormat.getChannels(),
            4,
            sourceFormat.getSampleRate(), // Note: Keep Sample Rate = Frame Rate
            sourceFormat.isBigEndian()
        );
        stream = AudioSystem.getAudioInputStream(format, sourceStream);
        data = new byte[frameLength * format.getFrameSize()];
        this.frameLength = frameLength;
    }

    public int getFrameLength() {
        return frameLength;
    }

    public AudioFormat getFormat() {
        return format;
    }

    public int getReadPos() {
        return readPos;
    }

    public void setReadPos(int readPos) {
        this.readPos = readPos;
    }

    public void startCaching() {
        if (caching || stream == null)
            return;
        caching = true;
        Apex.EXECUTOR.execute(() -> Util.run(this::cache));
    }

    public void stopCaching() {
        caching = false;
    }

    public byte[] readData(int frames) {
        int frameSize = format.getFrameSize();
        if (readPos + frames < cachedPos) {
            readPos += frames;
            return Arrays.copyOfRange(data, readPos * frameSize, (readPos + frames) * frameSize);
        } else if (caching) {
            Util.sleep(25);
            return readData(frames);
        }
        System.out.println("Error!");
        throw new IllegalStateException();
    }

    public void cache() throws IOException {
        int frameSize = stream.getFormat().getFrameSize();
        int off = 0, read;
        while (caching && (read = stream.read(data, off, data.length)) != -1) {
            off += read;
            cachedPos = off / frameSize;
        }
        caching = false;
        stream.close();
        stream = null;
    }
}
