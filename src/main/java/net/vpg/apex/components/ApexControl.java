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

package net.vpg.apex.components;

import net.vpg.apex.Apex;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import static net.vpg.apex.Apex.APEX;

public class ApexControl {
    public static final WrappedTextArea trackName;
    public static final JTextArea searchTextArea;
    public static final JButton next;
    public static final JButton previous;
    public static final JButton shuffleButton;
    public static final JButton playPause;
    public static final JButton stop;
    public static final JButton search;
    public static final DefaultListModel<String> trackListModel;
    public static final JList<String> trackList;
    public static final JScrollPane trackListPane;

    static {
        searchTextArea = new SearchTextArea("Search and Play");
        searchTextArea.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                if (e.getKeyChar() == '\n') {
                    Apex.APEX.takeAction(5);
                }
            }
        });

        next = makeButton("Next Track", "Go to the next track", 0);
        previous = makeButton("Previous Track", "Go to the previous track", 1);
        shuffleButton = makeButton("Shuffle OFF", "Shuffle the playlist", 2);
        stop = makeButton("Stop", "Stop the track", 3);
        playPause = makeButton("Play", "Play the track", 4);
        search = makeButton("Search and Play", "Search a track", 5);

        trackListModel = new DefaultListModel<>();
        trackList = new JList<>(trackListModel);
        trackList.setVisibleRowCount(7);
        trackList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1) {
                    Apex.APEX.takeAction(7);
                }
            }
        });
        trackList.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                if (e.getKeyChar() == '\n') {
                    Apex.APEX.takeAction(7);
                }
            }
        });
        trackListPane = new JScrollPane(trackList);
    }

    private static JButton makeButton(String name, String toolTip, int action) {
        JButton button = new JButton(name);
        button.setToolTipText(toolTip);
        button.addActionListener(_ -> APEX.takeAction(action));
        return button;
    }

    private static WrappedTextArea makeTextArea(String toolTip) {
        WrappedTextArea textArea = new WrappedTextArea();
        textArea.setToolTipText(toolTip);
        return textArea;
    }
}
