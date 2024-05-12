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

import net.vpg.apex.core.ApexClip;
import net.vpg.apex.core.Track;

import javax.swing.*;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static net.vpg.apex.Apex.Action.*;

public class Apex {
    public static final Executor EXECUTOR = Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("Apex ", 0).factory());
    private final ApexClip clip = new ApexClip();
    private ApexWindow window;
    private List<Track> playlist;
    private int index;
    private boolean shuffle;

    public void main() throws Exception {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        window = new ApexWindow(this);
        window.setVisible(true);
        updatePlaylist();
        updateUI(0);
        clip.stop();
        updateButtons();
    }

    public void setIndex(int index) {
        if (shuffle) {
            index = (int) (Math.random() * playlist.size());
        }
        updateUI(index);
    }

    public void takeAction(int action) {
        EXECUTOR.execute(() -> takeAction0(action));
    }

    private void takeAction0(int action) {
        switch (action) {
            case NEXT -> setIndex(index + 1);
            case PREVIOUS -> setIndex(index - 1);
            case SHUFFLE -> {
                shuffle = !shuffle;
                window.shuffleButton.setText("Shuffle " + (shuffle ? "ON" : "OFF"));
            }
            case STOP -> clip.stop();
            case PLAY_PAUSE -> clip.togglePlayPause();
            case SEARCH -> {
                window.searchTextArea.setText("");
                if (!searchAndPlay(index + 1, playlist.size()))
                    searchAndPlay(0, index);
            }
            case CLICK_ON_PLAYLIST -> setIndex(window.trackList.getSelectedIndex());
        }
        this.updateButtons();
    }

    private boolean searchAndPlay(int start, int end) {
        String searchText = window.searchTextArea.getText().toLowerCase().replaceAll("\n", "");
        for (int i = start; i < end; i++) {
            Track t = playlist.get(i);
            if ((t.id() + t.name().toLowerCase()).contains(searchText)) {
                updateUI(i);
                return true;
            }
        }
        return false;
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

    private void updateUI(int index) {
        clip.stop();
        this.index = index;
        Track track = playlist.get(index);
        Util.run(() -> clip.play(track));
        window.trackList.setSelectedIndex(index);
        updateScrollBar();
        window.trackName.setText(STR."NOW PLAYING: \{track.name()} (\{index + 1}/\{playlist.size()})");
    }

    private void updateButtons() {
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

    public static class Action {
        public static final int NEXT = 1;
        public static final int PREVIOUS = 2;
        public static final int SHUFFLE = 3;
        public static final int STOP = 4;
        public static final int PLAY_PAUSE = 5;
        public static final int SEARCH = 6;
        public static final int CLICK_ON_PLAYLIST = 7;
    }
}
