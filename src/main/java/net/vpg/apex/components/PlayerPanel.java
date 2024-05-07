package net.vpg.apex.components;

import net.vpg.apex.Apex;
import net.vpg.apex.Util;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

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
