package net.vpg.apex.components;

import javax.swing.*;
import java.awt.*;

public class ApexPanel extends JPanel {
    protected void addBox(String constraints, Component... components) {
        Box box = Box.createVerticalBox();
        this.add(box, constraints);
        for (Component component : components) {
            box.add(component);
        }
    }
}
