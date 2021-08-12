package net.vplaygames.apex;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;

public class Track {
    public static final AudioFormat audioFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 48000, 16, 2, 4, 48000, false);
    private static final Clip clip = Util.get(() -> (Clip) AudioSystem.getLine(new DataLine.Info(Clip.class, audioFormat)));
    private final Info info;
    private AudioInputStream audioInputStream;

    public Track(Info trackInfo) {
        this.info = trackInfo;
    }

    public static Track get(File file) {
        Info trackInfo = Info.get(file);
        if (trackInfo.getLoopStart() != 0 && trackInfo.getLoopEnd() != 0) {
            return loop(trackInfo);
        }
        return simple(trackInfo);
    }

    public static Track simple(Info trackInfo) {
        return new Track(trackInfo);
    }

    public static LoopingTrack loop(Info trackInfo) {
        return new LoopingTrack(trackInfo);
    }

    private static AudioInputStream getAudioInputStream(File file) throws IOException, UnsupportedAudioFileException {
        return AudioSystem.getAudioInputStream(audioFormat, AudioSystem.getAudioInputStream(file));
    }

    public String getName() {
        return info.getName();
    }

    public String getDescription() {
        return info.getDescription();
    }

    public String getId() {
        return info.getId();
    }

    public File getFile() {
        return info.getFile();
    }

    public AudioInputStream getAudioInputStream() {
        try {
            return audioInputStream == null ? audioInputStream = getAudioInputStream(info.getFile()) : audioInputStream;
        } catch (IOException | UnsupportedAudioFileException e) {
            throw new RuntimeException(e);
        }
    }

    public Clip getClip() {
        return clip.isOpen() ? clip : Util.apply(clip, clip -> clip.open(getAudioInputStream()));
    }

    public void close() {
        if (audioInputStream != null) {
            try {
                audioInputStream.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        audioInputStream = null;
        clip.close();
    }

    public Info getTrackInfo() {
        return info;
    }

    public void play() {
        getClip().start();
    }

    public void stop() {
        getClip().stop();
    }
}