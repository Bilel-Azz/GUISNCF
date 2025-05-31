package org.sncf.gui.services;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;


/**
 * Service pour gérer un dictionnaire de correspondances entre motifs hexadécimaux
 * et leurs descriptions textuelles. Permet l'ajout, la recherche, la conversion,
 * et le chargement complet du dictionnaire depuis une base SQLite.
 */
public class DictionaryService {
    private final DatabaseManager db;

    /**
     * Crée une instance du service de dictionnaire.
     *
     * @param db une instance de {@link DatabaseManager} pour accéder à la base de données.
     */
    public DictionaryService(DatabaseManager db) {
        this.db = db;
    }

    /**
     * Ajoute une nouvelle entrée dans le dictionnaire avec un motif hexadécimal
     * et une description associée.
     *
     * @param hex   motif hexadécimal (ex: "4A 2F").
     * @param label description ou étiquette à associer.
     * @throws SQLException si une erreur survient lors de l'insertion.
     */
    public void addEntry(String hex, String label) throws SQLException {
        try (Connection conn = db.getConnection()) {
            String sql = "INSERT INTO dictionary (hex_pattern, description) VALUES (?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, hex.trim());
                ps.setString(2, label.trim());
                ps.executeUpdate();
            }
        }
    }

    /**
     * Recherche une description dans le dictionnaire à partir d'un motif hexadécimal.
     * Les espaces et la casse sont ignorés.
     *
     * @param hex motif hexadécimal à rechercher.
     * @return la description correspondante si trouvée, sinon {@code null}.
     */
    public String lookup(String hex) {
        String normalized = hex.replaceAll("\\s+", "").toUpperCase();
        try (Connection conn = db.getConnection()) {
            String sql = "SELECT description FROM dictionary WHERE REPLACE(UPPER(hex_pattern), ' ', '') = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, normalized);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    return rs.getString("description");
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lookup dictionary : " + e.getMessage());
        }
        return null;
    }

    /**
     * Convertit une chaîne hexadécimale en texte ASCII si possible.
     * Si une correspondance est trouvée dans le dictionnaire, elle est utilisée.
     * Sinon, chaque octet est converti en caractère imprimable ou remplacé par '.'.
     *
     * @param hexLine la chaîne hexadécimale à convertir (ex: "48 65 6C 6C 6F").
     * @return la chaîne texte correspondante.
     */
    public String convertHexToText(String hexLine) {
        if (hexLine == null || hexLine.isEmpty()) return "";

        String[] tokens = hexLine.trim().split("\\s+");
        StringBuilder result = new StringBuilder();

        // Charger le dictionnaire dans une map
        Map<String, String> dict = new HashMap<>();
        for (DictionaryService.DictionaryEntry entry : getAllEntries()) {
            dict.put(entry.hexPattern.trim().toUpperCase(), entry.description);
        }

        int i = 0;
        while (i < tokens.length) {
            boolean matched = false;

            // Tente de faire correspondre les séquences les plus longues du dictionnaire
            for (int len = tokens.length - i; len > 0; len--) {
                StringBuilder seqBuilder = new StringBuilder();
                for (int j = 0; j < len; j++) {
                    if (j > 0) seqBuilder.append(" ");
                    seqBuilder.append(tokens[i + j]);
                }
                String candidate = seqBuilder.toString().toUpperCase();

                if (dict.containsKey(candidate)) {
                    result.append(dict.get(candidate)).append(" ");
                    i += len;
                    matched = true;
                    break;
                }
            }

            // Si aucun match dictionnaire, essaie de convertir en ASCII
            if (!matched) {
                try {
                    int value = Integer.parseInt(tokens[i], 16);
                    if (value >= 32 && value <= 126) {
                        result.append((char) value).append(" ");
                    } else {
                        result.append(". ");
                    }
                } catch (NumberFormatException e) {
                    result.append(". ");
                }
                i++;
            }
        }

        return result.toString().trim();
    }

    /**
     * Charge toutes les entrées du dictionnaire depuis la base de données.
     *
     * @return liste de toutes les entrées du dictionnaire.
     */
    public List<DictionaryEntry> loadAll() {
        List<DictionaryEntry> list = new ArrayList<>();
        try (Connection conn = db.getConnection()) {
            String sql = "SELECT hex_pattern, description FROM dictionary";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    String pattern = rs.getString("hex_pattern").replaceAll("\\s+", "").toUpperCase();
                    String desc = rs.getString("description");
                    list.add(new DictionaryEntry(pattern, desc));
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur chargement dictionnaire : " + e.getMessage());
        }
        return list;
    }


    /**
     * Supprime une entrée spécifique du dictionnaire à partir de sa clé hexadécimale.
     *
     * @param hex la chaîne hexadécimale de l’entrée à supprimer (par ex. "4A 5E").
     * @throws RuntimeException si une erreur SQL survient.
     */
    public void deleteEntry(String hex) {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM dictionary WHERE hex_pattern = ?")) {
            ps.setString(1, hex.trim());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erreur suppression dictionnaire: " + e.getMessage(), e);
        }
    }

    /**
     * Récupère toutes les entrées actuellement stockées dans le dictionnaire.
     *
     * @return une liste d'objets {@link DictionaryEntry} représentant chaque
     *         paire hexadécimal → texte.
     * @throws RuntimeException si une erreur SQL survient lors de la lecture.
     */
    public List<DictionaryEntry> getAllEntries() {
        List<DictionaryEntry> entries = new ArrayList<>();
        try (Connection conn = db.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT hex_pattern, description FROM dictionary")) {
            while (rs.next()) {
                entries.add(new DictionaryEntry(rs.getString("hex_pattern"), rs.getString("description")));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la récupération du dictionnaire", e);
        }
        return entries;
    }

    /**
     * Représente une entrée du dictionnaire, contenant un motif hexadécimal
     * et sa description associée.
     */
    public static class DictionaryEntry {
        /**
         * Motif hexadécimal de l’entrée (sans espaces, en majuscules).
         */
        public final String hexPattern;

        /**
         * Description associée à l’entrée.
         */
        public final String description;

        /**
         * Construit une nouvelle entrée du dictionnaire.
         *
         * @param hexPattern  motif hexadécimal.
         * @param description description associée.
         */
        public DictionaryEntry(String hexPattern, String description) {
            this.hexPattern = hexPattern;
            this.description = description;
        }
    }
}