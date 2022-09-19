package net.vpg.apex.components;

import net.vpg.apex.Apex;
import net.vpg.apex.Util;
import net.vpg.apex.core.Resources;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

public class DownloadPanel extends JPanel {
    boolean initialFocusGained = false;
    Box progressBox;

    public DownloadPanel() {
        setupButtons();
        this.setName("Download Tracks");
        this.setBorder(new EmptyBorder(new Insets(15, 15, 0, 15)));
        this.setLayout(new BorderLayout());
        this.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (!initialFocusGained) {
                    ApexControl.lookupTracks.setEnabled(false);
                    ApexControl.downloadAll.setEnabled(false);
                    ApexControl.tracksFound.setVisible(false);
                    ApexControl.tracksFound.setText(Resources.getInstance().getMissingTracks().size() + " more tracks found");
                    ApexControl.lookupTracks.setEnabled(true);
                    ApexControl.downloadAll.setEnabled(true);
                    ApexControl.tracksFound.setVisible(true);
                }
            }
        });
        Box box = Box.createVerticalBox();
        this.add(box, "North");
        box.add(Util.apply(new JPanel(),
            buttonPanel -> buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER)),
            buttonPanel -> buttonPanel.setAlignmentX(0),
            buttonPanel -> buttonPanel.add(ApexControl.lookupTracks),
            buttonPanel -> buttonPanel.add(ApexControl.downloadAll)));
        box.add(ApexControl.tracksFound);
        box.add(Box.createVerticalStrut(5));
        box.add(progressBox = Util.apply(Box.createVerticalBox(),
            buttonPanel -> buttonPanel.setAlignmentX(0),
            buttonPanel -> buttonPanel.add(ApexControl.fileProgressText),
            buttonPanel -> buttonPanel.add(Box.createVerticalStrut(5)),
            buttonPanel -> buttonPanel.add(ApexControl.fileProgressBar),
            buttonPanel -> buttonPanel.add(Box.createVerticalStrut(5)),
            buttonPanel -> buttonPanel.add(ApexControl.totalProgressText),
            buttonPanel -> buttonPanel.add(Box.createVerticalStrut(5)),
            buttonPanel -> buttonPanel.add(ApexControl.totalProgressBar)));
        progressBox.setVisible(false);
    }

    public void setupButtons() {
        ApexControl.lookupTracks = new JButton("Refresh");
        ApexControl.downloadAll = new JButton("Download all found tracks");
        ApexControl.lookupTracks.addActionListener(e -> ApexControl.tracksFound.setText(Resources.getInstance().getMissingTracks().size() + " more tracks found"));
        ApexControl.downloadAll.addActionListener(e -> {
            ApexControl.lookupTracks.setEnabled(false);
            ApexControl.downloadAll.setEnabled(false);
            new DownloadTask(Resources.getInstance().getMissingTracks(), () -> Apex.APEX.takeAction(6));
        });
    }

    public void showDownload() {
        progressBox.setVisible(true);
    }

    public void hideDownload() {
        progressBox.setVisible(false);
    }
}
