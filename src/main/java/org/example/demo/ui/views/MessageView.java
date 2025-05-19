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
    private JTextPane textPane;
    private final JButton toggleSimulationBtn;
    private final JButton toggleViewBtn;
    private final List<TrameEntry> trames = new ArrayList<>();
    private boolean showText = false;

    public MessageView() {
        setLayout(new BorderLayout());

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        toggleSimulationBtn = new JButton(getButtonLabel());
        toggleSimulationBtn.addActionListener(e -> toggleSimulation());
        toggleViewBtn = new JButton("Vue: Hex");
        toggleViewBtn.addActionListener(e -> toggleView());
        
        topPanel.add(toggleViewBtn);
        topPanel.add(toggleSimulationBtn);

        bitPane = new JTextPane();
        bitPane.setFont(new Font("Monospaced", Font.PLAIN, 12));
        bitPane.setEditable(false);

        hexPane = new JTextPane();
        hexPane.setFont(new Font("Monospaced", Font.PLAIN, 12));
        hexPane.setEditable(false);

        textPane = new JTextPane();
        textPane.setFont(new Font("Monospaced", Font.PLAIN, 12));
        textPane.setEditable(false);

        JPanel bitPanel = new JPanel(new BorderLayout());
        bitPanel.add(new JLabel("Bits reçus"), BorderLayout.NORTH);
        bitPanel.add(new JScrollPane(bitPane), BorderLayout.CENTER);

        JPanel hexPanel = new JPanel(new BorderLayout());
        hexPanel.add(new JLabel("Traduction hexadécimale"), BorderLayout.NORTH);
        hexPanel.add(new JScrollPane(hexPane), BorderLayout.CENTER);

        JPanel textPanel = new JPanel(new BorderLayout());
        textPanel.add(new JLabel("Traduction texte"), BorderLayout.NORTH);
        textPanel.add(new JScrollPane(textPane), BorderLayout.CENTER);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, bitPanel, showText ? textPanel : hexPanel);
        splitPane.setResizeWeight(0.5);

        add(topPanel, BorderLayout.NORTH);
        add(splitPane, BorderLayout.CENTER);
    }

    private void toggleView() {
        showText = !showText;
        toggleViewBtn.setText(showText ? "Vue: Texte" : "Vue: Hex");
        refreshView();
    }

    private void refreshView() {
        JSplitPane splitPane = (JSplitPane) getComponent(1);
        JPanel rightPanel = (JPanel) splitPane.getRightComponent();
        rightPanel.removeAll();
        
        if (showText) {
            rightPanel.add(new JLabel("Traduction texte"), BorderLayout.NORTH);
            rightPanel.add(new JScrollPane(textPane), BorderLayout.CENTER);
        } else {
            rightPanel.add(new JLabel("Traduction hexadécimale"), BorderLayout.NORTH);
            rightPanel.add(new JScrollPane(hexPane), BorderLayout.CENTER);
        }
        
        rightPanel.revalidate();
        rightPanel.repaint();
    }

    public void appendMessage(String bits) {
        String hex = convertBitsToHex(bits);
        String text = convertHexToText(hex);
        trames.add(new TrameEntry(bits, hex, text));
        appendText(bitPane, bits + "\n");
        appendText(hexPane, hex + "\n");
        appendText(textPane, text + "\n");
        saveTrameToDatabase(bits, hex);
    }

    private String convertHexToText(String hex) {
        StringBuilder text = new StringBuilder();
        String[] hexBytes = hex.split(" ");
        for (String hexByte : hexBytes) {
            try {
                int value = Integer.parseInt(hexByte, 16);
                // Vérifier si c'est un caractère ASCII imprimable
                if (value >= 32 && value <= 126) {
                    text.append((char) value);
                } else {
                    text.append(".");
                }
            } catch (NumberFormatException e) {
                text.append(".");
            }
        }
        return text.toString();
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
        textPane.setText("");
        trames.clear();
    }

    private static class TrameEntry {
        String bits, hex, text;
        TrameEntry(String bits, String hex, String text) {
            this.bits = bits;
            this.hex = hex;
            this.text = text;
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
        textPane.setText("");
        Highlighter bitsHighlighter = bitPane.getHighlighter();
        Highlighter hexHighlighter = hexPane.getHighlighter();
        Highlighter textHighlighter = textPane.getHighlighter();
        bitsHighlighter.removeAllHighlights();
        hexHighlighter.removeAllHighlights();
        textHighlighter.removeAllHighlights();

        try {
            for (TrameEntry entry : trames) {
                int bitStartOffset = bitPane.getDocument().getLength();
                int hexStartOffset = hexPane.getDocument().getLength();
                int textStartOffset = textPane.getDocument().getLength();

                appendText(bitPane, entry.bits + "\n");
                appendText(hexPane, entry.hex + "\n");
                appendText(textPane, entry.text + "\n");

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
