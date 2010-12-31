package net.vpg.apex.core;

import net.vpg.apex.Apex;
import net.vpg.apex.clip.SoftMixingClip;
import net.vpg.apex.clip.SoftMixingMixer;
import net.vpg.apex.clip.Toolkit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class Track {
    public static final AudioFormat audioFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 48000, 16, 2, 4, 48000, false);
    private static final Logger logger = LoggerFactory.getLogger(Track.class);
    private static final Clip clip = new SoftMixingClip(new SoftMixingMixer(), new DataLine.Info(Clip.class, audioFormat));
    private final TrackInfo info;
    private byte[] cache;
    private volatile boolean isCaching;
    private volatile Future<?> cacheTask;

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

    public Clip getClip() {
        return clip;
    }

    public void ensureCached() {
        if (cache != null) return;
        if (isCaching) {
            logger.info(getId() + ": CACHING_AWAIT");
        } else {
            startCaching();
            logger.debug(getId() + ": CACHING_START");
            cacheTask = Apex.APEX.getExecutor().submit(() -> {
                try {
                    cache = Toolkit.cache(info.getAudioInputStream(audioFormat));
                } catch (IOException | UnsupportedAudioFileException e) {
                    throw new RuntimeException(e);
                }
                stopCaching();
                logger.info(getId() + ": CACHING_END");
            });
        }
        awaitCache();
    }

    private void awaitCache() {
        try {
            cacheTask.get();
        } catch (InterruptedException | ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException)
                throw (RuntimeException) cause;
            else
                throw new RuntimeException(cause);
        }
    }

    public void clearCache() {
        if (cache != null) {
            cache = null;
            logger.info(getId() + ": CACHE_CLEAR");
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
        if (!clip.isOpen()) {
            ensureCached();
            try {
                clip.open(audioFormat, cache, 0, cache.length);
            } catch (LineUnavailableException e) {
                throw new RuntimeException(e);
            }
        }
        clip.setLoopPoints(getLoopStart(), getLoopEnd());
        clip.loop(Clip.LOOP_CONTINUOUSLY);
        clip.start();
    }

    public void stop() {
        getClip().stop();
    }
}
