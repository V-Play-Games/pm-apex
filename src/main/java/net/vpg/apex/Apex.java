package net.vpg.apex;

import net.vpg.apex.components.ApexControl;
import net.vpg.apex.components.ApexWindow;
import net.vpg.apex.core.ApexThreadFactory;
import net.vpg.apex.core.Resources;
import net.vpg.apex.core.Track;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.stream.Collectors;

public class Apex {
    public static final Apex APEX = new Apex();
    ApexWindow window;
    List<Track> playlist = new ArrayList<>();
    int index = 0;
    ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(16, new ApexThreadFactory());

    public static void main(String[] args) throws Exception {
        APEX.start();
    }

    private void start() throws Exception {
        ApexControl.init();
        window = new ApexWindow();
        updatePlaylist();
        changeTrack(0);
        updateCaches();
        ApexControl.update();
    }

    private void updatePlaylist() {
        playlist = Resources.getInstance()
            .getResources()
            .values()
            .stream()
            .filter(f -> f.getName().endsWith(".ogg"))
            .map(Track::get)
            .sorted(Comparator.comparing(Track::getId))
            .collect(Collectors.toList());
        playlist.forEach(track -> ApexControl.trackList.addElement(track.getName()));
    }

    public List<Track> getPlaylist() {
        return playlist;
    }

    public int getIndex() {
        return index;
    }

    public ScheduledThreadPoolExecutor getExecutor() {
        return executor;
    }

    public void takeAction(int action) {
        executor.execute(() -> {
            Track track = getCurrentTrack();
            switch (action) {
                case 0:
                    changeTrack(1);
                    ApexControl.playing = true;
                    ApexControl.stopped = false;
                    updateCaches();
                    break;
                case 1:
                    changeTrack(-1);
                    ApexControl.playing = true;
                    ApexControl.stopped = false;
                    updateCaches();
                    break;
                case 2:
                    Util.shuffle(playlist);
                    index = playlist.indexOf(track);
                    updateCaches();
                    break;
                case 3:
                    stopCurrentTrack();
                    ApexControl.playing = false;
                    ApexControl.stopped = true;
                    break;
                case 4:
                    boolean active = track.getClip().isActive();
                    ApexControl.playing = !active;
                    if (active) {
                        track.stop();
                    } else {
                        track.play();
                        ApexControl.stopped = false;
                    }
                    break;
                case 5:
                    if (!searchAndPlay(index + 1, playlist.size())) {
                        searchAndPlay(0, index);
                    }
                    updateCaches();
                    break;
                case 6:
                    updatePlaylist();
                    index = playlist.indexOf(track);
                    updateCaches();
            }
            ApexControl.update();
        });
    }

    public boolean searchAndPlay(int start, int end) {
        String searchText = ApexControl.searchTextArea.getText().toLowerCase();
        for (int i = start; i < end; i++) {
            Track t = playlist.get(i);
            if (t.getName().toLowerCase().contains(searchText) || t.getId().contains(searchText)) {
                changeTrack(t, i);
                return true;
            }
        }
        return false;
    }

    public Track getCurrentTrack() {
        return playlist.get(index);
    }

    public void stopCurrentTrack() {
        getCurrentTrack().close();
    }

    public void changeTrack(int change) {
        changeTrack(playlist.get(index += change), index);
    }

    public void changeTrack(Track track) {
        changeTrack(track, playlist.indexOf(track));
    }

    public void changeTrack(Track track, int index) {
        stopCurrentTrack();
        this.index = index;
        track.play();
        ApexControl.trackName.setText(track.getName());
        ApexControl.trackDescription.setText(track.getDescription());
        ApexControl.trackId.setText(track.getId());
    }

    public void updateCaches() {
        executor.execute(() -> {
            int start = Math.max(0, index - 3);
            int end = Math.min(playlist.size(), index + 3);
            for (int i = 0; i < playlist.size(); i++) {
                Track track = playlist.get(i);
                if (start <= i && i <= end) {
                    executor.execute(track::ensureCached);
                } else {
                    executor.execute(track::clearCache);
                }
            }
        });
    }
}
