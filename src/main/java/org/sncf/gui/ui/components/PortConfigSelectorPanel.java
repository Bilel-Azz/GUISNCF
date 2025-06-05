package org.sncf.gui.ui.components;

import com.fazecast.jSerialComm.SerialPort;
import org.sncf.gui.serial.SerialTransmitter;
import org.sncf.gui.services.DatabaseManager;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.List;
import java.util.function.Consumer;
import java.net.URL;

/**
 * Panneau graphique Swing permettant à l’utilisateur de :
 * <ul>
 *     <li>Sélectionner un port série</li>
 *     <li>Choisir une configuration parmi celles stockées en base</li>
 *     <li>Envoyer cette configuration à un port série</li>
 *     <li>Démarrer ou arrêter l’écoute des trames série</li>
 * </ul>
 * Intègre des styles personnalisés et une interaction fluide avec la base de données et le composant SerialTransmitter.
 */
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

    /**
     * Construit le panneau de sélection de port et de configuration.
     *
     * @param onSendInit      Callback à exécuter après un envoi réussi de configuration.
     * @param onTrameReceived Callback appelé à chaque trame reçue lors de l'écoute série.
     */
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
     * Crée le bouton de menu de configuration avec style personnalisé.
     *
     * @return bouton configuré.
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
     * Crée le sélecteur de ports série avec rafraîchissement intégré.
     *
     * @return composant JComboBox personnalisé.
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
     * Crée le bouton d’envoi avec effets de survol.
     *
     * @return bouton configuré.
     */
    private JButton createSendButton() {
        JButton button = createIconTextButton("envelope-2.png","Envoyer", "Envoyer");
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
     * Crée le bouton d'écoute avec gestion de styles et état.
     *
     * @return bouton configuré.
     */
    private JButton createListenButton() {
        JButton button = createIconTextButton("bouton-droit.png","Écouter","Écouter");
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
     * Crée le menu déroulant pour la sélection et gestion des configurations.
     *
     * @return menu contextuel de configurations.
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
     * Envoie la configuration sélectionnée au port série.
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
     * Démarre ou arrête l’écoute des trames série selon l’état actuel.
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
     * Rafraîchit la liste des ports série disponibles et met à jour l'interface.
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
     * Recharge les configurations disponibles depuis la base de données et les affiche dans le menu déroulant.
     */
    public void reloadConfigs() {
        configMenu.removeAll();

        try (Connection conn = DriverManager.getConnection(DatabaseManager.getDbUrl())) {
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

                String label = String.format("Config %d : %d - %s - %d/%d", id, baudrate, parity, databits, stopbits);

                // Panel global de la ligne
                JPanel configRow = new JPanel(new BorderLayout());
                configRow.setBackground(Color.WHITE);
                configRow.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));

                // Label configuration
                JLabel labelComponent = new JLabel(label);
                labelComponent.setFont(CONFIG_FONT);
                configRow.add(labelComponent, BorderLayout.CENTER);

                // Panneau d'actions
                JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 0));
                actionsPanel.setOpaque(false);

                JButton selectBtn = createIconButton("valide.png", "Sélectionner cette configuration");
                selectBtn.addActionListener(e -> {
                    selectedConfigId = id;
                    selectedConfigLabel = label;
                    configButton.setText("Config " + id + " ▼");
                    configMenu.setVisible(false);
                });

                JButton editBtn   = createIconButton("crayon.png", "Modifier cette configuration");
                editBtn.addActionListener(e -> {
                    JFrame parent = (JFrame) SwingUtilities.getWindowAncestor(this);
                    new org.sncf.gui.ui.dialogs.EditPortConfigDialog(parent, id).setVisible(true);
                    reloadConfigs();
                    configMenu.setVisible(false);
                });

                JButton deleteBtn = createIconButton("poubelle.png", "Supprimer cette configuration");
                deleteBtn.addActionListener(e -> {
                    int confirm = JOptionPane.showConfirmDialog(this,
                            "Supprimer cette configuration ?", "Confirmation",
                            JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

                    if (confirm == JOptionPane.YES_OPTION) {
                        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM port_config WHERE id = ?")) {
                            ps.setInt(1, id);
                            ps.executeUpdate();
                            if (selectedConfigId == id) {
                                selectedConfigId = -1;
                                selectedConfigLabel = "";
                                configButton.setText("Configuration Port ▼");
                            }
                            reloadConfigs();
                        } catch (SQLException ex) {
                            showError("Erreur lors de la suppression : " + ex.getMessage());
                        }
                    }

                    configMenu.setVisible(false);
                });

                actionsPanel.add(selectBtn);
                actionsPanel.add(editBtn);
                actionsPanel.add(deleteBtn);

                configRow.add(actionsPanel, BorderLayout.EAST);

                // Effet hover
                configRow.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseEntered(MouseEvent e) {
                        configRow.setBackground(new Color(245, 245, 245));
                    }

                    @Override
                    public void mouseExited(MouseEvent e) {
                        configRow.setBackground(Color.WHITE);
                    }
                });

                configMenu.add(configRow);
                configMenu.add(new JSeparator());
            }

            // Ajout d’une configuration
            JPanel addConfigPanel = new JPanel(new BorderLayout());
            addConfigPanel.setBackground(Color.WHITE);
            addConfigPanel.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));

            JLabel addLabel = new JLabel("+ Nouvelle configuration");
            addLabel.setFont(CONFIG_FONT);
            addLabel.setForeground(ACCENT_COLOR);
            addConfigPanel.add(addLabel, BorderLayout.CENTER);

            addConfigPanel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    addConfigPanel.setBackground(new Color(240, 240, 240));
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    addConfigPanel.setBackground(Color.WHITE);
                }

                @Override
                public void mouseClicked(MouseEvent e) {
                    JFrame parent = (JFrame) SwingUtilities.getWindowAncestor(PortConfigSelectorPanel.this);
                    new org.sncf.gui.ui.dialogs.AddPortConfigDialog(parent).setVisible(true);
                    reloadConfigs();
                    configMenu.setVisible(false);
                }
            });

            configMenu.add(addConfigPanel);

            if (!hasConfigs) {
                JLabel noConfigLabel = new JLabel("Aucune configuration enregistrée");
                noConfigLabel.setFont(CONFIG_FONT);
                noConfigLabel.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
                configMenu.add(noConfigLabel, 0);
                configMenu.add(new JSeparator(), 1);
            }

        } catch (SQLException e) {
            showError("Erreur de base de données : " + e.getMessage());
        }
    }

    /**
     * Crée un bouton avec uniquement une icône, sans texte.
     * <p>
     * L’icône est chargée depuis le dossier <code>/icons/</code> du classpath, redimensionnée à 16×16 pixels,
     * et utilisée comme image principale du bouton. Le style du bouton est plat, sans fond ni bordures visibles,
     * adapté aux barres d’outils ou menus contextuels minimalistes.
     *
     * @param iconFileName le nom du fichier image dans <code>/resources/icons</code> (ex : <code>"poubelle.png"</code>)
     * @param tooltip      le texte de l’infobulle (affiché au survol)
     * @return un bouton Swing affichant une icône seule
     * @throws IllegalArgumentException si l’icône n’est pas trouvée dans le classpath
     */
    private JButton createIconButton(String iconFileName, String tooltip) {
        URL iconUrl = getClass().getResource("/icons/" + iconFileName);
        if (iconUrl == null) {
            throw new IllegalArgumentException("Icon not found: " + iconFileName);
        }

        ImageIcon icon = new ImageIcon(iconUrl);
        Image scaled = icon.getImage().getScaledInstance(16, 16, Image.SCALE_SMOOTH);
        JButton button = new JButton(new ImageIcon(scaled));
        button.setPreferredSize(new Dimension(26, 26));
        button.setFocusPainted(false);
        button.setContentAreaFilled(false);
        button.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        button.setToolTipText(tooltip);
        return button;
    }

    /**
     * Crée un bouton avec une icône à gauche du texte.
     * <p>
     * L’icône est chargée depuis le dossier <code>/icons/</code> du classpath, redimensionnée à 16×16 pixels,
     * puis affichée à gauche du libellé. Ce bouton est stylisé avec une police lisible, fond blanc et alignement gauche.
     *
     * @param iconFileName le nom du fichier image dans <code>/resources/icons</code> (ex : <code>"export.png"</code>)
     * @param text         le libellé du bouton (ex : <code>"Exporter"</code>)
     * @param tooltip      le texte de l’infobulle (affiché au survol)
     * @return un bouton Swing avec icône + texte
     * @throws IllegalArgumentException si l’icône n’est pas trouvée dans le classpath
     */
    private JButton createIconTextButton(String iconFileName, String text, String tooltip) {
        URL iconUrl = getClass().getResource("/icons/" + iconFileName);
        if (iconUrl == null) {
            throw new IllegalArgumentException("Icon not found: " + iconFileName);
        }

        ImageIcon icon = new ImageIcon(iconUrl);
        Image scaled = icon.getImage().getScaledInstance(16, 16, Image.SCALE_SMOOTH);
        ImageIcon scaledIcon = new ImageIcon(scaled);

        JButton button = new JButton(text, scaledIcon);
        button.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        button.setFocusPainted(false);
        button.setBackground(Color.WHITE);
        button.setToolTipText(tooltip);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        return button;
    }


    /**
     * Récupère les lignes de configuration (baudrate, parité, etc.) en fonction de l’ID sélectionné.
     *
     * @param id identifiant de configuration.
     * @return liste de lignes de configuration pour l’ESP32.
     */
    private List<String> getConfigLinesById(int id) {
        try (Connection conn = DriverManager.getConnection(DatabaseManager.getDbUrl())) {
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
     * Crée un bouton pour une action dans le menu contextuel de configuration.
     *
     * @param text    symbole du bouton (ex : ✔, ✎).
     * @param tooltip info-bulle du bouton.
     * @return bouton de menu configuré.
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
     * Affiche une boîte de dialogue d'erreur.
     *
     * @param message message à afficher.
     */
    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Erreur", JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Affiche une boîte de dialogue d'information.
     *
     * @param message message à afficher.
     */
    private void showInfo(String message) {
        JOptionPane.showMessageDialog(this, message, "Information", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Affiche une boîte de dialogue de succès.
     *
     * @param message message à afficher.
     */
    private void showSuccess(String message) {
        JOptionPane.showMessageDialog(this, message, "Succès", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Représente un port série détecté avec son nom système et sa description.
     */
    private static class PortItem {
        private final String portName;
        private final String description;

        /**
         * @param portName     nom système du port (ex: COM3, /dev/ttyUSB0).
         * @param description  description fournie par le système.
         */
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
     * Renderer personnalisé pour l’affichage des ports série dans le JComboBox.
     * Affiche un tooltip avec la description du port si disponible.
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