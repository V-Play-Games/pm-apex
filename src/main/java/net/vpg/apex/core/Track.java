package net.vpg.apex.core;

import net.vpg.apex.Util;
import net.vpg.apex.clip.SoftMixingClip;
import net.vpg.apex.clip.SoftMixingMixer;
import net.vpg.apex.clip.Toolkit;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;

public class Track {
    public static final AudioFormat audioFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 48000, 16, 2, 4, 48000, false);
    private static final Clip clip = new SoftMixingClip(new SoftMixingMixer(), new DataLine.Info(Clip.class, audioFormat));
    private final TrackInfo info;
    private byte[] cache;
    private volatile boolean isCaching;

    public Track(TrackInfo trackInfo) {
        this.info = trackInfo;
    }

    public static Track get(File file) {
        TrackInfo trackInfo = TrackInfo.get(file);
        if (trackInfo.getLoopStart() != 0) {
            return loop(trackInfo);
        }
        return simple(trackInfo);
    }

    public static Track simple(TrackInfo trackInfo) {
        return new Track(trackInfo);
    }

    public static LoopingTrack loop(TrackInfo trackInfo) {
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

    public Clip getClip() {
        ensureCached();
        return clip.isOpen() ? clip : Util.apply(clip, clip -> clip.open(audioFormat, cache, 0, cache.length));
    }

    public void ensureCached() {
        if (cache == null) {
            if (isCaching) {
                System.out.println("Waiting " + getId());
                int maxWait = 5, current = 0;
                while (isCaching) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        // ignore
                    }
                    current++;
                    if (maxWait == current) {
                        isCaching = false;
                        ensureCached();
                        return;
                    }
                }
            } else {
                try {
                    startCaching();
                    System.out.println("Caching " + getId());
                    cache = Toolkit.cache(getAudioInputStream(info.getFile()));
                    stopCaching();
                } catch (IOException | UnsupportedAudioFileException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public void clearCache() {
        if (cache != null) {
            cache = null;
            System.out.println("Cache Cleared! " + getId());
        }
    }

    public void startCaching() {
        isCaching = true;
    }

    public void stopCaching() {
        isCaching = false;
    }

    public byte[] getCache() {
        return cache;
    }

    public void close() {
        clip.close();
    }

    public TrackInfo getTrackInfo() {
        return info;
    }

    public void play() {
        getClip().start();
    }

    public void stop() {
        getClip().stop();
    }
}
