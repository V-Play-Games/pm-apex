package net.vpg.apex;

import net.vpg.apex.components.ApexWindow;
import net.vpg.apex.core.ApexClip;
import net.vpg.apex.core.ApexThreadFactory;
import net.vpg.apex.core.Resources;
import net.vpg.apex.core.Track;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.stream.Collectors;

import static net.vpg.apex.components.ApexControl.*;

public class Apex {
    public static final Apex APEX = new Apex();
    public static final Logger LOGGER = LoggerFactory.getLogger(Apex.class);
    private final ApexClip clip = new ApexClip();
    private final ScheduledThreadPoolExecutor mainExecutor = new ScheduledThreadPoolExecutor(2, new ApexThreadFactory("Main"));
    private List<Track> playlist = new ArrayList<>();
    private int index;
    private boolean shuffle = false;

    public static void main() {
        APEX.start();
    }

    private void start() {
        Util.lookAndFeel();
        ApexWindow.getInstance().setVisible(true);
        this.updatePlaylist();
        this.setIndex(0);
        clip.stop();
        this.update();
    }

    private void updatePlaylist() {
        playlist = Resources.getInstance()
            .getResources()
            .values()
            .stream()
            .filter(f -> f.getName().endsWith(".ogg"))
            .map(Track::of)
            .sorted(Comparator.comparing(Track::id))
            .collect(Collectors.toList());
        updateListModel();
    }

    private void updateListModel() {
        trackListModel.clear();
        trackListModel.addAll(playlist.stream().map(Track::name).collect(Collectors.toList()));
        trackList.setSelectedIndex(index);
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
        if (shuffle) {
            index = (int) (Math.random() * playlist.size());
        }
        modifyAndUpdateApp(playlist.get(index), index);
    }

    public ScheduledThreadPoolExecutor getMainExecutor() {
        return mainExecutor;
    }

    public void takeAction(int action) {
        mainExecutor.execute(() -> takeAction0(action));
    }

    private void takeAction0(int action) {
        Track track = getCurrentTrack();
        switch (action) {
            case 0: // Next
                setIndex(index + 1);
                break;
            case 1: // Previous
                setIndex(index - 1);
                break;
            case 2: // Shuffle
                shuffle = !shuffle;
                shuffleButton.setText("Shuffle " + (shuffle ? "ON" : "OFF"));
                break;
            case 3: // Stop
                clip.stop();
                break;
            case 4: // Pause/Play
                clip.togglePlayPause();
                break;
            case 5: // Search
                if (!searchAndPlay(index + 1, playlist.size()))
                    searchAndPlay(0, index);
                break;
            case 6: // Update
                updatePlaylist();
                index = playlist.indexOf(track);
                break;
            case 7: // Mouse Double-click/Enter on the playlist
                setIndex(trackList.getSelectedIndex());
                break;
        }
        this.update();
    }

    public boolean searchAndPlay(int start, int end) {
        String searchText = searchTextArea.getText().toLowerCase().replaceAll("\n", "");
        for (int i = start; i < end; i++) {
            Track t = playlist.get(i);
            if ((t.id() + t.name().toLowerCase()).contains(searchText)) {
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
        Util.run(() -> clip.play(track));
        trackList.setSelectedIndex(index);
        updateScrollBar();
        trackName.setText(track.name());
        trackId.setText(track.id());
    }

    public void update() {
        trackIndex.setText(STR."Track \{index + 1}/\{playlist.size()}");
        next.setEnabled(shuffle || index != playlist.size() - 1);
        previous.setEnabled(shuffle || index != 0);
        stop.setEnabled(!clip.isStopped());
        playPause.setText(clip.isPlaying() ? "Pause" : "Play");
        playPause.setToolTipText(clip.isPlaying() ? "Pause the track" : "Play the track");
    }

    private void updateScrollBar() {
        JScrollBar scrollBar = trackListPane.getVerticalScrollBar();
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
