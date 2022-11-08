package net.vpg.apex.core;

import net.vpg.apex.Util;
import net.vpg.apex.clip.SoftMixingClip;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import java.io.File;

public class Track {
    public static final AudioFormat AUDIO_FORMAT = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 48000, 16, 2, 4, 48000, false);
    public static final SoftMixingClip CLIP = new SoftMixingClip();
    private final TrackInfo info;

    public Track(TrackInfo trackInfo) {
        this.info = trackInfo;
    }

    public static Track get(File file) {
        return TrackInfo.get(file).getTrack();
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

    public int getLoopStart() {
        return info.getLoopStart();
    }

    public int getLoopEnd() {
        return info.getLoopEnd();
    }

    public TrackInfo getTrackInfo() {
        return info;
    }

    public void play() {
        Util.run(() -> CLIP.open(AudioSystem.getAudioInputStream(AUDIO_FORMAT, AudioSystem.getAudioInputStream(getFile()))));
        CLIP.setLoopPoints(getLoopStart(), getLoopEnd());
        CLIP.loop(Clip.LOOP_CONTINUOUSLY);
        CLIP.start();
    }
}
