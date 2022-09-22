package net.vpg.apex.components;

import net.vpg.apex.Util;
import net.vpg.apex.core.Resources;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;

public class ApexWindow extends JFrame {
    public ApexWindow() throws Exception {
        super("PM APEX");
        this.add(Util.apply(new JTabbedPane(),
            tabPane -> tabPane.add(PlayerPanel.getInstance()),
            tabPane -> tabPane.add(DownloadPanel.getInstance()),
            tabPane -> tabPane.add(CreditsPanel.getInstance())));
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setLocationRelativeTo(null);
        this.setResizable(true);
        this.setMinimumSize(new Dimension(500, 400));
        this.setIconImage(ImageIO.read(Resources.get("icon.png")));
        this.pack();
        this.setVisible(true);
    }
}
