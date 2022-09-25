package net.vpg.apex.components;

import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class SearchTextArea extends JTextArea {
    public SearchTextArea(String text) {
        this();
        setText(text);
    }

    public SearchTextArea() {
        setAlignmentX(0);
        setBorder(new EmptyBorder(5, 5, 0, 5));
        setEditable(true);
        setLineWrap(false);
        setFont(new JLabel().getFont());
        setFocusable(true);
        setRows(0);
        invalidate();
    }
}
