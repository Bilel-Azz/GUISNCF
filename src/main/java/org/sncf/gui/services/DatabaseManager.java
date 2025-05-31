package org.sncf.gui.services;

import java.sql.*;

/**
 * Classe utilitaire pour gérer les interactions avec la base de données SQLite.
 * Elle permet d'insérer, de supprimer et de mettre à jour des trames dans la table
 * {@code frame_capture}.
 */
public class DatabaseManager {
    private static final String DB_URL = "jdbc:sqlite:bdd.db";

    /**
     * Obtient une connexion JDBC à la base de données SQLite {@code bdd.db}.
     *
     * @return connexion à la base de données.
     * @throws SQLException en cas d'erreur de connexion.
     */
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    /**
     * Insère une nouvelle trame dans la base de données.
     *
     * @param bits chaîne de bits représentant la trame brute.
     * @param hex  représentation hexadécimale de la trame.
     * @param text représentation texte (ASCII) de la trame.
     * @throws SQLException si une erreur survient lors de l'insertion.
     */
    public void insertTrame(String bits, String hex, String text) throws SQLException {
        String sql = "INSERT INTO frame_capture (raw_bits, raw_hexa, raw_text) VALUES (?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, bits);
            ps.setString(2, hex);
            ps.setString(3, text);
            ps.executeUpdate();
        }
    }

    /**
     * Supprime toutes les trames présentes dans la table {@code frame_capture}.
     *
     * @throws SQLException si une erreur survient lors de la suppression.
     */
    public void clearTrames() throws SQLException {
        String sql = "DELETE FROM frame_capture";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.executeUpdate();
        }
    }

    /**
     * Met à jour le champ {@code raw_text} d’une trame existante identifiée par sa valeur hexadécimale.
     *
     * @param hex      représentation hexadécimale de la trame cible.
     * @param newText  nouvelle valeur texte à associer.
     * @throws SQLException si une erreur survient lors de la mise à jour.
     */
    public void updateText(String hex, String newText) throws SQLException {
        String sql = "UPDATE frame_capture SET raw_text = ? WHERE raw_hexa = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newText);
            ps.setString(2, hex);
            ps.executeUpdate();
        }
    }
}