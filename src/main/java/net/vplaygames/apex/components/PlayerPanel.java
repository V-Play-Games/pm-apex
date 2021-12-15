package net.vplaygames.apex.components;

import net.vplaygames.apex.Util;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

import static net.vplaygames.apex.components.ApexControl.*;

public class PlayerPanel extends JPanel {
    public PlayerPanel() {
        this.setName("Player");
        this.setBorder(new EmptyBorder(new Insets(15, 15, 0, 15)));
        this.setLayout(new BorderLayout());
        Box box = Box.createVerticalBox();
        this.add(box, "North");
        box.add(trackName);
        box.add(Box.createVerticalStrut(5));
        box.add(trackDescription);
        box.add(Box.createVerticalStrut(5));
        box.add(trackDescription);
        box.add(Box.createVerticalStrut(5));
        box.add(trackId);
        box.add(Box.createVerticalStrut(5));
        box.add(trackIndex);
        box.add(Box.createVerticalStrut(5));
        box.add(Util.apply(new JPanel(),
            buttonPanel -> buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER)),
            buttonPanel -> buttonPanel.setAlignmentX(0),
            buttonPanel -> buttonPanel.add(shuffle),
            buttonPanel -> buttonPanel.add(next),
            buttonPanel -> buttonPanel.add(stop),
            buttonPanel -> buttonPanel.add(playPause),
            buttonPanel -> buttonPanel.add(previous)));
        box.add(Box.createVerticalStrut(5));
        box.add(searchTextArea);
        box.add(Util.apply(new JPanel(),
            buttonPanel -> buttonPanel.setAlignmentX(0),
            buttonPanel -> buttonPanel.add(search)));
    }
}
