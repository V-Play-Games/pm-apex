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
    public PlayerPanel() {
        this.setName("Player");
        this.setBorder(new EmptyBorder(new Insets(15, 15, 0, 15)));
        this.setLayout(new BorderLayout());
        this.addBox("North",
            ApexControl.trackListPane = new JScrollPane(
                Util.apply(ApexControl.trackList = new JList<>(ApexControl.trackListModel),
                    list -> list.setVisibleRowCount(7),
                    list -> list.addMouseListener(new MouseAdapter() {
                        @Override
                        public void mouseClicked(MouseEvent e) {
                            if (e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1) {
                                Apex.APEX.takeAction(7);
                            }
                        }
                    }),
                    list -> list.addKeyListener(new KeyAdapter() {
                        @Override
                        public void keyTyped(KeyEvent e) {
                            if (e.getKeyChar() == '\n') {
                                Apex.APEX.takeAction(7);
                            }
                        }
                    })
                )
            ),
            Box.createHorizontalStrut(5),
            ApexControl.trackName,
            Box.createVerticalStrut(5),
            ApexControl.trackDescription,
            Box.createVerticalStrut(5),
            ApexControl.trackDescription,
            Box.createVerticalStrut(5),
            ApexControl.trackId,
            Box.createVerticalStrut(5),
            ApexControl.trackIndex,
            Box.createVerticalStrut(5));
        this.addBox("South",
            ApexControl.searchTextArea,
            Util.apply(new JPanel(),
                buttonPanel -> buttonPanel.setAlignmentX(0),
                buttonPanel -> buttonPanel.add(ApexControl.search)),
            Box.createVerticalStrut(5),
            Util.apply(new JPanel(),
                buttonPanel -> buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER)),
                buttonPanel -> buttonPanel.setAlignmentX(0),
                buttonPanel -> buttonPanel.add(ApexControl.shuffle),
                buttonPanel -> buttonPanel.add(ApexControl.next),
                buttonPanel -> buttonPanel.add(ApexControl.stop),
                buttonPanel -> buttonPanel.add(ApexControl.playPause),
                buttonPanel -> buttonPanel.add(ApexControl.previous)));
    }

    private void addBox(String constraints, Component... components) {
        Box box = Box.createVerticalBox();
        this.add(box, constraints);
        for (Component component : components) {
            box.add(component);
        }
    }
}
