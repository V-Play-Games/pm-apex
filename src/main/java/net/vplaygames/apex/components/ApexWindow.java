package net.vplaygames.apex.components;

import net.vplaygames.apex.core.Resources;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;

public class ApexWindow extends JFrame {
    public static JTabbedPane tabPane;
    public static PlayerPanel playerPanel = new PlayerPanel();
    public static DownloadPanel downloadPanel = new DownloadPanel();
    public static CreditsPanel creditsPanel = new CreditsPanel();

    public ApexWindow() throws Exception {
        super("PM APEX");
        tabPane = new JTabbedPane();
        add(tabPane);
        tabPane.add(playerPanel);
        tabPane.add(downloadPanel);
        tabPane.add(creditsPanel);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(true);
        setMinimumSize(new Dimension(500, 260));
        setIconImage(ImageIO.read(Resources.geFile("icon.png")));
        pack();
        setVisible(true);
    }
}
