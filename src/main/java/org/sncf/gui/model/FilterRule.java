package org.sncf.gui.model;

import java.awt.*;

/**
 * Représente une règle de filtrage qui associe un motif (pattern)
 * à une couleur utilisée pour le surlignage ou la catégorisation.
 * <p>
 * Cette classe est principalement utilisée pour définir des règles
 * de mise en forme basées sur des expressions régulières ou des
 * chaînes spécifiques, associées à une couleur.
 * </p>
 */
public class FilterRule {

    /**
     * Motif (pattern) de filtrage à appliquer, généralement une chaîne ou une expression régulière.
     */
    public String pattern;

    /**
     * Couleur associée au motif, utilisée pour la mise en forme (par exemple, surlignage).
     */
    public Color color;

    /**
     * Construit une règle de filtrage avec un motif donné et une couleur en format hexadécimal.
     *
     * @param pattern   Le motif de filtrage à utiliser (ex. : une regex ou un mot-clé).
     * @param hexColor  La couleur en format hexadécimal (ex. : "#FF0000" pour rouge).
     */
    public FilterRule(String pattern, String hexColor) {
        this.pattern = pattern;
        this.color = Color.decode(hexColor);
    }
}
