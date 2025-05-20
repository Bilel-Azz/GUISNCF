package org.example.demo.ui.components;

import com.fazecast.jSerialComm.SerialPort;
import org.example.demo.serial.SerialTransmitter;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.List;
import java.util.function.Consumer;

public class PortConfigSelectorPanel extends JPanel {
    // Couleurs cohérentes avec le reste de l'application
    private static final Color BACKGROUND_COLOR = new Color(245, 245, 245);
    private static final Color ACCENT_COLOR = new Color(78, 110, 142);
    private static final Color SEND_BUTTON_COLOR = new Color(50, 120, 180);
    private static final Color LISTEN_BUTTON_COLOR = new Color(50, 140, 50);
    private static final Color LISTEN_ACTIVE_COLOR = new Color(180, 60, 60);

    // Polices
    private static final Font BUTTON_FONT = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font MENU_FONT = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font CONFIG_FONT = new Font("Segoe UI", Font.PLAIN, 13);

    // Composants UI
    private JButton configButton;
    private JButton sendButton;
    private JButton listenButton;
    private JPopupMenu configMenu;
    private JComboBox<PortItem> portSelector;

    // État
    private int selectedConfigId = -1;
    private String selectedConfigLabel = "";
    private boolean listening = false;
    private Thread listenThread;

    // Callback
    private final Consumer<String> onTrameReceived;
    private final Runnable onSendInit;

    public PortConfigSelectorPanel(Runnable onSendInit, Consumer<String> onTrameReceived) {
        this.onTrameReceived = onTrameReceived;
        this.onSendInit = onSendInit;

        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        setBackground(BACKGROUND_COLOR);
        setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        // Bouton de configuration avec style amélioré
        configButton = createConfigButton();

        // Sélecteur de port avec style amélioré
        portSelector = createPortSelector();

        // Bouton d'envoi avec style amélioré
        sendButton = createSendButton();

        // Bouton d'écoute avec style amélioré
        listenButton = createListenButton();

        // Menu déroulant pour les configurations
        configMenu = createConfigMenu();

        // Ajouter les composants au panel
        add(configButton);
        add(Box.createRigidArea(new Dimension(8, 0)));
        add(portSelector);
        add(Box.createRigidArea(new Dimension(8, 0)));
        add(sendButton);
        add(Box.createRigidArea(new Dimension(8, 0)));
        add(listenButton);

        // Charger les configurations
        reloadConfigs();

        // Rafraîchir les ports série
        refreshSerialPorts();
    }

    /**
     * Crée le bouton de configuration
     */
    private JButton createConfigButton() {
        JButton button = new JButton("Configuration Port ▼");
        button.setFont(BUTTON_FONT);
        button.setFocusPainted(false);
        button.setBackground(BACKGROUND_COLOR);
        button.setForeground(ACCENT_COLOR);
        button.setBorder(BorderFactory.createCompoundBorder(
                new RoundRectBorder(6, new Color(220, 220, 220)),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)
        ));

