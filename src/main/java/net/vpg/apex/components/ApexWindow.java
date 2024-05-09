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

public class ApexWindow extends JFrame {
    private static final ApexWindow instance = new ApexWindow();

    private ApexWindow() {
        Box box = Util.addBox(this, null);
        box.setBorder(new EmptyBorder(3, 3, 3, 3));
        box.add(Util.apply(new JTabbedPane(),
            pane -> pane.add(PlayerPanel.getInstance()),
            pane -> pane.add(CreditsPanel.getInstance())));
        Util.addBox(this, "South",
            ApexControl.searchTextArea,
            Util.apply(new JPanel(),
                panel -> panel.setAlignmentX(0),
                panel -> panel.add(ApexControl.search)),
            Box.createVerticalStrut(5),
            Util.apply(new JPanel(),
                panel -> panel.setLayout(new FlowLayout(FlowLayout.CENTER)),
                panel -> panel.setAlignmentX(0),
                panel -> panel.add(ApexControl.shuffleButton),
                panel -> panel.add(ApexControl.previous),
                panel -> panel.add(ApexControl.stop),
                panel -> panel.add(ApexControl.playPause),
                panel -> panel.add(ApexControl.next))
        );
        this.setTitle("PM APEX");
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setLocationRelativeTo(null);
        this.setResizable(true);
        this.setMinimumSize(new Dimension(500, 400));
        this.setIconImage(Util.get(() -> ImageIO.read(Resources.get("icon.png"))));
        this.pack();
    }

    public static ApexWindow getInstance() {
        return instance;
    }
}
