// GraphView.java
package org.example.demo.ui.views;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class GraphView extends JPanel {
    private final List<Integer> bits = new ArrayList<>();
    private final List<Integer> frameBoundaries = new ArrayList<>();
    private int bitWidth = 20;

    public GraphView() {
        setBackground(Color.WHITE);
    }

    public void appendBits(String bitString) {
        for (char c : bitString.toCharArray()) {
            if (c == '0' || c == '1') {
                bits.add(c - '0');
            }
        }
        // Marque la fin de la trame actuelle
        frameBoundaries.add(bits.size());

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

        int width = getWidth();
        int height = getHeight();

        // Espacement de la grille
        int gridSpacingY = 40;

        // Axe horizontal centré sur une ligne de grille
        int centerY = (height / 2 / gridSpacingY) * gridSpacingY;

        // Zone de tracé (50 % de la hauteur)
        int bitAreaHeight = height / 2;
        int topMargin = centerY - bitAreaHeight / 2;
        int bottomMargin = centerY + bitAreaHeight / 2;

        int x = 40 + bitWidth; // début du tracé du signal

        // 🧩 Grille
        g2d.setColor(new Color(200, 200, 200));
        g2d.setStroke(new BasicStroke(1));
        for (int y = 0; y <= height; y += gridSpacingY) {
            g2d.drawLine(0, y, width, y);
        }
        int xGrid = 40;
        while (xGrid <= width) {
            g2d.drawLine(xGrid, 0, xGrid, height);
            xGrid += bitWidth;
        }

        // 🟩 Axes
        g2d.setColor(Color.GRAY);
        g2d.setStroke(new BasicStroke(1));
        g2d.drawLine(40, topMargin, 40, bottomMargin);       // Axe vertical
        g2d.drawLine(40, centerY, width - 40, centerY);      // Axe horizontal

        // 🏷️ Légendes
        g2d.setFont(new Font("SansSerif", Font.PLAIN, 14));
        g2d.drawString("1", 20, topMargin + 5);
        g2d.drawString("0", 20, bottomMargin + 5);
        g2d.drawString("Temps →", width - 80, centerY + 20);
        g2d.drawString("Bit", 5, centerY - 60);

        // 📉 Signal
        g2d.setColor(Color.BLUE);
        g2d.setStroke(new BasicStroke(2));
        for (int i = 0; i < bits.size(); i++) {
            int currentBit = bits.get(i);
            int y = currentBit == 1 ? topMargin : bottomMargin;
            int nextY = y;
            if (i < bits.size() - 1) {
                int nextBit = bits.get(i + 1);
                nextY = nextBit == 1 ? topMargin : bottomMargin;
            }

            g2d.drawLine(x, y, x + bitWidth, y);
            if (i < bits.size() - 1 && nextY != y) {
                g2d.drawLine(x + bitWidth, y, x + bitWidth, nextY);
            }

            x += bitWidth;
        }

        // Séparateurs de trames
        g2d.setColor(Color.BLACK);
        g2d.setStroke(new BasicStroke(1.5f));
        for (int boundaryIndex : frameBoundaries) {
            int separatorX = 40 + bitWidth + boundaryIndex * bitWidth;
            g2d.drawLine(separatorX, 0, separatorX, height);
        }

        // 🚩 Affichage du bit de start de chaque trame
        g2d.setColor(Color.RED);
        g2d.setFont(new Font("SansSerif", Font.BOLD, 12));

        int previousEnd = 0;
        for (int boundaryIndex : frameBoundaries) {
            int startBitIndex = previousEnd;
            int startX = 40 + bitWidth + startBitIndex * bitWidth;
            g2d.drawString("Start", startX + 2, 30);
            previousEnd = boundaryIndex;
        }
    }

    public void clear() {
        bits.clear();
        frameBoundaries.clear();
        repaint();
    }

    public JScrollPane asScrollableView() {
        JScrollPane scrollPane = new JScrollPane(this,
                JScrollPane.VERTICAL_SCROLLBAR_NEVER,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getViewport().setBackground(Color.WHITE);
        return scrollPane;
    }
}
