// SerialTransmitter.java
package org.example.demo.serial;

import com.fazecast.jSerialComm.SerialPort;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

public class SerialTransmitter {

    private static volatile boolean listeningActive = false;
    private static volatile boolean simulationMode = false;
    private static volatile boolean stopSimulation = false;

    public static void setSimulationMode(boolean enabled) {
        simulationMode = enabled;
    }

    public static boolean isSimulationMode() {
        return simulationMode;
    }

    public static void stopSimulation() {
        stopSimulation = true;
        stopListening();
    }

    public static void stopListening() {
        listeningActive = false;
        stopSimulation = true;
    }

    public static void sendConfigAndListen(String portName, int baudrate, List<String> configLines, Consumer<String> onTrameReceived) {
        sendConfigAndListen(portName, baudrate, configLines, onTrameReceived, true);
    }

    public static void sendConfigAndListen(String portName, int baudrate, List<String> configLines, Consumer<String> onTrameReceived, boolean autoStopAfterTimeout) {
        if (simulationMode) {
            System.out.println("🧪 MODE SIMULATION ACTIVÉ");
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
                        saveTrameToDatabase(msg, convertBitsToHex(msg));
                        Thread.sleep(1000);
                    }
                } catch (InterruptedException ignored) {
                } finally {
                    System.out.println("🛑 Simulation arrêtée.");
                }
            }).start();
            return;
        }

        listeningActive = true;
        SerialPort serialPort = SerialPort.getCommPort(portName);
        serialPort.setBaudRate(baudrate);
        serialPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING, 1000, 0);

        if (!serialPort.openPort()) {
            System.err.println("❌ Impossible d’ouvrir le port " + portName);
            return;
        }

        System.out.println("✅ Port ouvert : " + portName);

        try (OutputStream out = serialPort.getOutputStream();
             InputStream in = serialPort.getInputStream()) {

            for (String line : configLines) {
                String data = line + "\n";
                out.write(data.getBytes(StandardCharsets.UTF_8));
                out.flush();
                System.out.println("📤 Envoyé : " + line);
                Thread.sleep(300);
            }

            System.out.println("📡 En attente de trames RS232 depuis ESP32...");

            long lastReceived = System.currentTimeMillis();
            StringBuilder buffer = new StringBuilder();

            while (listeningActive) {
                if (in.available() > 0) {
                    int c = in.read();
                    if (c == '\n') {
                        String line = buffer.toString();
                        System.out.println("📥 Reçu : " + line);
                        onTrameReceived.accept(line);
                        saveTrameToDatabase(line, convertBitsToHex(line));
                        buffer.setLength(0);
                    } else {
                        buffer.append((char) c);
                    }
                    lastReceived = System.currentTimeMillis();
                } else {
                    if (autoStopAfterTimeout && System.currentTimeMillis() - lastReceived > 10_000) {
                        System.out.println("⏱️ Inactivité > 10s. Fermeture du port.");
                        break;
                    }
                    Thread.sleep(100);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            serialPort.closePort();
            System.out.println("🔌 Port série fermé.");
        }
    }

    private static void saveTrameToDatabase(String bits, String hex) {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:bdd.db")) {
            String insertSQL = "INSERT INTO frame_capture (raw_frame, translated) VALUES (?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(insertSQL)) {
                stmt.setString(1, bits);
                stmt.setString(2, hex);
                stmt.executeUpdate();
            }
        } catch (Exception e) {
            System.err.println("❌ Erreur insertion trame: " + e.getMessage());
        }
    }

    private static String convertBitsToHex(String bits) {
        StringBuilder hex = new StringBuilder();
        for (int i = 0; i < bits.length(); i += 8) {
            String byteStr = bits.substring(i, Math.min(i + 8, bits.length()));
            int decimal = Integer.parseInt(byteStr, 2);
            hex.append(String.format("%02X ", decimal));
        }
        return hex.toString().trim();
    }

    public static void sendConfigOnly(String portName, int baudrate, List<String> configLines) {
        SerialPort serialPort = SerialPort.getCommPort(portName);
        serialPort.setBaudRate(baudrate);

        if (!serialPort.openPort()) {
            System.err.println("❌ Impossible d’ouvrir le port " + portName);
            return;
        }

        try (OutputStream out = serialPort.getOutputStream()) {
            for (String line : configLines) {
                String data = line + "\n";
                out.write(data.getBytes(StandardCharsets.UTF_8));
                out.flush();
                System.out.println("📤 Envoyé (config) : " + line);
                Thread.sleep(300);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            serialPort.closePort();
            System.out.println("🔌 Port fermé après envoi config");
        }
    }
}
