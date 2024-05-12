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
import net.vpg.apex.Util;
import net.vpg.apex.core.Resources;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import static net.vpg.apex.Apex.APEX;

public class ApexWindow extends JFrame {
    public final JTextArea trackName;
    public final JTextArea searchTextArea;
    public final JButton next;
    public final JButton previous;
    public final JButton shuffleButton;
    public final JButton playPause;
    public final JButton stop;
    public final JButton search;
    public final DefaultListModel<String> trackListModel;
    public final JList<String> trackList;
    public final JScrollPane trackListPane;

    public ApexWindow() {
        trackName = createTextArea("Loading...");
        trackName.setToolTipText("Track Name");

        searchTextArea = Util.apply(new JTextArea("Search and Play"),
            search -> search.addKeyListener(new KeyAdapter() {
                public void keyTyped(KeyEvent e) {
                    if (e.getKeyChar() == '\n')
                        Apex.APEX.takeAction(5);
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

        createMainFrame();
    }

    private void createMainFrame() {
        add(Util.apply(new JTabbedPane(),
            pane -> pane.add(playerPanel()),
            pane -> pane.add(creditsPanel()))
        );
        addBox(this, "South",
            Util.apply(new JPanel(),
                panel -> panel.setLayout(new FlowLayout(FlowLayout.CENTER)),
                panel -> panel.add(searchTextArea),
                panel -> panel.add(search)),
            Box.createVerticalStrut(5),
            Util.apply(new JPanel(),
                panel -> panel.setLayout(new FlowLayout(FlowLayout.CENTER)),
                panel -> panel.add(shuffleButton),
                panel -> panel.add(previous),
                panel -> panel.add(stop),
                panel -> panel.add(playPause),
                panel -> panel.add(next))
        );
        setTitle("PM APEX");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(true);
        setMinimumSize(new Dimension(500, 400));
        setIconImage(Resources.get("icon.png", ImageIO::read));
        pack();
    }

    private JPanel playerPanel() {
        return createPanel("Player",
            trackListPane,
            Box.createVerticalStrut(10),
            trackName,
            Box.createVerticalStrut(5)
        );
    }

    private JPanel creditsPanel() {
        return createPanel("Credits and Info",
            createTextArea("Welcome to Pokemon Masters Audio Player EX, PM APEX in short."),
            Box.createVerticalStrut(10),
            createTextArea("""
                This is an application made for playing audio tracks from Pokemon Masters.
                It also has looping support, which means you can loop your favourite battle theme for as long as you want!
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
            panel -> addBox(panel, "North", components)
        );
    }

    private JButton makeButton(String name, String toolTip, int action) {
        JButton button = new JButton(name);
        button.setToolTipText(toolTip);
        button.addActionListener(_ -> APEX.takeAction(action));
        return button;
    }

    private void addBox(Container container, String constraints, Component... components) {
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
