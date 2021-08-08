package net.vplaygames.apex;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class SearchTextArea extends JTextArea {
    public SearchTextArea(String var1) {
        this();
        setText(var1);
    }

    public SearchTextArea() {
        setBorder(new EmptyBorder(new Insets(0, 0, 0, 0)));
        setEditable(true);
        setLineWrap(false);
        setFont(new JLabel().getFont());
        setFocusable(true);
        setRows(0);
        invalidate();
    }
}
