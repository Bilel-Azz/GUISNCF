// PortConfigSelectorPanel.java
package org.example.demo.ui.components;

import com.fazecast.jSerialComm.SerialPort;
import org.example.demo.serial.SerialTransmitter;

import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.util.List;
import java.util.function.Consumer;

public class PortConfigSelectorPanel extends JPanel {
    private JButton dropdownButton;
    private JButton sendButton;
    private JButton listenButton;
    private JPopupMenu dropdownMenu;
    private int selectedConfigId = -1;
    private String selectedConfigLabel = "";
    private JComboBox<String> portSelector;

    private boolean listening = false;
    private Thread listenThread;

    private final Consumer<String> onTrameReceived;

    public PortConfigSelectorPanel(Runnable onSendInit, Consumer<String> onTrameReceived) {
        this.onTrameReceived = onTrameReceived;
        setLayout(new FlowLayout(FlowLayout.LEFT, 5, 0));
        setBackground(Color.WHITE);

        dropdownButton = new JButton("Configuration Port ▼");
        dropdownButton.setFocusPainted(false);
        dropdownButton.setBackground(new Color(245, 245, 245));
        dropdownButton.setBorder(BorderFactory.createCompoundBorder(
                new RoundRectBorder(10),
                BorderFactory.createEmptyBorder(5, 12, 5, 12)
        ));

        sendButton = new JButton("Envoyer la config");
        sendButton.setFocusPainted(false);
        sendButton.setBackground(new Color(220, 245, 255));
        sendButton.setBorder(BorderFactory.createCompoundBorder(
                new RoundRectBorder(10),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));

        listenButton = new JButton("▶ Écouter");
        listenButton.setFocusPainted(false);
        listenButton.setBackground(new Color(230, 250, 230));
        listenButton.setBorder(BorderFactory.createCompoundBorder(
                new RoundRectBorder(10),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));

        portSelector = new JComboBox<>();
        portSelector.setPreferredSize(new Dimension(120, 25));
        refreshSerialPorts();

        dropdownMenu = new JPopupMenu();
        dropdownMenu.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));

        dropdownButton.addActionListener(e -> dropdownMenu.show(dropdownButton, 0, dropdownButton.getHeight()));

        sendButton.addActionListener(e -> {
            if (selectedConfigId == -1) {
                JOptionPane.showMessageDialog(this, "Veuillez d'abord sélectionner une configuration.");
                return;
            }
            String portName = (String) portSelector.getSelectedItem();
            if (portName == null) {
                JOptionPane.showMessageDialog(this, "Aucun port série sélectionné.");
                return;
            }
            List<String> configLines = getConfigLinesById(selectedConfigId);

            if (SerialTransmitter.isSimulationMode()) {
                // Ne rien faire ici — la simulation ne démarre que sur écoute
                JOptionPane.showMessageDialog(this, "Configuration simulée prête. Appuyez sur ▶ pour démarrer.");
            } else {
                SerialTransmitter.sendConfigOnly(portName, 115200, configLines);
            }
        });

        listenButton.addActionListener(e -> toggleListening());

        add(dropdownButton);
        add(portSelector);
        add(sendButton);
        add(listenButton);
        reloadConfigs();
    }

    private void toggleListening() {
        if (listening) {
            SerialTransmitter.stopListening();
            listening = false;
            listenButton.setText("▶ Écouter");
        } else {
            if (selectedConfigId == -1) {
                JOptionPane.showMessageDialog(this, "Veuillez d'abord sélectionner une configuration.");
                return;
            }
            String portName = (String) portSelector.getSelectedItem();
            if (portName == null) {
                JOptionPane.showMessageDialog(this, "Aucun port série sélectionné.");
                return;
            }
            List<String> configLines = getConfigLinesById(selectedConfigId);
            listening = true;
            listenButton.setText("⏸ Pause");
            listenThread = new Thread(() ->
                    SerialTransmitter.sendConfigAndListen(portName, 115200, configLines, onTrameReceived, false)
            );
            listenThread.start();
        }
    }

    private void refreshSerialPorts() {
        portSelector.removeAllItems();
        for (SerialPort port : SerialPort.getCommPorts()) {
            portSelector.addItem(port.getSystemPortName());
        }
    }

    public void reloadConfigs() {
        dropdownMenu.removeAll();

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:bdd.db")) {
            String query = "SELECT * FROM port_config";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(query);

            while (rs.next()) {
                int id = rs.getInt("id");
                int baudrate = rs.getInt("baudrate");
                String parity = rs.getString("parity");
                int databits = rs.getInt("databits");
                int stopbits = rs.getInt("stopbits");

                String label = String.format("Config %d : %d - %s - %d/%d", id, baudrate, parity, databits, stopbits);

                JMenu configItem = new JMenu(label);
                configItem.setFont(new Font("SansSerif", Font.PLAIN, 13));
                configItem.setIcon(null);

                JMenuItem selectItem = new JMenuItem("✅ Sélectionner");
                selectItem.setIcon(null);
                selectItem.addActionListener(e -> {
                    selectedConfigId = id;
                    selectedConfigLabel = label;
                    dropdownButton.setText(label + " ▼");
                    System.out.println("Sélectionné : " + label);
                });

                JMenuItem editItem = new JMenuItem("✏️ Modifier");
                editItem.setIcon(null);
                editItem.addActionListener(e -> {
                    JFrame parent = (JFrame) SwingUtilities.getWindowAncestor(this);
                    new org.example.demo.ui.dialogs.EditPortConfigDialog(parent, id).setVisible(true);
                    reloadConfigs();
                });

                JMenuItem deleteItem = new JMenuItem("🗑️ Supprimer");
                deleteItem.setIcon(null);
                deleteItem.addActionListener(e -> {
                    int confirm = JOptionPane.showConfirmDialog(this,
                            "Supprimer cette configuration ?", "Confirmation", JOptionPane.YES_NO_OPTION);
                    if (confirm == JOptionPane.YES_OPTION) {
                        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM port_config WHERE id = ?")) {
                            ps.setInt(1, id);
                            ps.executeUpdate();
                            reloadConfigs();
                        } catch (SQLException ex) {
                            ex.printStackTrace();
                        }
                    }
                });

                configItem.add(selectItem);
                configItem.add(editItem);
                configItem.add(deleteItem);
                dropdownMenu.add(configItem);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private List<String> getConfigLinesById(int id) {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:bdd.db")) {
            PreparedStatement ps = conn.prepareStatement("SELECT * FROM port_config WHERE id = ?");
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return List.of(
                        "baudrate=" + rs.getInt("baudrate"),
                        "parity=" + rs.getString("parity"),
                        "databits=" + rs.getInt("databits"),
                        "stopbits=" + rs.getInt("stopbits")
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return List.of();
    }
}