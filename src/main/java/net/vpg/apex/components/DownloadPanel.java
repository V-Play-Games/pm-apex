package net.vpg.apex.components;

import net.vpg.apex.Util;
import net.vpg.apex.core.Resources;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

public class DownloadPanel extends ApexPanel {
    private static final DownloadPanel instance = new DownloadPanel();
    private final boolean initialFocusGained = false;
    private final Box progressBox;

    private DownloadPanel() {
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
        this.addBox("North",
            Util.apply(new JPanel(),
                buttonPanel -> buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER)),
                buttonPanel -> buttonPanel.setAlignmentX(0),
                buttonPanel -> buttonPanel.add(ApexControl.lookupTracks),
                buttonPanel -> buttonPanel.add(ApexControl.downloadAll)),
            ApexControl.tracksFound,
            Box.createVerticalStrut(5),
            progressBox = Util.apply(Box.createVerticalBox(),
                panel -> panel.setAlignmentX(0),
                panel -> panel.add(ApexControl.fileProgressText),
                panel -> panel.add(Box.createVerticalStrut(5)),
                panel -> panel.add(ApexControl.fileProgressBar),
                panel -> panel.add(Box.createVerticalStrut(5)),
                panel -> panel.add(ApexControl.totalProgressText),
                panel -> panel.add(Box.createVerticalStrut(5)),
                panel -> panel.add(ApexControl.totalProgressBar)));
        progressBox.setVisible(false);
    }

    public static DownloadPanel getInstance() {
        return instance;
    }

    public void showDownload() {
        progressBox.setVisible(true);
    }

    public void hideDownload() {
        progressBox.setVisible(false);
    }
}
