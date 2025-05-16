// GraphView.java
package org.example.demo.ui.views;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class GraphView extends JPanel {
    private final List<Integer> bits = new ArrayList<>();
    private int bitWidth = 20;
    private int bitHeight = 60;

    public GraphView() {
        setBackground(Color.WHITE);
    }

    public void appendBits(String bitString) {
        for (char c : bitString.toCharArray()) {
            if (c == '0' || c == '1') {
                bits.add(c - '0');
            }
        }
        Container parent = getParent();
        if (parent != null) {
            parent.revalidate();
        }
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        int width = 100 + bits.size() * bitWidth;
        int height = getParent() != null ? getParent().getHeight() : 600;
        height = Math.max(height, 600);
        return new Dimension(width, height);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int height = getHeight();
        int width = getWidth();
        int centerY = height / 2;
        int topY = centerY - bitHeight / 2;
        int botY = centerY + bitHeight / 2;
        int x = 60;

        // Axes
        g2d.setColor(Color.GRAY);
        g2d.setStroke(new BasicStroke(1));
        g2d.drawLine(40, topY - 40, 40, botY + 40);
        g2d.drawLine(40, centerY, width - 40, centerY);

        // Légendes
        g2d.setFont(new Font("SansSerif", Font.PLAIN, 14));
        g2d.drawString("1", 20, topY + 5);
        g2d.drawString("0", 20, botY + 5);
        g2d.drawString("Temps →", width - 80, centerY + 20);
        g2d.drawString("Bit", 5, centerY - bitHeight);

        // Signal
        g2d.setColor(Color.BLUE);
        g2d.setStroke(new BasicStroke(2));
        for (int i = 0; i < bits.size(); i++) {
            int currentBit = bits.get(i);
            int y = currentBit == 1 ? topY : botY;
            int nextY = (i < bits.size() - 1 && bits.get(i + 1) == 0) ? botY : topY;
            g2d.drawLine(x, y, x + bitWidth, y);
            if (i < bits.size() - 1 && bits.get(i + 1) != currentBit) {
                g2d.drawLine(x + bitWidth, y, x + bitWidth, nextY);
            }
            x += bitWidth;
        }
    }

    public JScrollPane asScrollableView() {
        JScrollPane scrollPane = new JScrollPane(this,
                JScrollPane.VERTICAL_SCROLLBAR_NEVER,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getViewport().setBackground(Color.WHITE);
        return scrollPane;
    }

}