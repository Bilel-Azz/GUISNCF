package org.sncf.gui.serial;

import com.fazecast.jSerialComm.SerialPort;
import org.sncf.gui.services.DatabaseManager;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

/**
 * Classe utilitaire pour transmettre des configurations à un appareil série (ESP32)
 * et écouter les trames reçues. Elle permet aussi de simuler la réception de trames
 * pour des tests hors ligne.
 *
 * <p>Utilise la bibliothèque jSerialComm pour la communication série.</p>
 */
public class SerialTransmitter {

    private static volatile boolean listeningActive = false;
    private static volatile boolean simulationMode = false;
    private static volatile boolean stopSimulation = false;

    /**
     * Active ou désactive le mode simulation.
     *
     * @param enabled true pour activer le mode simulation, false pour le désactiver.
     */
    public static void setSimulationMode(boolean enabled) {
        simulationMode = enabled;
    }

    /**
     * Indique si le mode simulation est actuellement actif.
     *
     * @return true si le mode simulation est activé, false sinon.
     */
    public static boolean isSimulationMode() {
        return simulationMode;
    }

    /**
     * Arrête le mode simulation (et l'écoute série si elle est active).
     */
    public static void stopSimulation() {
        stopSimulation = true;
        stopListening();
    }

    /**
     * Arrête l'écoute série en cours et la simulation si active.
     */
    public static void stopListening() {
        listeningActive = false;
        stopSimulation = true;
    }

