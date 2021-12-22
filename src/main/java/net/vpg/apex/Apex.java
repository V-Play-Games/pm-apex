package net.vpg.apex;

import net.vpg.apex.components.ApexControl;
import net.vpg.apex.components.ApexWindow;
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
    ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(16);

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
        playlist = Resources.getResources()
            .values()
            .stream()
            .filter(f -> f.getName().endsWith(".ogg"))
            .map(Track::get)
            .sorted(Comparator.comparing(Track::getId))
            .collect(Collectors.toList());
    }

    public List<Track> getPlaylist() {
        return playlist;
    }

    public int getIndex() {
        return index;
    }

    public void takeAction(int action) {
        synchronized (this) {
            Track track = getPlaylist().get(getIndex());
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
                    ApexControl.playing = track.getClip().isActive();
                    if (ApexControl.playing) {
                        track.stop();
                    } else {
                        track.play();
                        ApexControl.stopped = false;
                    }
                    ApexControl.playing = !ApexControl.playing;
                    break;
                case 5:
                    String searchText = ApexControl.searchTextArea.getText().toLowerCase();
                    for (int i = 0; i < playlist.size(); i++) {
                        Track t = playlist.get(i);
                        if (t.getName().toLowerCase().contains(searchText) || t.getId().contains(searchText)) {
                            changeTrack(i - index);
                            break;
                        }
                    }
                    break;
                case 6:
                    updatePlaylist();
                    index = playlist.indexOf(track);
            }
            ApexControl.update();
        }
    }

    public void stopCurrentTrack() {
        getPlaylist().get(getIndex()).close();
    }

    public void changeTrack(int change) {
        stopCurrentTrack();
        Track current = playlist.get(index += change);
        current.play();
        ApexControl.trackName.setText(current.getName());
        ApexControl.trackDescription.setText(current.getDescription());
        ApexControl.trackId.setText(current.getId());
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
