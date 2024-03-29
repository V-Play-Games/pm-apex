package net.vpg.apex;

import net.vpg.apex.components.ApexControl;
import net.vpg.apex.components.ApexWindow;
import net.vpg.apex.core.ApexClip;
import net.vpg.apex.core.ApexThreadFactory;
import net.vpg.apex.core.Resources;
import net.vpg.apex.core.Track;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.AudioFormat;
import javax.swing.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.stream.Collectors;

public class Apex {
    public static final Apex APEX = new Apex();
    public static final Logger LOGGER = LoggerFactory.getLogger(Apex.class);
    public static final AudioFormat AUDIO_FORMAT = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 48000, 16, 2, 4, 48000, false);
    private final ApexClip clip = new ApexClip();
    private final ScheduledThreadPoolExecutor mainExecutor = new ScheduledThreadPoolExecutor(2, new ApexThreadFactory("Main"));
    private List<Track> playlist = new ArrayList<>();
    private int index = 0;

    public static void main(String[] args) {
        APEX.start();
    }

    private void start() {
        ApexControl.init();
        ApexWindow.getInstance().setVisible(true);
        this.updatePlaylist();
        this.setIndex(0);
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
        updateListModel();
    }

    private void updateListModel() {
        ApexControl.trackListModel.clear();
        ApexControl.trackListModel.addAll(playlist.stream().map(Track::getName).collect(Collectors.toList()));
        ApexControl.trackList.setSelectedIndex(index);
        Util.sleep(100);
        updateScrollBar();
    }

    public List<Track> getPlaylist() {
        return playlist;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        modifyAndUpdateApp(playlist.get(index), index);
    }

    public ScheduledThreadPoolExecutor getMainExecutor() {
        return mainExecutor;
    }

    public void takeAction(int action) {
        mainExecutor.execute(() -> {
            Track track = getCurrentTrack();
            switch (action) {
                case 0: // Next
                    setIndex(index + 1);
                    break;
                case 1: // Previous
                    setIndex(index - 1);
                    break;
                case 2: // Shuffle
                    Util.shuffle(playlist);
                    index = playlist.indexOf(track);
                    updateListModel();
                    break;
                case 3: // Stop
                    clip.stop();
                    ApexControl.playing = false;
                    ApexControl.stopped = true;
                    break;
                case 4: // Pause/Play
                    boolean active = clip.isActive();
                    ApexControl.playing = !active;
                    if (active) {
                        clip.stop();
                    } else {
                        if (ApexControl.stopped) {
                            ApexControl.stopped = false;
                            clip.open(track, AUDIO_FORMAT);
                            clip.start();
                        }
                        clip.start();
                    }
                    break;
                case 5: // Search
                    if (!searchAndPlay(index + 1, playlist.size())) {
                        searchAndPlay(0, index);
                    }
                    break;
                case 6: // Update
                    updatePlaylist();
                    index = playlist.indexOf(track);
                    break;
                case 7: // Mouse Double-click/Enter on the playlist
                    setIndex(ApexControl.trackList.getSelectedIndex());
                    break;
                case 8: // Surprise Me
                    setIndex(Util.random(0, playlist.size()));
                    break;
            }
            ApexControl.update();
        });
    }

    public boolean searchAndPlay(int start, int end) {
        String searchText = ApexControl.searchTextArea.getText().toLowerCase().replaceAll("\n", "");
        for (int i = start; i < end; i++) {
            Track t = playlist.get(i);
            if (t.getId().contains(searchText)) {
                modifyAndUpdateApp(t, i);
                return true;
            }
        }
        for (int i = start; i < end; i++) {
            Track t = playlist.get(i);
            if (t.getName().toLowerCase().contains(searchText)) {
                modifyAndUpdateApp(t, i);
                return true;
            }
        }
        return false;
    }

    public Track getCurrentTrack() {
        return playlist.get(index);
    }

    private void modifyAndUpdateApp(Track track, int index) {
        clip.stop();
        this.index = index;
        clip.open(track, AUDIO_FORMAT);
        clip.start();
        ApexControl.trackList.setSelectedIndex(index);
        updateScrollBar();
        ApexControl.trackName.setText(track.getName());
        ApexControl.trackId.setText(track.getId());
        ApexControl.playing = true;
        ApexControl.stopped = false;
    }

    private void updateScrollBar() {
        JScrollBar scrollBar = ApexControl.trackListPane.getVerticalScrollBar();
        int rowHeight = scrollBar.getMaximum() / playlist.size();
        int firstVisibleIndex = scrollBar.getValue() / rowHeight;
        int visibleAmount = scrollBar.getVisibleAmount() / rowHeight;
        if (index < firstVisibleIndex) {
            scrollBar.setValue(index * rowHeight);
        } else if (index > firstVisibleIndex + visibleAmount - 1) {
            scrollBar.setValue(Math.min(index - visibleAmount + 1, playlist.size() - visibleAmount + 1) * rowHeight);
        }
    }
}
