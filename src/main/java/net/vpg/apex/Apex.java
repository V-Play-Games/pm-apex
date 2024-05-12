/*
 * Copyright 2021 Vaibhav Nargwani
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.vpg.apex;

import net.vpg.apex.components.ApexWindow;
import net.vpg.apex.core.ApexClip;
import net.vpg.apex.core.Track;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class Apex {
    public static final Apex APEX = new Apex();
    public static final Executor EXECUTOR = Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("Apex ", 0).factory());
    private ApexWindow window;
    private final ApexClip clip = new ApexClip();
    private List<Track> playlist = new ArrayList<>();
    private int index;
    private boolean shuffle;

    public static void main() throws Exception {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        APEX.window = new ApexWindow();
        APEX.window.setVisible(true);
        APEX.updatePlaylist();
        APEX.setIndex(0);
        APEX.clip.stop();
        APEX.update();
    }

    private void updatePlaylist() {
        playlist = Track.entries.values()
            .stream()
            .sorted(Comparator.comparing(Track::id))
            .toList();
        window.trackListModel.clear();
        window.trackListModel.addAll(playlist.stream().map(Track::name).collect(Collectors.toList()));
        window.trackList.setSelectedIndex(index);
        Util.sleep(200);
        updateScrollBar();
    }

    public void setIndex(int index) {
        if (shuffle) {
            index = (int) (Math.random() * playlist.size());
        }
        modifyAndUpdateApp(playlist.get(index), index);
    }

    public void takeAction(int action) {
        EXECUTOR.execute(() -> takeAction0(action));
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
                window.shuffleButton.setText("Shuffle " + (shuffle ? "ON" : "OFF"));
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
                window.searchTextArea.setText("");
                break;
            case 6: // Update
                updatePlaylist();
                index = playlist.indexOf(track);
                break;
            case 7: // Mouse Double-click/Enter on the playlist
                setIndex(window.trackList.getSelectedIndex());
                break;
        }
        this.update();
    }

    public boolean searchAndPlay(int start, int end) {
        String searchText = window.searchTextArea.getText().toLowerCase().replaceAll("\n", "");
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
        window.trackList.setSelectedIndex(index);
        updateScrollBar();
        window.trackName.setText(STR."NOW PLAYING: \{track.name()} (\{index + 1}/\{playlist.size()})");
    }

    public void update() {
        window.next.setEnabled(shuffle || index != playlist.size() - 1);
        window.previous.setEnabled(shuffle || index != 0);
        window.stop.setEnabled(!clip.isStopped());
        window.playPause.setText(clip.isPlaying() ? "Pause" : "Play");
    }

    private void updateScrollBar() {
        JScrollBar scrollBar = window.trackListPane.getVerticalScrollBar();
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
