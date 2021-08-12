package net.vplaygames.apex.components;

import net.vplaygames.apex.Util;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class CreditsPanel extends JPanel {
    public CreditsPanel() {
        this.setName("Credits and Info");
        this.setBorder(new EmptyBorder(new Insets(15, 15, 0, 15)));
        this.setLayout(new BorderLayout());
        Box box = Box.createVerticalBox();
        this.add(box, "North");
        addTextArea(box, 10, "Welcome to Pokemon Masters Audio Player EX, PM APEX in short.");
        addTextArea(box, 20, "This is an application made for playing audio tracks from Pokemon Masters. " +
            "It also has looping support, which means you can loop your favourite battle theme for as long as you want. " +
            "It also comes preloaded with some battle themes. You can download more tracks as well!" +
            "Have Fun!");
        addTextArea(box, 5, Util.apply(new WrappedTextArea("Credits"), jLabel -> jLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14))));
        addTextArea(box, 3, "V Play Games - The Author and Maintainer of this project");
        addTextArea(box, 3, "Trilarion (GitHub) - For Providing OGG File Support");
        addTextArea(box, 3, "Made with Java 8 (Build: 261) using IntelliJ IDE\nBuilt with Maven 3");
    }

    public void addTextArea(Box box, int trailingStrutHeight, String text) {
        addTextArea(box, trailingStrutHeight, new WrappedTextArea(text));
    }

    public void addTextArea(Box box, int trailingStrutHeight, JTextArea area) {
        box.add(Util.apply(area, jLabel -> jLabel.setAlignmentX(0)));
        box.add(Box.createVerticalStrut(trailingStrutHeight));
    }
}
