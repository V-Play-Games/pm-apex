package net.vpg.apex.components;

import net.vpg.apex.Util;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

import static javax.swing.Box.createVerticalStrut;

public class CreditsPanel extends JPanel {
    private static final CreditsPanel instance = new CreditsPanel();

    private CreditsPanel() {
        this.setName("Credits and Info");
        this.setBorder(new EmptyBorder(15, 15, 0, 15));
        this.setLayout(new BorderLayout());
        Util.addBox(this, "North",
            createTextArea("Welcome to Pokemon Masters Audio Player EX, PM APEX in short."),
            createVerticalStrut(10),
            createTextArea("This is an application made for playing audio tracks from Pokemon Masters. " +
                "It also has looping support, which means you can loop your favourite battle theme for as long as you want. " +
                "It also comes preloaded with some battle themes. You can download more tracks as well!" +
                "Have Fun!"),
            createVerticalStrut(20),
            Util.apply(new WrappedTextArea("Credits"),
                textArea -> textArea.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14)),
                textArea -> textArea.setAlignmentX(0)),
            createVerticalStrut(5),
            createTextArea("V Play Games#9783 - The Author and Maintainer of this project"),
            createVerticalStrut(3),
            createTextArea("Trilarion (GitHub) - For Providing OGG File Support"),
            createVerticalStrut(3),
            createTextArea("Made with Java, Built with Maven 3")
        );
    }

    public static CreditsPanel getInstance() {
        return instance;
    }

    private static WrappedTextArea createTextArea(String text) {
        return Util.apply(new WrappedTextArea(text), jLabel -> jLabel.setAlignmentX(0));
    }
}
