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

import net.vpg.apex.Util;
import net.vpg.apex.core.Resources;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

import static net.vpg.apex.components.ApexControl.*;

public class ApexWindow extends JFrame {
    public ApexWindow() {
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

    private void addBox(Container container, String constraints, Component... components) {
        Box box = Box.createVerticalBox();
        container.add(box, constraints);
        for (Component component : components)
            box.add(component);
    }

    public static JTextArea createTextArea(String text) {
        JTextArea textArea = new JTextArea() {
            public void paintComponent(Graphics graphics) {
                setBackground(getParent().getBackground());
                super.paintComponent(graphics);
            }
        };
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
