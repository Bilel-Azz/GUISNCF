package org.sncf.gui.services;

import org.sncf.gui.model.FilterRule;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Service permettant de traiter, filtrer, sauvegarder et charger des trames binaires.
 * Chaque trame est représentée sous forme binaire, hexadécimale et textuelle.
 */
public class TrameService {
    private final DatabaseManager db;
    private final DictionaryService dictionary;

    /**
     * Crée une instance du service de trames.
     *
     * @param db         gestionnaire de base de données {@link DatabaseManager}.
     * @param dictionary service d'interprétation textuelle des trames hexadécimales.
     */
    public TrameService(DatabaseManager db, DictionaryService dictionary) {
        this.db = db;
        this.dictionary = dictionary;
    }

    /**
     * Convertit une chaîne binaire en représentation hexadécimale et textuelle,
     * à l’aide du dictionnaire.
     *
     * @param bits chaîne binaire représentant la trame (ex: "0100100001100101...").
     * @return une nouvelle instance de {@link TrameEntry} contenant les trois représentations.
     */
    public TrameEntry processBits(String bits) {
        String hex = convertBitsToHex(bits);
        String text = dictionary.convertHexToText(hex);
        return new TrameEntry(bits, hex, text);
    }

    /**
     * Sauvegarde une trame dans la base de données via le {@link DatabaseManager}.
     *
     * @param entry trame à sauvegarder.
     */
    public void saveTrame(TrameEntry entry) {
        try {
            db.insertTrame(entry.bits, entry.hex, entry.text);
        } catch (Exception e) {
            System.err.println("Erreur enregistrement trame : " + e.getMessage());
        }
    }

    /**
     * Charge toutes les trames stockées dans la base de données.
     *
     * @return liste de trames récupérées, sous forme de {@link TrameEntry}.
     */
    public List<TrameEntry> loadAllFromDb() {
        List<TrameEntry> list = new ArrayList<>();
        try (Connection conn = db.getConnection()) {
            String sql = "SELECT raw_bits, raw_hexa, raw_text FROM frame_capture";
            try (PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new TrameEntry(
                            rs.getString("raw_bits"),
                            rs.getString("raw_hexa"),
                            rs.getString("raw_text")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur lecture BDD : " + e.getMessage());
        }
        return list;
    }

    /**
     * Vérifie si une trame correspond à au moins une des règles de filtrage.
     * Le filtrage s’applique sur les champs bits, hex ou texte.
     *
     * @param entry   trame à tester.
     * @param filters liste de règles de filtrage à appliquer.
     * @return true si au moins une règle correspond, false sinon.
     */
    public boolean matchesFilter(TrameEntry entry, List<FilterRule> filters) {
        if (filters == null) return true;
        for (FilterRule filter : filters) {
            if (entry.bits.contains(filter.pattern) ||
                    entry.hex.contains(filter.pattern) ||
                    entry.text.contains(filter.pattern)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Applique un ensemble de règles de filtrage à une liste de trames.
     *
     * @param source  liste initiale de trames.
     * @param filters liste de règles à appliquer.
     * @return liste filtrée de trames.
     */
    public List<TrameEntry> filterTrames(List<TrameEntry> source, List<FilterRule> filters) {
        if (filters == null || filters.isEmpty()) return source;
        List<TrameEntry> filtered = new ArrayList<>();
        for (TrameEntry t : source) {
            if (matchesFilter(t, filters)) filtered.add(t);
        }
        return filtered;
    }

    /**
     * Convertit une chaîne binaire en chaîne hexadécimale lisible (ex: "4A 2F").
     *
     * @param bitString chaîne de bits dont la longueur est un multiple de 8.
     * @return chaîne hexadécimale formatée ou chaîne vide si la longueur est invalide.
     */
    public String convertBitsToHex(String bitString) {
        if (bitString == null || bitString.length() % 8 != 0) return "";
        StringBuilder hex = new StringBuilder();
        for (int i = 0; i < bitString.length(); i += 8) {
            String byteStr = bitString.substring(i, i + 8);
            int byteVal = Integer.parseInt(byteStr, 2);
            hex.append(String.format("%02X ", byteVal));
        }
        return hex.toString().trim();
    }

    /**
     * Représente une trame complète avec trois formats :
     * binaire, hexadécimal, et texte interprété.
     */
    public static class TrameEntry {
        /**
         * Chaîne binaire représentant la trame.
         */
        public final String bits;

        /**
         * Représentation hexadécimale de la trame.
         */
        public final String hex;

        /**
         * Représentation textuelle (ASCII ou dictionnaire) de la trame.
         */
        public final String text;

        /**
         * Crée une nouvelle trame avec ses trois formes représentatives.
         *
         * @param bits chaîne de bits.
         * @param hex  chaîne hexadécimale.
         * @param text chaîne texte interprétée.
         */
        public TrameEntry(String bits, String hex, String text) {
            this.bits = bits;
            this.hex = hex;
            this.text = text;
        }
    }
}