        // Ajouter des effets de survol
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(new Color(235, 235, 235));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(BACKGROUND_COLOR);
            }
        });

        // Afficher le menu lors du clic
        button.addActionListener(e -> configMenu.show(button, 0, button.getHeight()));

        return button;
    }

    /**
     * Crée le sélecteur de port
     */
    private JComboBox<PortItem> createPortSelector() {
        JComboBox<PortItem> selector = new JComboBox<>();
        selector.setFont(CONFIG_FONT);
        selector.setBackground(Color.WHITE);
        selector.setPreferredSize(new Dimension(150, 32));
        selector.setMaximumSize(new Dimension(150, 32));
        selector.setBorder(BorderFactory.createCompoundBorder(
                new RoundRectBorder(6, new Color(220, 220, 220)),
                BorderFactory.createEmptyBorder(0, 5, 0, 5)
        ));

        // Ajouter un bouton de rafraîchissement
        JButton refreshButton = new JButton("⟳");
        refreshButton.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        refreshButton.setFocusPainted(false);
        refreshButton.setBorderPainted(false);
        refreshButton.setContentAreaFilled(false);
        refreshButton.setToolTipText("Rafraîchir la liste des ports");
        refreshButton.addActionListener(e -> refreshSerialPorts());

        // Ajouter le bouton de rafraîchissement au sélecteur
        selector.setRenderer(new PortItemRenderer());

        // Ajouter un panel pour contenir le sélecteur et le bouton
        JPanel selectorPanel = new JPanel(new BorderLayout());
        selectorPanel.add(selector, BorderLayout.CENTER);
        selectorPanel.add(refreshButton, BorderLayout.EAST);

        return selector;
    }

    /**
     * Crée le bouton d'envoi
     */
    private JButton createSendButton() {
        JButton button = new JButton("Envoyer");
        button.setFont(BUTTON_FONT);
        button.setFocusPainted(false);
        button.setBackground(SEND_BUTTON_COLOR);
        button.setBorder(BorderFactory.createCompoundBorder(
                new RoundRectBorder(6, SEND_BUTTON_COLOR),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)
        ));

        // Ajouter des effets de survol
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (button.isEnabled()) {
                    button.setBackground(SEND_BUTTON_COLOR.darker());
                    button.setBorder(BorderFactory.createCompoundBorder(
                            new RoundRectBorder(6, SEND_BUTTON_COLOR.darker()),
                            BorderFactory.createEmptyBorder(6, 12, 6, 12)
                    ));
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (button.isEnabled()) {
                    button.setBackground(SEND_BUTTON_COLOR);
                    button.setBorder(BorderFactory.createCompoundBorder(
                            new RoundRectBorder(6, SEND_BUTTON_COLOR),
                            BorderFactory.createEmptyBorder(6, 12, 6, 12)
                    ));
                }
            }
        });

        // Ajouter l'action d'envoi
        button.addActionListener(e -> sendConfiguration());
        button.setToolTipText("Envoyer la configuration au port sélectionné");

        return button;
    }

    /**
     * Crée le bouton d'écoute
     */
    private JButton createListenButton() {
        JButton button = new JButton("▶ Écouter");
        button.setFont(BUTTON_FONT);
        button.setFocusPainted(false);
        button.setBackground(LISTEN_BUTTON_COLOR);
        button.setBorder(BorderFactory.createCompoundBorder(
                new RoundRectBorder(6, LISTEN_BUTTON_COLOR),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)
        ));

        // Ajouter des effets de survol
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (button.isEnabled()) {
                    Color currentColor = listening ? LISTEN_ACTIVE_COLOR : LISTEN_BUTTON_COLOR;
                    button.setBackground(currentColor.darker());
                    button.setBorder(BorderFactory.createCompoundBorder(
                            new RoundRectBorder(6, currentColor.darker()),
                            BorderFactory.createEmptyBorder(6, 12, 6, 12)
                    ));
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (button.isEnabled()) {
                    Color currentColor = listening ? LISTEN_ACTIVE_COLOR : LISTEN_BUTTON_COLOR;
                    button.setBackground(currentColor);
                    button.setBorder(BorderFactory.createCompoundBorder(
                            new RoundRectBorder(6, currentColor),
                            BorderFactory.createEmptyBorder(6, 12, 6, 12)
                    ));
                }
            }
        });

        // Ajouter l'action d'écoute
        button.addActionListener(e -> toggleListening());
        button.setToolTipText("Démarrer/arrêter l'écoute sur le port sélectionné");

        return button;
    }

    /**
     * Crée le menu de configuration
     */
    private JPopupMenu createConfigMenu() {
        JPopupMenu menu = new JPopupMenu();
        menu.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(220, 220, 220), 1),
                new EmptyBorder(5, 0, 5, 0)
        ));

        return menu;
    }

    /**
     * Envoie la configuration au port sélectionné
     */
    private void sendConfiguration() {
        if (selectedConfigId == -1) {
            showError("Veuillez d'abord sélectionner une configuration.");
            return;
        }

        PortItem selectedPort = (PortItem) portSelector.getSelectedItem();
        if (selectedPort == null) {
            showError("Aucun port série sélectionné.");
            return;
        }

        List<String> configLines = getConfigLinesById(selectedConfigId);

        if (SerialTransmitter.isSimulationMode()) {
            showInfo("Configuration simulée prête. Appuyez sur ▶ pour démarrer.");
        } else {
            try {
                SerialTransmitter.sendConfigOnly(selectedPort.getPortName(), 115200, configLines);
                showSuccess("Configuration envoyée avec succès.");
                if (onSendInit != null) {
                    onSendInit.run();
                }
            } catch (Exception ex) {
                showError("Erreur lors de l'envoi de la configuration: " + ex.getMessage());
            }
        }
    }

    /**
     * Démarre ou arrête l'écoute sur le port sélectionné
     */
    private void toggleListening() {
        if (listening) {
            SerialTransmitter.stopListening();
            listening = false;
            listenButton.setText("▶ Écouter");
            listenButton.setBackground(LISTEN_BUTTON_COLOR);
            listenButton.setBorder(BorderFactory.createCompoundBorder(
                    new RoundRectBorder(6, LISTEN_BUTTON_COLOR),
                    BorderFactory.createEmptyBorder(6, 12, 6, 12)
            ));
            // Réactiver le bouton d'envoi
            sendButton.setEnabled(true);
            showInfo("Écoute arrêtée.");
        } else {
            if (selectedConfigId == -1) {
                showError("Veuillez d'abord sélectionner une configuration.");
                return;
            }

            PortItem selectedPort = (PortItem) portSelector.getSelectedItem();
            if (selectedPort == null) {
                showError("Aucun port série sélectionné.");
                return;
            }

            List<String> configLines = getConfigLinesById(selectedConfigId);
            listening = true;
            listenButton.setText("⏹ Arrêter");
            listenButton.setBackground(LISTEN_ACTIVE_COLOR);
            listenButton.setBorder(BorderFactory.createCompoundBorder(
                    new RoundRectBorder(6, LISTEN_ACTIVE_COLOR),
                    BorderFactory.createEmptyBorder(6, 12, 6, 12)
            ));
            // Désactiver le bouton d'envoi
            sendButton.setEnabled(false);

            listenThread = new Thread(() -> {
                try {
                    SerialTransmitter.sendConfigAndListen(
                            selectedPort.getPortName(), 115200, configLines, onTrameReceived, false
                    );
                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> {
                        showError("Erreur lors de l'écoute: " + ex.getMessage());
                        toggleListening(); // Arrêter l'écoute en cas d'erreur
                    });
                }
            });
            listenThread.start();
            showSuccess("Écoute démarrée sur " + selectedPort.getPortName());
        }
    }

    /**
     * Rafraîchit la liste des ports série disponibles
     */
    private void refreshSerialPorts() {
        portSelector.removeAllItems();

        SerialPort[] ports = SerialPort.getCommPorts();
        if (ports.length == 0) {
            portSelector.addItem(new PortItem("Aucun port", ""));
            portSelector.setEnabled(false);
            sendButton.setEnabled(false);
            listenButton.setEnabled(false);
        } else {
            portSelector.setEnabled(true);
            sendButton.setEnabled(true);
            listenButton.setEnabled(true);

            for (SerialPort port : ports) {
                String name = port.getSystemPortName();
                String description = port.getDescriptivePortName();
                portSelector.addItem(new PortItem(name, description));
            }
        }
    }

    /**
     * Recharge les configurations depuis la base de données
     */
    public void reloadConfigs() {
        configMenu.removeAll();

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:bdd.db")) {
            String query = "SELECT * FROM port_config ORDER BY id";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(query);

            boolean hasConfigs = false;

            while (rs.next()) {
                hasConfigs = true;
                int id = rs.getInt("id");
                int baudrate = rs.getInt("baudrate");
                String parity = rs.getString("parity");
                int databits = rs.getInt("databits");
                int stopbits = rs.getInt("stopbits");

                String label = String.format("Config %d : %d - %s - %d/%d",
                        id, baudrate, parity, databits, stopbits);

                JPanel configPanel = new JPanel();
                configPanel.setLayout(new BorderLayout());
                configPanel.setBackground(Color.WHITE);

                JLabel configLabel = new JLabel(label);
                configLabel.setFont(CONFIG_FONT);
                configLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
                configPanel.add(configLabel, BorderLayout.CENTER);

                JPanel buttonsPanel = new JPanel();
                buttonsPanel.setLayout(new FlowLayout(FlowLayout.RIGHT, 5, 0));
                buttonsPanel.setBackground(Color.WHITE);

                // Bouton de sélection
                JButton selectButton = createMenuButton("\u2713", "Sélectionner cette configuration");
                selectButton.addActionListener(e -> {
                    selectedConfigId = id;
                    selectedConfigLabel = label;
                    configButton.setText(String.format("Config %d ▼", id));
                    configMenu.setVisible(false);
                });

                // Bouton d'édition
                JButton editButton = createMenuButton("\u270E", "Modifier cette configuration");
                editButton.addActionListener(e -> {
                    JFrame parent = (JFrame) SwingUtilities.getWindowAncestor(this);
                    new org.example.demo.ui.dialogs.EditPortConfigDialog(parent, id).setVisible(true);
                    reloadConfigs();
                    configMenu.setVisible(false);
                });

                // Bouton de suppression
                JButton deleteButton = createMenuButton("\u1F5D1", "Supprimer cette configuration");
                deleteButton.addActionListener(e -> {
                    int confirm = JOptionPane.showConfirmDialog(this,
                            "Supprimer cette configuration ?", "Confirmation",
                            JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

                    if (confirm == JOptionPane.YES_OPTION) {
                        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM port_config WHERE id = ?")) {
                            ps.setInt(1, id);
                            ps.executeUpdate();

                            // Réinitialiser la sélection si la configuration supprimée était sélectionnée
                            if (selectedConfigId == id) {
                                selectedConfigId = -1;
                                selectedConfigLabel = "";
                                configButton.setText("Configuration Port ▼");
                            }

                            reloadConfigs();
                        } catch (SQLException ex) {
                            showError("Erreur lors de la suppression: " + ex.getMessage());
                        }
                    }
                    configMenu.setVisible(false);
                });

                buttonsPanel.add(selectButton);
                buttonsPanel.add(editButton);
                buttonsPanel.add(deleteButton);
                configPanel.add(buttonsPanel, BorderLayout.EAST);

                // Ajouter un séparateur pour chaque élément sauf le dernier
                configMenu.add(configPanel);
                configMenu.add(new JSeparator());
            }

            // Ajouter un élément pour créer une nouvelle configuration
            JPanel newConfigPanel = new JPanel(new BorderLayout());
            newConfigPanel.setBackground(Color.WHITE);

            JLabel newConfigLabel = new JLabel("Ajouter une nouvelle configuration");
            newConfigLabel.setFont(CONFIG_FONT);
            newConfigLabel.setForeground(ACCENT_COLOR);
            newConfigLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

            newConfigPanel.add(newConfigLabel, BorderLayout.CENTER);
            newConfigPanel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    newConfigPanel.setBackground(new Color(240, 240, 240));
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    newConfigPanel.setBackground(Color.WHITE);
                }

                @Override
                public void mouseClicked(MouseEvent e) {
                    JFrame parent = (JFrame) SwingUtilities.getWindowAncestor(PortConfigSelectorPanel.this);
                    new org.example.demo.ui.dialogs.AddPortConfigDialog(parent).setVisible(true);
                    reloadConfigs();
                    configMenu.setVisible(false);
                }
            });

            configMenu.add(newConfigPanel);

            // Si aucune configuration n'est trouvée
            if (!hasConfigs) {
                JLabel noConfigLabel = new JLabel("Aucune configuration trouvée");
                noConfigLabel.setFont(CONFIG_FONT);
                noConfigLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
                configMenu.add(noConfigLabel, 0);
                configMenu.add(new JSeparator(), 1);
            }

        } catch (SQLException e) {
            showError("Erreur de base de données: " + e.getMessage());
        }
    }

    /**
     * Récupère les lignes de configuration par ID
     */
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
            showError("Erreur lors de la récupération de la configuration: " + e.getMessage());
        }

        return List.of();
    }

    /**
     * Crée un bouton pour le menu
     */
    private JButton createMenuButton(String text, String tooltip) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 14));
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setToolTipText(tooltip);
        button.setPreferredSize(new Dimension(30, 25));
        button.setMargin(new Insets(0, 0, 0, 0));

        return button;
    }

    /**
     * Affiche un message d'erreur
     */
    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Erreur", JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Affiche un message d'information
     */
    private void showInfo(String message) {
        JOptionPane.showMessageDialog(this, message, "Information", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Affiche un message de succès
     */
    private void showSuccess(String message) {
        JOptionPane.showMessageDialog(this, message, "Succès", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Classe pour représenter un élément de port dans le sélecteur
     */
    private static class PortItem {
        private final String portName;
        private final String description;

        public PortItem(String portName, String description) {
            this.portName = portName;
            this.description = description;
        }

        public String getPortName() {
            return portName;
        }

        public String getDescription() {
            return description;
        }

        @Override
        public String toString() {
            return portName;
        }
    }

    /**
     * Renderer personnalisé pour les éléments de port
     */
    private static class PortItemRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value,
                                                      int index, boolean isSelected, boolean cellHasFocus) {

            JLabel label = (JLabel) super.getListCellRendererComponent(
                    list, value, index, isSelected, cellHasFocus);

            if (value instanceof PortItem) {
                PortItem item = (PortItem) value;
                if (!item.getDescription().isEmpty()) {
                    label.setToolTipText(item.getDescription());
                }
            }

            return label;
        }
    }
}
