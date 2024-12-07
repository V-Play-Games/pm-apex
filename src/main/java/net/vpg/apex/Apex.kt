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

import net.vpg.apex.core.ApexPlayer;
import net.vpg.apex.core.ApexPlaylist;
import net.vpg.apex.core.ApexTrack;

import javax.swing.*;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static net.vpg.apex.Apex.Action.*;

public class Apex {
    public static final Executor EXECUTOR
        = Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("Apex ", 1).factory());
    private final ApexPlayer player;
    private final ApexWindow window;
    private final Map<String, ApexPlaylist> playlists;
    private ApexPlaylist playlistPlaying;
    private ApexPlaylist playlistShown;
    private int index = -1;
    private boolean shuffle;

    public Apex() {
        Util.run(() -> UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()));
        player = new ApexPlayer();
        window = new ApexWindow(this);
        playlists = ApexTrack.entries
            .values()
            .stream()
            .sorted(Comparator.comparing(ApexTrack::id))
            .collect(Collectors.groupingBy(ApexTrack::category))
            .entrySet()
            .stream()
            .collect(Collectors.toMap(Map.Entry::getKey, e -> new ApexPlaylist(e.getKey(), e.getValue())));
        //noinspection resource
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            if (player.isPlaying()) {
                int len = player.getLength();
                if (window.seekBar.getValueIsAdjusting()) {
                    int seek = player.getLength() * window.seekBar.getValue() / 10000;
                    window.progress.setText(String.format("%02d:%02d / %02d:%02d",
                        seek / 60, seek % 60, len / 60, len % 60));
                } else {
                    int pos = player.getPosition();
                    window.seekBar.setValue(pos * 10000 / len);
                    window.progress.setText(String.format("%02d:%02d / %02d:%02d",
                        pos / 60, pos % 60, len / 60, len % 60));
                }
            }
        }, 50, 50, TimeUnit.MILLISECONDS);
    }

    public void main() {
        window.setVisible(true);
    }

    public void takeAction(int action) {
        EXECUTOR.execute(() -> takeAction0(action));
    }

    private void takeAction0(int action) {
        switch (action) {
            case NEXT -> updateTrack(index + 1, false);
            case PREVIOUS -> updateTrack(index - 1, false);
            case SHUFFLE -> window.shuffleButton.setText("Shuffle " + ((shuffle = !shuffle) ? "ON" : "OFF"));
            case PLAY_PAUSE -> player.togglePlayPause();
            case SEARCH -> search(playlistShown == playlistPlaying ? index + 1 : 0, playlistShown.size());
            case CLICK_ON_PLAYLIST -> updateTrack(window.trackList.getSelectedIndex(), true);
            case UPDATE_CATEGORY -> updatePlaylistShown(playlists.getOrDefault(
                window.categories.getSelectedItem().toString(),
                ApexPlaylist.EMPTY
            ));
            case PROGRESS_SEEK -> updateProgress();
        }
        updateButtons();
    }

    private void search(int start, int end) {
        String searchText = window.searchTextArea.getText().toLowerCase().replace("\n", "");
        for (int i = start; i < end; i++) {
            if (playlistShown.get(i).id().toLowerCase().contains(searchText)) {
                updateScrollBar(i);
                window.searchTextArea.setText("");
                return;
            }
        }
        if (start != 0) {
            search(0, start);
        }
    }

    private void updateProgress() {
        if (window.seekBar.isEnabled()) {
            player.setPosition(player.getLength() * window.seekBar.getValue() / 10000);
        }
    }

    private void updatePlaylistShown(ApexPlaylist playlist) {
        if (playlistShown == playlist)
            return;
        playlistShown = playlist;
        window.trackList.setModel(playlistShown.getModel());
        window.categories.setSelectedItem(playlistShown.getCategory());
    }

    private void updateTrack(int i, boolean playlistClick) {
        if (i < 0)
            return;
        if (playlistClick)
            playlistPlaying = playlistShown;
        else
            updatePlaylistShown(playlistPlaying);
        window.seekBar.setEnabled(false);
        window.trackName.setText("Loading...");
        index = playlistClick || !shuffle ? i : (int) (Math.random() * playlistPlaying.size());
        ApexTrack track = playlistPlaying.get(index);
        updateScrollBar(index);
        Util.run(() -> player.play(track));
        window.seekBar.setEnabled(true);
        window.trackName.setText("Now Playing: %s (%d/%d)".formatted(track.name(), index + 1, playlistPlaying.size()));
    }

    private void updateButtons() {
        if (index == -1)
            return;
        window.next.setEnabled(shuffle || index != playlistPlaying.size() - 1);
        window.previous.setEnabled(shuffle || index != 0);
        window.seekBar.setEnabled(true);
        window.playPause.setEnabled(true);
        window.playPause.setText(player.isPlaying() ? "Pause" : "Play");
    }

    private void updateScrollBar(int i) {
        window.trackList.setSelectedIndex(i);
        var scrollBar = window.trackListPane.getVerticalScrollBar();
        int rowHeight = scrollBar.getMaximum() / playlistShown.size();
        int firstVisibleIndex = scrollBar.getValue() / rowHeight;
        int visibleAmount = scrollBar.getVisibleAmount() / rowHeight;
        if (i < firstVisibleIndex) {
            scrollBar.setValue(i * rowHeight);
        } else if (i > firstVisibleIndex + visibleAmount - 1) {
            scrollBar.setValue(Math.min(i - visibleAmount + 1, playlistShown.size() - visibleAmount + 1) * rowHeight);
        }
    }

    public static class Action {
        public static final int NEXT = 1;
        public static final int PREVIOUS = 2;
        public static final int SHUFFLE = 3;
        public static final int PLAY_PAUSE = 5;
        public static final int SEARCH = 6;
        public static final int CLICK_ON_PLAYLIST = 7;
        public static final int UPDATE_CATEGORY = 8;
        public static final int PROGRESS_SEEK = 9;
    }
}
