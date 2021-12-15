package net.vpg.apex.components;

import net.vpg.apex.Util;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class PlayerPanel extends JPanel {
    public PlayerPanel() {
        this.setName("Player");
        this.setBorder(new EmptyBorder(new Insets(15, 15, 0, 15)));
        this.setLayout(new BorderLayout());
        Box box = Box.createVerticalBox();
        this.add(box, "North");
        box.add(ApexControl.trackName);
        box.add(Box.createVerticalStrut(5));
        box.add(ApexControl.trackDescription);
        box.add(Box.createVerticalStrut(5));
        box.add(ApexControl.trackDescription);
        box.add(Box.createVerticalStrut(5));
        box.add(ApexControl.trackId);
        box.add(Box.createVerticalStrut(5));
        box.add(ApexControl.trackIndex);
        box.add(Box.createVerticalStrut(5));
        box.add(Util.apply(new JPanel(),
            buttonPanel -> buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER)),
            buttonPanel -> buttonPanel.setAlignmentX(0),
            buttonPanel -> buttonPanel.add(ApexControl.shuffle),
            buttonPanel -> buttonPanel.add(ApexControl.next),
            buttonPanel -> buttonPanel.add(ApexControl.stop),
            buttonPanel -> buttonPanel.add(ApexControl.playPause),
            buttonPanel -> buttonPanel.add(ApexControl.previous)));
        box.add(Box.createVerticalStrut(5));
        box.add(ApexControl.searchTextArea);
        box.add(Util.apply(new JPanel(),
            buttonPanel -> buttonPanel.setAlignmentX(0),
            buttonPanel -> buttonPanel.add(ApexControl.search)));
    }
}
