package org.example.demo.ui.components;

import javax.swing.border.AbstractBorder;
import java.awt.*;

public class RoundRectBorder extends AbstractBorder {
    private final int radius;
    private final Color color;

    public RoundRectBorder(int radius) {
        this(radius, Color.LIGHT_GRAY);
    }

    public RoundRectBorder(int radius, Color color) {
        this.radius = radius;
        this.color = color;
    }

    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setColor(color);
        g2.drawRoundRect(x, y, width - 1, height - 1, radius, radius);
        g2.dispose();
    }
}
