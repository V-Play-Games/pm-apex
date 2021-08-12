package net.vplaygames.apex.core;

import javax.sound.sampled.Clip;

public class LoopingTrack extends Track {
    boolean loopPointsSet = false;

    public LoopingTrack(TrackInfo trackInfo) {
        super(trackInfo);
    }

    public int getLoopStart() {
        return getTrackInfo().getLoopStart();
    }

    public int getLoopEnd() {
        return getTrackInfo().getLoopEnd();
    }

    public void ensureLoopPointsSet() {
        if (!loopPointsSet) {
            try {
                getClip().setLoopPoints(getTrackInfo().getLoopStart(), getTrackInfo().getLoopEnd());
            } catch (Exception e) {
                getClip().setLoopPoints(getTrackInfo().getLoopStart(), -1);
            }
        }
    }

    @Override
    public void play() {
        ensureLoopPointsSet();
        getClip().loop(Clip.LOOP_CONTINUOUSLY);
        super.play();
    }
}
