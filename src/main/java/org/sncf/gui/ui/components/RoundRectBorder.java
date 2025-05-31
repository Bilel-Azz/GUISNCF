package org.sncf.gui.ui.components;

import javax.swing.border.AbstractBorder;
import java.awt.*;

/**
 * Bordure personnalisée arrondie pour composants Swing.
 * <p>
 * Cette bordure dessine un rectangle aux coins arrondis autour du composant.
 * Elle est utile pour styliser des boutons, panneaux ou champs avec un rendu plus moderne.
 * </p>
 */
public class RoundRectBorder extends AbstractBorder {

    /**
     * Rayon des coins arrondis.
     */
    private final int radius;

    /**
     * Couleur de la bordure.
     */
    private final Color color;

    /**
     * Crée une bordure arrondie avec un rayon donné et une couleur par défaut (gris clair).
     *
     * @param radius Rayon des coins en pixels.
     */
    public RoundRectBorder(int radius) {
        this(radius, Color.LIGHT_GRAY);
    }

    /**
     * Crée une bordure arrondie avec un rayon et une couleur spécifiés.
     *
     * @param radius Rayon des coins en pixels.
     * @param color  Couleur de la bordure.
     */
    public RoundRectBorder(int radius, Color color) {
        this.radius = radius;
        this.color = color;
    }

    /**
     * Dessine la bordure arrondie autour du composant.
     *
     * @param c      Composant cible.
     * @param g      Contexte graphique.
     * @param x      Coordonnée X de départ.
     * @param y      Coordonnée Y de départ.
     * @param width  Largeur disponible.
     * @param height Hauteur disponible.
     */
    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setColor(color);
        g2.drawRoundRect(x, y, width - 1, height - 1, radius, radius);
        g2.dispose();
    }
}