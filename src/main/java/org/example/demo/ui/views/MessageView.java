// MessageView.java
package org.example.demo.ui.views;

import org.example.demo.serial.SerialTransmitter;
import org.example.demo.model.FilterRule;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageView extends JPanel {
    private JTextPane bitPane;
    private JTextPane hexPane;
    private final JButton toggleSimulationBtn;
    private final List<TrameEntry> trames = new ArrayList<>();

    public MessageView() {
        setLayout(new BorderLayout());

        toggleSimulationBtn = new JButton(getButtonLabel());
        toggleSimulationBtn.addActionListener(e -> toggleSimulation());

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        topPanel.add(toggleSimulationBtn);

        bitPane = new JTextPane();
        bitPane.setFont(new Font("Monospaced", Font.PLAIN, 12));
        bitPane.setEditable(false);

        hexPane = new JTextPane();
        hexPane.setFont(new Font("Monospaced", Font.PLAIN, 12));
        hexPane.setEditable(false);

        JPanel bitPanel = new JPanel(new BorderLayout());
        bitPanel.add(new JLabel("Bits reçus"), BorderLayout.NORTH);
        bitPanel.add(new JScrollPane(bitPane), BorderLayout.CENTER);

        JPanel hexPanel = new JPanel(new BorderLayout());
        hexPanel.add(new JLabel("Traduction hexadécimale"), BorderLayout.NORTH);
        hexPanel.add(new JScrollPane(hexPane), BorderLayout.CENTER);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, bitPanel, hexPanel);
        splitPane.setResizeWeight(0.5);

        add(topPanel, BorderLayout.NORTH);
        add(splitPane, BorderLayout.CENTER);
    }

    public void appendMessage(String bits) {
        String hex = convertBitsToHex(bits);
        trames.add(new TrameEntry(bits, hex));
        appendText(bitPane, bits + "\n");
        appendText(hexPane, hex + "\n");
        saveTrameToDatabase(bits, hex);
    }

    private void appendText(JTextPane pane, String text) {
        StyledDocument doc = pane.getStyledDocument();
        try {
            doc.insertString(doc.getLength(), text, null);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    public void clearMessages() {
        bitPane.setText("");
        hexPane.setText("");
        trames.clear();
    }

    private static class TrameEntry {
        String bits, hex;
        TrameEntry(String bits, String hex) {
            this.bits = bits;
            this.hex = hex;
        }
    }

    private void toggleSimulation() {
        boolean current = SerialTransmitter.isSimulationMode();
        if (current) SerialTransmitter.stopSimulation();
        SerialTransmitter.setSimulationMode(!current);
        toggleSimulationBtn.setText(getButtonLabel());
    }

    private String getButtonLabel() {
        return SerialTransmitter.isSimulationMode() ? "Simulation: ON ⏸" : "Simulation: OFF ▶";
    }

    private String convertBitsToHex(String bitString) {
        if (bitString == null || bitString.length() % 8 != 0) return "";
        StringBuilder hex = new StringBuilder();
        for (int i = 0; i < bitString.length(); i += 8) {
            String byteStr = bitString.substring(i, Math.min(i + 8, bitString.length()));
            int byteVal = Integer.parseInt(byteStr, 2);
            hex.append(String.format("%02X ", byteVal));
        }
        return hex.toString().trim();
    }

    private void saveTrameToDatabase(String bits, String hex) {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:bdd.db")) {
            String sql = "INSERT INTO frame_capture (raw_frame, translated) VALUES (?, ?)";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, bits);
            ps.setString(2, hex);
            ps.executeUpdate();
        } catch (Exception e) {
            System.err.println("❌ Erreur enregistrement trame: " + e.getMessage());
        }
    }

    public void refreshWithFilters(List<FilterRule> activeFilters) {
        bitPane.setText("");
        hexPane.setText("");
        Highlighter bitsHighlighter = bitPane.getHighlighter();
        Highlighter hexHighlighter = hexPane.getHighlighter();
        bitsHighlighter.removeAllHighlights();
        hexHighlighter.removeAllHighlights();

        try {
            for (TrameEntry entry : trames) {
                int bitStartOffset = bitPane.getDocument().getLength();
                int hexStartOffset = hexPane.getDocument().getLength();

                appendText(bitPane, entry.bits + "\n");
                appendText(hexPane, entry.hex + "\n");

                List<String> bitsChunks = new ArrayList<>();
                for (int i = 0; i < entry.bits.length(); i += 8) {
                    bitsChunks.add(entry.bits.substring(i, Math.min(i + 8, entry.bits.length())));
                }

                String[] hexTokens = entry.hex.split(" ");

                for (FilterRule filter : activeFilters) {
                    if (filter.pattern.matches("[01]+")) {
                        int idx = entry.bits.indexOf(filter.pattern);
                        if (idx >= 0) {
                            int start = bitStartOffset + idx;
                            int end = start + filter.pattern.length();
                            bitsHighlighter.addHighlight(start, end, new DefaultHighlighter.DefaultHighlightPainter(filter.color));

                            int byteStart = idx / 8;
                            int byteEnd = (idx + filter.pattern.length() - 1) / 8;
                            for (int i = byteStart; i <= byteEnd && i < hexTokens.length; i++) {
                                int hexTokenOffset = hexStartOffset + getHexOffset(hexTokens, i);
                                hexHighlighter.addHighlight(hexTokenOffset, hexTokenOffset + hexTokens[i].length(), new DefaultHighlighter.DefaultHighlightPainter(filter.color));
                            }
                        }
                    } else if (filter.pattern.matches("[0-9A-Fa-f]{2}")) {
                        for (int i = 0; i < hexTokens.length; i++) {
                            if (hexTokens[i].equalsIgnoreCase(filter.pattern)) {
                                int hexTokenOffset = hexStartOffset + getHexOffset(hexTokens, i);
                                hexHighlighter.addHighlight(hexTokenOffset, hexTokenOffset + hexTokens[i].length(), new DefaultHighlighter.DefaultHighlightPainter(filter.color));

                                int bitIdx = i * 8;
                                if (bitIdx < entry.bits.length()) {
                                    int bitEnd = Math.min(bitIdx + 8, entry.bits.length());
                                    bitsHighlighter.addHighlight(bitStartOffset + bitIdx, bitStartOffset + bitEnd, new DefaultHighlighter.DefaultHighlightPainter(filter.color));
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int getHexOffset(String[] hexTokens, int index) {
        int offset = 0;
        for (int i = 0; i < index; i++) {
            offset += hexTokens[i].length() + 1;
        }
        return offset;
    }
}
