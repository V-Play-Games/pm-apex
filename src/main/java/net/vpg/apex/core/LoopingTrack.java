package net.vpg.apex.core;

import javax.sound.sampled.Clip;

public class LoopingTrack extends Track {
    public LoopingTrack(TrackInfo trackInfo) {
        super(trackInfo);
    }

    public int getLoopStart() {
        return getTrackInfo().getLoopStart();
    }

    public int getLoopEnd() {
        return getTrackInfo().getLoopEnd();
    }

    @Override
    public void play() {
        getClip().setLoopPoints(getTrackInfo().getLoopStart(), getTrackInfo().getLoopEnd());
        getClip().loop(Clip.LOOP_CONTINUOUSLY);
        super.play();
    }
}
