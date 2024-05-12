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
    private static final ApexWindow instance = new ApexWindow();

    private ApexWindow() {
        Util.addBox(this, null,
            Util.apply(new JTabbedPane(),
                pane -> pane.add(playerPanel()),
                pane -> pane.add(creditsPanel()))
        );
        Util.addBox(this, "South",
            searchTextArea,
            Util.apply(new JPanel(),
                panel -> panel.setAlignmentX(0),
                panel -> panel.add(search)),
            Box.createVerticalStrut(5),
            Util.apply(new JPanel(),
                panel -> panel.setLayout(new FlowLayout(FlowLayout.CENTER)),
                panel -> panel.setAlignmentX(0),
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
        JPanel panel = (new JPanel());
        panel.setName("Player");
        panel.setBorder(new EmptyBorder(15, 15, 0, 15));
        panel.setLayout(new BorderLayout());
        Util.addBox(panel, "North",
            trackListPane,
            Box.createVerticalStrut(10),
            trackName,
            Box.createVerticalStrut(5));
        return panel;
    }

    private JPanel creditsPanel() {
        JPanel panel = new JPanel();
        panel.setName("Credits and Info");
        panel.setBorder(new EmptyBorder(15, 15, 0, 15));
        panel.setLayout(new BorderLayout());
        Util.addBox(panel, "North",
            new WrappedTextArea("Welcome to Pokemon Masters Audio Player EX, PM APEX in short."),
            Box.createVerticalStrut(10),
            new WrappedTextArea("""
                This is an application made for playing audio tracks from Pokemon Masters.
                It also has looping support, which means you can loop your favourite battle theme for as long as you want!
                Although you can't download tracks right now, you can play them online!
                Have Fun!
                """),
            Box.createVerticalStrut(20),
            Util.apply(new WrappedTextArea("Credits"),
                textArea -> textArea.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14))
            ),
            Box.createVerticalStrut(5),
            new WrappedTextArea("V Play Games#9783 - The Author and Maintainer of this project"),
            Box.createVerticalStrut(3),
            new WrappedTextArea("Trilarion (GitHub) - For Providing OGG File Support"),
            Box.createVerticalStrut(3),
            new WrappedTextArea("Made with Java, Built with Maven 3")
        );
        return panel;
    }

    public static ApexWindow getInstance() {
        return instance;
    }
}
