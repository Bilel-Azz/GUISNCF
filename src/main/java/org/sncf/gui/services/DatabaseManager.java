package org.sncf.gui.services;

import java.io.InputStream;
import java.nio.file.*;
import java.sql.*;

/**
 * Classe utilitaire pour gérer les interactions avec la base de données SQLite.
 * Elle permet d'insérer, de supprimer et de mettre à jour des trames dans la table
 * {@code frame_capture}.
 */
public class DatabaseManager {

    /**
     * Génère dynamiquement le chemin de la base de données selon le système d’exploitation.
     *
     * @return URL JDBC de la base de données.
     */
    public static String getDbUrl() {
        String os = System.getProperty("os.name").toLowerCase();
        String userHome = System.getProperty("user.home");
        String dbPath;

        if (os.contains("win")) {
            dbPath = System.getenv("APPDATA") + "\\AnalyseTram\\bdd.db";
        } else if (os.contains("mac")) {
            dbPath = userHome + "/Library/Application Support/AnalyseTram/bdd.db";
        } else {
            dbPath = userHome + "/.local/share/AnalyseTram/bdd.db";
        }

        return "jdbc:sqlite:" + dbPath;
    }

    /**
     * Obtient une connexion JDBC à la base de données SQLite {@code bdd.db}.
     *
     * @return connexion à la base de données.
     * @throws SQLException en cas d'erreur de connexion.
     */
    public Connection getConnection() throws SQLException {
        try {
            Path dbFile = Paths.get(getDbUrl().replace("jdbc:sqlite:", ""));
            if (Files.notExists(dbFile)) {
                Files.createDirectories(dbFile.getParent());

                // Tente de copier une BDD de modèle embarquée (si disponible dans les ressources du jar)
                InputStream in = DatabaseManager.class.getResourceAsStream("/bdd.db");
                if (in != null) {
                    Files.copy(in, dbFile);
                } else {
                    System.err.println("Aucune BDD modèle embarquée trouvée, création à vide.");
                }
            }
        } catch (Exception e) {
            throw new SQLException("Erreur d'initialisation de la base de données", e);
        }

        return DriverManager.getConnection(getDbUrl());
    }

    /**
     * Initialise la base de données :
     * - crée le dossier si nécessaire
     * - copie un modèle embarqué si disponible
     * - sinon crée la BDD vide avec les 4 tables requises
     */
    public static void initializeDatabase() throws SQLException {
        try {
            Path dbFile = Paths.get(getDbUrl().replace("jdbc:sqlite:", ""));
            Path dbDir = dbFile.getParent();

            if (Files.notExists(dbDir)) {
                Files.createDirectories(dbDir);
            }

            if (Files.notExists(dbFile)) {
                System.out.println("Création de la base de données...");

                try (InputStream in = DatabaseManager.class.getResourceAsStream("/bdd.db")) {
                    if (in != null) {
                        Files.copy(in, dbFile, StandardCopyOption.REPLACE_EXISTING);
                        System.out.println("BDD copiée depuis les ressources : " + dbFile);
                    } else {
                        System.out.println("Aucun modèle trouvé, création d'une BDD vide avec les tables nécessaires...");
                        createEmptyDatabase(dbFile.toString());
                    }
                }
            }

        } catch (Exception e) {
            throw new SQLException("Échec d'initialisation de la base de données", e);
        }
    }

    /**
     * Crée une base de données SQLite vide et y initialise les 4 tables :
     * {@code frame_capture}, {@code port_config}, {@code custom_filter} et {@code dictionary}.
     *
     * @param dbPath chemin vers le fichier de la base de données.
     * @throws SQLException en cas d'erreur de création ou d'exécution SQL.
     */
    private static void createEmptyDatabase(String dbPath) throws SQLException {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath)) {
            Statement stmt = conn.createStatement();

            // Table frame_capture
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS frame_capture (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    raw_bits TEXT,
                    raw_hexa TEXT,
                    raw_text TEXT,
                    timestamp TEXT NOT NULL
                );
            """);

            // Table port_config
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS port_config (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    baudrate TEXT,
                    parity TEXT,
                    databits TEXT,
                    stopbits TEXT
                );
            """);

            // Table custom_filter
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS custom_filter (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    color TEXT,
                    name TEXT,
                    pattern TEXT
                );
            """);

            // Table dictionary
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS dictionary (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    traduction TEXT NOT NULL,
                    hex_pattern TEXT NOT NULL
                );
            """);

            System.out.println("Base initialisée avec les 4 tables nécessaires.");
        }
    }

    /**
     * Insère une trame dans la table {@code frame_capture}.
     *
     * @param bits données binaires de la trame.
     * @param hex représentation hexadécimale de la trame.
     * @param text texte brut de la trame.
     * @throws SQLException en cas d'erreur lors de l'insertion.
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
     * Supprime toutes les trames de la table {@code frame_capture}.
     *
     * @throws SQLException en cas d'erreur lors de la suppression.
     */
    public void clearTrames() throws SQLException {
        String sql = "DELETE FROM frame_capture";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.executeUpdate();
        }
    }

    /**
     * Met à jour le texte associé à une trame donnée dans la table {@code frame_capture}.
     *
     * @param hex      représentation hexadécimale de la trame cible.
     * @param newText  nouveau texte à associer à la trame.
     * @throws SQLException en cas d'erreur lors de la mise à jour.
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