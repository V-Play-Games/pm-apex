package net.vplaygames.apex.components;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class WrappedTextArea extends JTextArea {
    public WrappedTextArea(String text) {
        this();
        setText(text);
    }

    public WrappedTextArea() {
        setAlignmentX(0);
        setBorder(new EmptyBorder(new Insets(0, 0, 0, 0)));
        setEditable(false);
        setLineWrap(true);
        setWrapStyleWord(true);
        setFont(new JLabel().getFont());
        setFocusable(false);
        setRows(0);
        invalidate();
    }

    public void paintComponent(Graphics graphics) {
        setBackground(getParent().getBackground());
        super.paintComponent(graphics);
    }
}
