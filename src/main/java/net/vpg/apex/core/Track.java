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
import java.util.concurrent.atomic.AtomicBoolean;

public class Track {
    public static final AudioFormat AUDIO_FORMAT = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 48000, 16, 2, 4, 48000, false);
    private static final Logger LOGGER = LoggerFactory.getLogger(Track.class);
    private static final Clip CLIP = new SoftMixingClip(new SoftMixingMixer(), new DataLine.Info(Clip.class, AUDIO_FORMAT));
    private final TrackInfo info;
    private final AtomicBoolean isCaching = new AtomicBoolean();
    private final AtomicBoolean isPlaying = new AtomicBoolean();
    private byte[] cache;
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
        return CLIP;
    }

    public void ensureCached() {
        if (cache != null) return;
        if (isCaching.get()) {
            LOGGER.info(getId() + ": CACHING_AWAIT");
        } else {
            startCaching();
            LOGGER.debug(getId() + ": CACHING_START");
            cacheTask = Apex.APEX.getCacheExecutor().submit(() -> {
                try {
                    cache = Toolkit.cache(info.getAudioInputStream(AUDIO_FORMAT));
                } catch (IOException | UnsupportedAudioFileException e) {
                    LOGGER.error("Encountered an unexpected uncaught exception:", e);
                }
                LOGGER.debug(getId() + ": CACHING_END");
                stopCaching();
            });
        }
        awaitCache();
    }

    private void awaitCache() {
        try {
            cacheTask.get();
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.error("Encountered an unexpected uncaught exception:", e.getCause());
        }
    }

    public void clearCache() {
        stopCaching();
    }

    public void stopCaching() {
        if (!isCaching.getAndSet(false) && cache != null) {
            cache = null;
            LOGGER.info(getId() + ": CACHE_CLEAR");
        }
    }

    public void startCaching() {
        isCaching.set(true);
    }

    public byte[] getCache() {
        return cache;
    }

    public void close() {
        isPlaying.set(false);
        CLIP.close();
    }

    public TrackInfo getTrackInfo() {
        return info;
    }

    public void play() {
        isPlaying.set(true);
        if (!CLIP.isOpen()) {
            ensureCached();
            if (!isPlaying.get()) return;
            try {
                CLIP.open(AUDIO_FORMAT, cache, 0, cache.length);
            } catch (LineUnavailableException e) {
                LOGGER.error("Encountered an unexpected uncaught exception:", e);
            }
        }
        CLIP.setLoopPoints(getLoopStart(), getLoopEnd());
        CLIP.loop(Clip.LOOP_CONTINUOUSLY);
        CLIP.start();
    }

    public void stop() {
        isPlaying.set(false);
        getClip().stop();
    }
}
