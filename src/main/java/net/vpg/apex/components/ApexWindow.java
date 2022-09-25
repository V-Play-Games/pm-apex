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
        super("PM APEX");
        Box box = Util.addBox(this, null);
        box.setBorder(new EmptyBorder(3, 3, 3, 3));
        box.add(Util.apply(new JTabbedPane(),
            tabPane -> tabPane.add(PlayerPanel.getInstance()),
            tabPane -> tabPane.add(DownloadPanel.getInstance()),
            tabPane -> tabPane.add(CreditsPanel.getInstance())));
        Util.addBox(this, "South",
            ApexControl.searchTextArea,
            Util.apply(new JPanel(),
                buttonPanel -> buttonPanel.setAlignmentX(0),
                buttonPanel -> buttonPanel.add(ApexControl.search),
                buttonPanel -> buttonPanel.add(ApexControl.surpriseMe)),
            Box.createVerticalStrut(5),
            Util.apply(new JPanel(),
                buttonPanel -> buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER)),
                buttonPanel -> buttonPanel.setAlignmentX(0),
                buttonPanel -> buttonPanel.add(ApexControl.shuffle),
                buttonPanel -> buttonPanel.add(ApexControl.next),
                buttonPanel -> buttonPanel.add(ApexControl.stop),
                buttonPanel -> buttonPanel.add(ApexControl.playPause),
                buttonPanel -> buttonPanel.add(ApexControl.previous)));
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
