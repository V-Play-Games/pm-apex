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

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class PlayerPanel extends JPanel {
    private static final PlayerPanel instance = new PlayerPanel();

    private PlayerPanel() {
        this.setName("Player");
        this.setBorder(new EmptyBorder(15, 15, 0, 15));
        this.setLayout(new BorderLayout());
        Util.addBox(this, "North",
            ApexControl.trackListPane,
            Box.createHorizontalStrut(10),
            ApexControl.trackName,
            Box.createVerticalStrut(5),
            ApexControl.trackId,
            Box.createVerticalStrut(5),
            ApexControl.trackIndex,
            Box.createVerticalStrut(5));
    }

    public static PlayerPanel getInstance() {
        return instance;
    }
}
