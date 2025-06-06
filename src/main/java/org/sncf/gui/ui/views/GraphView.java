package org.sncf.gui.ui.views;

import org.sncf.gui.model.FilterRule;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Composant Swing personnalisée représentant une vue graphique des trames binaires.
 * <p>
 * Cette vue affiche les bits reçus sous forme de signal numérique, avec :
 * <ul>
 *     <li>Une échelle temporelle horizontale</li>
 *     <li>Des séparateurs de trames (frame boundaries)</li>
 *     <li>Des surlignages colorés en fonction de filtres binaires appliqués</li>
 * </ul>
 * <p>
 * Les filtres sont fournis via des instances de {@link FilterRule}, et seuls
 * les motifs binaires sont reconnus pour la coloration.
 */
public class GraphView extends JPanel {
    private final List<Integer> bits = new ArrayList<>();
    private final List<Integer> frameBoundaries = new ArrayList<>();
    private int bitWidth = 20;
    private final JPanel graphPanel;
    private double bitDurationMs = 1.0;
    private List<FilterRule> activeFilters = new ArrayList<>();

    /**
     * Initialise la vue graphique avec une zone défilante et un bouton de réinitialisation.
     */
    public GraphView() {
        setLayout(new BorderLayout());

        // Bouton fixe en haut
        JButton resetButton = new JButton("Réinitialiser le graphique");
        resetButton.addActionListener(e -> clear());

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.add(resetButton);

        add(topPanel, BorderLayout.NORTH); // Ce panel ne scrollera pas

        // GraphPanel : zone scrollable
        graphPanel = new JPanel() {
            @Override
            public Dimension getPreferredSize() {
                int baseWidth = 600;
                int width = Math.max(baseWidth, 100 + bits.size() * bitWidth);
                return new Dimension(width, 600);
            }

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawGraph((Graphics2D) g);
            }
        };
        graphPanel.setBackground(Color.WHITE);

        // Seul ce scrollPane défile
        JScrollPane scrollPane = new JScrollPane(graphPanel,
                JScrollPane.VERTICAL_SCROLLBAR_NEVER,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getViewport().setBackground(Color.WHITE);

        add(scrollPane, BorderLayout.CENTER); // Scrollable uniquement ici
    }

    /**
     * Ajoute une chaîne binaire à la liste affichée sur le graphique.
     * Une nouvelle trame est automatiquement marquée à la fin de l’ajout.
     *
     * @param bitString chaîne de caractères composée de '0' et '1'.
     */
    public void appendBits(String bitString) {
        for (char c : bitString.toCharArray()) {
            if (c == '0' || c == '1') {
                bits.add(c - '0');
            }
        }
        frameBoundaries.add(bits.size());
        graphPanel.revalidate();
        graphPanel.repaint();
    }

    /**
     * Efface toutes les données du graphique (bits et délimitations de trame).
     */
    public void clear() {
        bits.clear();
        frameBoundaries.clear();
        graphPanel.setPreferredSize(new Dimension(100, 600));
        graphPanel.revalidate();
        graphPanel.repaint();
    }

    /**
     * Applique une nouvelle liste de filtres actifs.
     *
     * @param filters liste de règles de filtrage (peut être {@code null} ou vide).
     */
    public void setFilters(List<FilterRule> filters) {
        this.activeFilters = filters != null ? filters : new ArrayList<>();
        graphPanel.repaint();
    }

