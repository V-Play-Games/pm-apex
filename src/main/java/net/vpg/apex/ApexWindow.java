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

import net.vpg.apex.core.Resources;
import net.vpg.apex.core.ApexTrack;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.stream.Collectors;

import static net.vpg.apex.Apex.Action.*;

public class ApexWindow extends JFrame {
    public final JTextArea trackName;
    public final JTextArea searchTextArea;
    public final JComboBox<String> categories;
    public final JButton next;
    public final JButton previous;
    public final JButton shuffleButton;
    public final JButton playPause;
    public final JButton stop;
    public final JButton search;
    public final JList<String> trackList;
    public final JScrollPane trackListPane;
    public final JTextArea progress;
    public final JSlider seekBar;
    private final Apex apex;

    public ApexWindow(Apex apex) {
        this.apex = apex;

        trackName = createTextArea("Double-click on a track to get started!");
        trackName.setToolTipText("Track Name");

        searchTextArea = Util.apply(new JTextArea(1, 10),
            search -> search.addKeyListener(new KeyAdapter() {
                public void keyTyped(KeyEvent e) {
                    if (e.getKeyChar() == '\n')
                        apex.takeAction(SEARCH);
                }
            }),
            search -> search.setAlignmentX(0),
            search -> search.setBorder(new EmptyBorder(5, 5, 0, 5)),
            search -> search.setEditable(true),
            search -> search.setLineWrap(false),
            search -> search.setFont(new JLabel().getFont()),
            search -> search.setFocusable(true),
            search -> search.setRows(0)
        );

        next = createButton("Next Track", "Go to the next track", NEXT, false);
        previous = createButton("Previous Track", "Go to the previous track", PREVIOUS, false);
        shuffleButton = createButton("Shuffle OFF", "Shuffle the playlist", SHUFFLE, true);
        stop = createButton("Stop", "Stop the track", STOP, false);
        playPause = createButton("Play", "Play the track", PLAY_PAUSE, false);
        search = createButton("Search", "Search a track", SEARCH, true);

        List<String> categoriesList = ApexTrack.entries.values()
            .stream()
            .map(ApexTrack::category)
            .distinct()
            .sorted()
            .collect(Collectors.toList());
        categoriesList.addFirst("-- Select a category --");
        categories = new JComboBox<>(categoriesList.toArray(String[]::new));
        categories.addItemListener(_ -> apex.takeAction(UPDATE_CATEGORY));

        trackList = new JList<>();
        trackList.setVisibleRowCount(7);
        trackList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1) {
                    apex.takeAction(CLICK_ON_PLAYLIST);
                }
            }
        });
        trackList.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                if (e.getKeyChar() == '\n') {
                    apex.takeAction(CLICK_ON_PLAYLIST);
                }
            }
        });
        trackListPane = new JScrollPane(trackList);

        seekBar = new JSlider(SwingConstants.HORIZONTAL, 0, 10000, 0);
        seekBar.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                apex.takeAction(PROGRESS_SEEK);
            }
        });
        seekBar.setEnabled(false);
        progress = createTextArea("--:--/--:--");

        createMainFrame();
    }

    private void createMainFrame() {
        add(Util.apply(new JTabbedPane(),
            pane -> pane.add(createPlayerPanel()),
            pane -> pane.add(createCreditsPanel()))
        );
        createBox(this, "South",
            createPanel(searchTextArea, search),
            Box.createVerticalStrut(5),
            createPanel(shuffleButton, previous, stop, playPause, next)
        );
        setTitle("PM APEX");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(true);
        setMinimumSize(new Dimension(500, 360));
        setIconImage(Resources.get("icon.png", ImageIO::read));
        pack();
    }

    private JPanel createPlayerPanel() {
        return Util.apply(createPanel("Player",
                categories,
                Box.createVerticalStrut(5),
                trackListPane,
                Box.createVerticalStrut(10)),
            panel -> createBox(panel, "Center", trackName),
            panel -> createBox(panel, "West", progress),
            panel -> createBox(panel, "South", seekBar));
    }

    private JPanel createCreditsPanel() {
        return createPanel("Credits and Info",
            createTextArea("Welcome to Pokemon Masters Audio Player EX, PM APEX in short."),
            Box.createVerticalStrut(10),
            createTextArea("""
                This is an application made for playing audio tracks from Pokemon Masters.
                It also has looping support, so go loop your favourite battle theme for as long as you want!
                Although you can't download tracks right now, you can play them online!
                Have Fun!
                """),
            Box.createVerticalStrut(20),
            Util.apply(createTextArea("Credits"),
                textArea -> textArea.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14))
            ),
            Box.createVerticalStrut(5),
            createTextArea("V Play Games#9783 - The Author and Maintainer of this project"),
            Box.createVerticalStrut(3),
            createTextArea("Trilarion (GitHub) - For Providing OGG File Support"),
            Box.createVerticalStrut(3),
            createTextArea("Made with Java, Built with Maven 3")
        );
    }

    private JPanel createPanel(String name, Component... components) {
        return Util.apply(new JPanel(),
            panel -> panel.setName(name),
            panel -> panel.setBorder(new EmptyBorder(15, 15, 0, 15)),
            panel -> panel.setLayout(new BorderLayout()),
            panel -> createBox(panel, "North", components)
        );
    }

    private JPanel createPanel(Component... components) {
        return Util.apply(new JPanel(),
            panel -> panel.setLayout(new FlowLayout(FlowLayout.CENTER)),
            panel -> {
                for (Component component : components)
                    panel.add(component);
            }
        );
    }

    private JButton createButton(String name, String toolTip, int action, boolean enabled) {
        JButton button = new JButton(name);
        button.setToolTipText(toolTip);
        button.addActionListener(_ -> apex.takeAction(action));
        button.setEnabled(enabled);
        return button;
    }

    private void createBox(Container container, String constraints, Component... components) {
        Box box = Box.createVerticalBox();
        container.add(box, constraints);
        for (Component component : components)
            box.add(component);
    }

    public JTextArea createTextArea(String text) {
        JTextArea textArea = new JTextArea() {
            public void paintComponent(Graphics graphics) {
                setBackground(getParent().getBackground());
                super.paintComponent(graphics);
            }
        };
        textArea.setText(text);
        textArea.setAlignmentX(0);
        textArea.setBorder(new EmptyBorder(0, 0, 0, 0));
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setFont(new JLabel().getFont());
        textArea.setFocusable(false);
        textArea.setRows(0);
        textArea.invalidate();
        return textArea;
    }
}
