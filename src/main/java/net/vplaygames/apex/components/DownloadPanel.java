package net.vplaygames.apex.components;

import net.vplaygames.apex.Util;
import net.vplaygames.apex.core.Resources;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

import static net.vplaygames.apex.Apex.apex;
import static net.vplaygames.apex.components.ApexControl.*;

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
                    lookupTracks.setEnabled(false);
                    downloadAll.setEnabled(false);
                    tracksFound.setVisible(false);
                    tracksFound.setText(Resources.getMissingTracks().size() + " more tracks found");
                    lookupTracks.setEnabled(true);
                    downloadAll.setEnabled(true);
                    tracksFound.setVisible(true);
                }
            }
        });
        Box box = Box.createVerticalBox();
        this.add(box, "North");
        box.add(Util.apply(new JPanel(),
            buttonPanel -> buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER)),
            buttonPanel -> buttonPanel.setAlignmentX(0),
            buttonPanel -> buttonPanel.add(lookupTracks),
            buttonPanel -> buttonPanel.add(downloadAll)));
        box.add(tracksFound);
        box.add(Box.createVerticalStrut(5));
        box.add(progressBox = Util.apply(Box.createVerticalBox(),
            buttonPanel -> buttonPanel.setAlignmentX(0),
            buttonPanel -> buttonPanel.add(fileProgressText),
            buttonPanel -> buttonPanel.add(Box.createVerticalStrut(5)),
            buttonPanel -> buttonPanel.add(fileProgressBar),
            buttonPanel -> buttonPanel.add(Box.createVerticalStrut(5)),
            buttonPanel -> buttonPanel.add(totalProgressText),
            buttonPanel -> buttonPanel.add(Box.createVerticalStrut(5)),
            buttonPanel -> buttonPanel.add(totalProgressBar)));
        progressBox.setVisible(false);
    }

    public void setupButtons() {
        lookupTracks = new JButton("Refresh");
        downloadAll = new JButton("Download all found tracks");
        lookupTracks.addActionListener(e -> {
            tracksFound.setText(Resources.getMissingTracks().size() + " more tracks found");
        });
        downloadAll.addActionListener(e -> {
            lookupTracks.setEnabled(false);
            downloadAll.setEnabled(false);
            new DownloadTask(Resources.getMissingTracks(), () -> apex.takeAction(6));
        });
    }

    public void showDownload() {
        progressBox.setVisible(true);
    }

    public void hideDownload() {
        progressBox.setVisible(false);
    }
}
