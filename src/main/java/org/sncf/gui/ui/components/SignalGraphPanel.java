package org.sncf.gui.ui.components;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Composant Swing personnalisé qui affiche un graphe de signal binaire (0 ou 1)
 * sur un axe temporel simulé.
 * <p>
 * Il est principalement utilisé pour représenter visuellement une trame binaire
 * sous forme de graphes logiques (courbes en escalier).
 * </p>
 */
public class SignalGraphPanel extends JPanel {

    /**
     * Liste des points représentant l'évolution du signal (x = temps, y = niveau).
     */
    private List<Point> points;

    /**
     * Construit un panneau de graphe avec une série de points prédéfinis.
     */
    public SignalGraphPanel() {
        setBackground(Color.WHITE);
        initializePoints();
    }

    /**
     * Initialise une séquence de points simulant une trame binaire.
     */
    private void initializePoints() {
        points = new ArrayList<>();
        points.add(new Point(0, 1));
        points.add(new Point(10, 1));
        points.add(new Point(10, 0));
        points.add(new Point(15, 0));
        points.add(new Point(15, 1));
        points.add(new Point(20, 1));
        points.add(new Point(20, 0));
        points.add(new Point(25, 0));
        points.add(new Point(25, 1));
        points.add(new Point(30, 1));
    }

    /**
     * Redessine le graphe à chaque mise à jour du composant.
     *
     * @param g contexte graphique
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();
        int padding = 50;

        // Axes
        g2d.setColor(Color.BLACK);
        g2d.drawLine(padding, height - padding, width - padding, height - padding); // X
        g2d.drawLine(padding, padding, padding, height - padding); // Y

        // Légendes
        g2d.drawString("Time (ms)", width / 2, height - 10);
        g2d.drawString("Bit", 10, height / 2);

        // Graduation axe X
        for (int i = 0; i <= 30; i += 5) {
            int x = padding + (i * (width - 2 * padding) / 30);
            g2d.drawLine(x, height - padding - 5, x, height - padding + 5);
            g2d.drawString(String.valueOf(i), x - 5, height - padding + 20);
        }

        // Graduation axe Y (0/1)
        for (int i = 0; i <= 1; i++) {
            int y = height - padding - (i * (height - 2 * padding));
            g2d.drawLine(padding - 5, y, padding + 5, y);
            g2d.drawString(String.valueOf(i), padding - 15, y + 5);
        }

        // Courbe du signal
        g2d.setColor(Color.BLUE);
        g2d.setStroke(new BasicStroke(2));

        for (int i = 0; i < points.size() - 1; i++) {
            Point p1 = points.get(i);
            Point p2 = points.get(i + 1);

            int x1 = padding + (p1.x * (width - 2 * padding) / 30);
            int y1 = height - padding - (p1.y * (height - 2 * padding));
            int x2 = padding + (p2.x * (width - 2 * padding) / 30);
            int y2 = height - padding - (p2.y * (height - 2 * padding));

            g2d.drawLine(x1, y1, x2, y2);
        }
    }
}