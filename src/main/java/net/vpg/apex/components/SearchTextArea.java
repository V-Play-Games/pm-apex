package net.vpg.apex.components;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class SearchTextArea extends JTextArea {
    public SearchTextArea(String text) {
        this();
        setText(text);
    }

    public SearchTextArea() {
        setAlignmentX(0);
        setBorder(new EmptyBorder(new Insets(0, 0, 0, 0)));
        setEditable(true);
        setLineWrap(false);
        setFont(new JLabel().getFont());
        setFocusable(true);
        setRows(0);
        invalidate();
    }
}