    /**
     * Méthode de rendu du graphique. Affiche :
     * <ul>
     *     <li>Les lignes de signal (0 ou 1)</li>
     *     <li>Les filtres appliqués en couleurs spécifiques</li>
     *     <li>Les séparateurs entre trames</li>
     *     <li>Des étiquettes indicatives (temps, bit, etc.)</li>
     * </ul>
     *
     * @param g2d le contexte graphique utilisé pour dessiner.
     */
    private void drawGraph(Graphics2D g2d) {
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = graphPanel.getWidth();
        int height = graphPanel.getHeight();
        int gridSpacingY = 40;
        int centerY = (height / 2 / gridSpacingY) * gridSpacingY;
        int bitAreaHeight = height / 2;
        int topMargin = centerY - bitAreaHeight / 2;
        int bottomMargin = centerY + bitAreaHeight / 2;
        int x = 40 + bitWidth;

        g2d.setColor(new Color(200, 200, 200));
        g2d.setStroke(new BasicStroke(1));
        for (int y = 0; y <= height; y += gridSpacingY) {
            g2d.drawLine(0, y, width, y);
        }
        g2d.setColor(new Color(200, 200, 200));
        g2d.setStroke(new BasicStroke(1));
        g2d.setFont(new Font("SansSerif", Font.PLAIN, 10));

        int xGrid = 40 + bitWidth;
        int bitIndex = 0;

        while (xGrid <= width) {
            g2d.drawLine(xGrid, 0, xGrid, height);

            if (bitIndex % 10 == 0) {
                // Tick vertical centré autour de l'axe horizontal
                g2d.setColor(Color.BLACK);
                g2d.drawLine(xGrid, centerY - 5, xGrid, centerY + 5);

                // Étiquette de temps
                int timeMs = (int) (bitIndex * bitDurationMs);
                g2d.drawString(timeMs + " ms", xGrid - 15, centerY + 40);

                g2d.setColor(new Color(200, 200, 200));
            }

            xGrid += bitWidth;
            bitIndex++;
        }

        g2d.setColor(Color.GRAY);
        g2d.setStroke(new BasicStroke(1));
        g2d.drawLine(40, topMargin, 40, bottomMargin);
        g2d.drawLine(40, centerY, width - 40, centerY);

        g2d.setFont(new Font("SansSerif", Font.PLAIN, 14));
        g2d.drawString("1", 20, topMargin + 5);
        g2d.drawString("0", 20, bottomMargin + 5);
        g2d.drawString("Temps →", width - 80, centerY + 20);
        g2d.drawString("Bit", 5, centerY - 60);

        Color[] bitColors = new Color[bits.size()];
        for (int i = 0; i < bits.size(); i++) {
            bitColors[i] = Color.BLUE;
        }

        for (FilterRule rule : activeFilters) {
            if (rule.pattern.matches("[01]+")) {
                String pattern = rule.pattern;
                int len = pattern.length();
                for (int i = 0; i <= bits.size() - len; ) {
                    boolean match = true;
                    for (int j = 0; j < len; j++) {
                        if (bits.get(i + j) != (pattern.charAt(j) - '0')) {
                            match = false;
                            break;
                        }
                    }
                    if (match) {
                        for (int j = 0; j < len; j++) {
                            bitColors[i + j] = rule.color;
                        }
                        i += len;
                    } else {
                        i++;
                    }
                }
            }
        }

        for (int i = 0; i < bits.size(); i++) {
            int currentBit = bits.get(i);
            int y = currentBit == 1 ? topMargin : bottomMargin;
            int nextY = y;
            if (i < bits.size() - 1) {
                int nextBit = bits.get(i + 1);
                nextY = nextBit == 1 ? topMargin : bottomMargin;
            }

            g2d.setColor(bitColors[i]);
            g2d.setStroke(new BasicStroke(2));
            g2d.drawLine(x, y, x + bitWidth, y);
            if (i < bits.size() - 1 && nextY != y) {
                g2d.drawLine(x + bitWidth, y, x + bitWidth, nextY);
            }

            x += bitWidth;
        }

        g2d.setColor(Color.BLACK);
        g2d.setStroke(new BasicStroke(1.5f));
        for (int boundaryIndex : frameBoundaries) {
            int separatorX = 40 + bitWidth + boundaryIndex * bitWidth;
            g2d.drawLine(separatorX, 0, separatorX, height);
        }

        g2d.setFont(new Font("SansSerif", Font.BOLD, 12));
        int previousEnd = 0;

        double timePerBitMs = 1.0; // 1 ms par bit

        for (int boundaryIndex : frameBoundaries) {
            int startBitIndex = previousEnd;
            int endBitIndex = boundaryIndex;

            int startX = 40 + bitWidth + startBitIndex * bitWidth;
            int endX = 40 + bitWidth + endBitIndex * bitWidth;

            // Tracé du séparateur "Start"
            g2d.setColor(Color.RED);
            g2d.drawString("Start", startX + 2, 30);

            // Calcul des temps
            int durationBits = endBitIndex - startBitIndex;
            int durationPixels = durationBits * bitWidth;

            String startLabel = String.format("%d ms", (int) (startBitIndex * timePerBitMs));
            String endLabel = String.format("%d ms", (int) (endBitIndex * timePerBitMs));

            g2d.setFont(new Font("SansSerif", Font.ITALIC, 11));
            g2d.setColor(Color.DARK_GRAY);

            // Affichage conditionnel si la trame est courte
            if (durationPixels < 60) {
                g2d.drawString(startLabel, startX, centerY + 65);
                g2d.drawString(endLabel, endX, centerY + 80);
            } else {
                g2d.drawString(startLabel, startX, centerY + 65);
                g2d.drawString(endLabel, endX - 40, centerY + 65);
            }

            previousEnd = boundaryIndex;
        }
    }
}