    /**
     * Convertit une chaîne binaire en texte ASCII.
     *
     * @param bits chaîne de bits (doit être multiple de 8 pour un encodage complet).
     * @return texte correspondant (caractères non imprimables remplacés par ".").
     */
    private static String convertBitsToText(String bits) {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i + 7 < bits.length(); i += 8) {
            String byteStr = bits.substring(i, i + 8);
            int decimal = Integer.parseInt(byteStr, 2);
            if (decimal >= 32 && decimal <= 126) {
                text.append((char) decimal);
            } else {
                text.append("."); // caractère non imprimable
            }
        }
        return text.toString();
    }

    /**
     * Envoie une configuration à un port série, puis écoute les trames reçues
     * et appelle un callback pour chaque trame. Peut fonctionner en mode simulation.
     *
     * @param portName              nom du port série (ex: "COM3", "/dev/ttyUSB0").
     * @param baudrate              débit en bauds (ex: 9600, 115200).
     * @param configLines           liste des lignes de configuration à envoyer.
     * @param onTrameReceived       fonction appelée pour chaque trame reçue.
     * @param autoStopAfterTimeout  true pour arrêter l'écoute après 10s d'inactivité.
     */
    public static void sendConfigAndListen(String portName, int baudrate, List<String> configLines, Consumer<String> onTrameReceived, boolean autoStopAfterTimeout) {
        if (simulationMode) {
            System.out.println("MODE SIMULATION ACTIVÉ");
            stopSimulation = false;
            listeningActive = true;

            new Thread(() -> {
                Random random = new Random();
                try {
                    while (listeningActive && !stopSimulation) {
                        StringBuilder bits = new StringBuilder();
                        for (int i = 0; i < 40; i++) {
                            bits.append(random.nextBoolean() ? "1" : "0");
                        }
                        String msg = bits.toString();
                        onTrameReceived.accept(msg);
                        Thread.sleep(1000);
                    }
                } catch (InterruptedException ignored) {
                } finally {
                    System.out.println("Simulation arrêtée.");
                }
            }).start();
            return;
        }

        listeningActive = true;
        SerialPort serialPort = SerialPort.getCommPort(portName);
        serialPort.setBaudRate(baudrate);
        serialPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING, 1000, 0);

        if (!serialPort.openPort()) {
            System.err.println("Impossible d’ouvrir le port " + portName);
            return;
        }

        System.out.println("Port ouvert : " + portName);

        try (OutputStream out = serialPort.getOutputStream();
             InputStream in = serialPort.getInputStream()) {

            for (String line : configLines) {
                String data = line + "\n";
                out.write(data.getBytes(StandardCharsets.UTF_8));
                out.flush();
                System.out.println("Envoyé : " + line);
                Thread.sleep(300);
            }

            // Attente du READY_TO_SNIFF
            System.out.println("Attente de confirmation ESP32 (READY_TO_SNIFF)...");
            long start = System.currentTimeMillis();
            StringBuilder buffer = new StringBuilder();
            boolean espReady = false;

            while (System.currentTimeMillis() - start < 5000) {
                if (in.available() > 0) {
                    int c = in.read();
                    if (c == '\n') {
                        String lineRead = buffer.toString().trim();
                        buffer.setLength(0);
                        System.out.println("ESP32 dit : " + lineRead);
                        if (lineRead.contains("READY_TO_SNIFF")) {
                            espReady = true;
                            break;
                        }
                    } else {
                        buffer.append((char) c);
                    }
                } else {
                    Thread.sleep(50);
                }
            }

            /*if (!espReady) {
                System.err.println("Aucun message READY_TO_SNIFF reçu de l’ESP32.");
                serialPort.closePort();
                return;
            }*/

            System.out.println("ESP32 prêt. Démarrage de la réception des trames...");
            buffer.setLength(0);
            long lastReceived = System.currentTimeMillis();

            while (listeningActive) {
                if (in.available() > 0) {
                    int c = in.read();
                    if (c == '\n') {
                        String line = buffer.toString();
                        System.out.println("Reçu : " + line);
                        onTrameReceived.accept(line);
                        saveTrameToDatabase(line, convertBitsToHex(line));
                        buffer.setLength(0);
                        lastReceived = System.currentTimeMillis();
                    } else {
                        buffer.append((char) c);
                    }
                } else {
                    if (autoStopAfterTimeout && System.currentTimeMillis() - lastReceived > 10000) {
                        System.out.println("Inactivité > 10s. Fermeture du port.");
                        break;
                    }
                    Thread.sleep(100);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            serialPort.closePort();
            System.out.println("Port série fermé.");
        }
    }

    /**
     * Sauvegarde une trame dans une base de données SQLite locale (bdd.db).
     * Les champs sauvegardés sont les bits, l'équivalent hexadécimal et texte.
     *
     * @param bits trame binaire.
     * @param hex  trame en représentation hexadécimale.
     */
    private static void saveTrameToDatabase(String bits, String hex) {
        try (Connection conn = DriverManager.getConnection(DatabaseManager.getDbUrl())) {
            String insertSQL = "INSERT INTO frame_capture (raw_bits, raw_hexa, raw_text) VALUES (?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(insertSQL)) {
                stmt.setString(1, bits);
                stmt.setString(2, hex);
                stmt.setString(3, convertBitsToText(bits));
                stmt.executeUpdate();
            }
        } catch (Exception e) {
            System.err.println("Erreur insertion trame: " + e.getMessage());
        }
    }

    /**
     * Convertit une chaîne de bits en une chaîne hexadécimale lisible.
     *
     * @param bits chaîne binaire à convertir.
     * @return chaîne hexadécimale équivalente (ex: "4A 3F").
     */
    private static String convertBitsToHex(String bits) {
        StringBuilder hex = new StringBuilder();
        for (int i = 0; i < bits.length(); i += 8) {
            String byteStr = bits.substring(i, Math.min(i + 8, bits.length()));
            int decimal = Integer.parseInt(byteStr, 2);
            hex.append(String.format("%02X ", decimal));
        }
        return hex.toString().trim();
    }

    /**
     * Envoie uniquement une configuration sur un port série, sans écouter les réponses.
     *
     * @param portName    nom du port série.
     * @param baudrate    débit en bauds.
     * @param configLines lignes de configuration à envoyer.
     */
    public static void sendConfigOnly(String portName, int baudrate, List<String> configLines) {
        SerialPort serialPort = SerialPort.getCommPort(portName);
        serialPort.setBaudRate(baudrate);

        if (!serialPort.openPort()) {
            System.err.println("Impossible d’ouvrir le port " + portName);
            return;
        }

        try (OutputStream out = serialPort.getOutputStream()) {
            for (String line : configLines) {
                String data = line + "\n";
                out.write(data.getBytes(StandardCharsets.UTF_8));
                out.flush();
                System.out.println("Envoyé (config) : " + line);
                Thread.sleep(300);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            serialPort.closePort();
            System.out.println("Port fermé après envoi config");
        }
    }
}