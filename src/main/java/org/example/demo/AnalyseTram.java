package org.example.demo;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.border.*;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

public class AnalyseTram extends JFrame {
    private JTextArea hexArea;
    private JPanel rightPanel;
    private CardLayout cardLayout;
    private JPanel messageView;
    private JPanel graphView;
    private Map<String, Color> alertColors;

    public AnalyseTram() {
        initializeColors();
        setupMainFrame();
        createToolbar();
        createMainContent();
    }

    private void initializeColors() {
        alertColors = new HashMap<>();
        alertColors.put("emergency", new Color(255, 102, 102));    // Red for emergency
        alertColors.put("warning", new Color(255, 255, 153));      // Yellow for non-emergency
        alertColors.put("control", new Color(153, 255, 153));      // Green for control requests
    }

    private void setupMainFrame() {
        setTitle("ANALYSETRAM©");
        setSize(1200, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBackground(Color.WHITE);
    }

    private void createToolbar() {
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        toolbar.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        toolbar.setBackground(Color.WHITE);

        // Logo
        JLabel logo = new JLabel("∞ - ANALYSETRAM©");
        logo.setFont(new Font("Arial", Font.BOLD, 14));
        toolbar.add(logo);

        // Boutons de vue avec gestion des événements
        JButton graphiqueBtn = createToolbarButton("Graphique", "📈");
        JButton messageBtn = createToolbarButton("Message", "💬");
        JButton filtreBtn = createToolbarButton("Filtre", "🔍");

        graphiqueBtn.addActionListener(e -> showGraphView());
        messageBtn.addActionListener(e -> showMessageView());

        toolbar.add(graphiqueBtn);
        toolbar.add(messageBtn);
        toolbar.add(filtreBtn);

        // Boutons de droite
        JPanel rightButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        rightButtons.setBackground(Color.WHITE);
        rightButtons.add(createToolbarButton("Protocol", "➕"));
        rightButtons.add(createRS232Dropdown());

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(Color.WHITE);
        topPanel.add(toolbar, BorderLayout.WEST);
        topPanel.add(rightButtons, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);
    }

    private JButton createToolbarButton(String text, String icon) {
        JButton button = new JButton(text + " " + icon);
        button.setFocusPainted(false);
        button.setBackground(Color.WHITE);
        button.setBorder(BorderFactory.createCompoundBorder(
                new RoundRectBorder(10),
                BorderFactory.createEmptyBorder(5, 15, 5, 15)
        ));
        return button;
    }

    private JPanel createRS232Dropdown() {
        JPanel rs232Panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        rs232Panel.setBackground(Color.WHITE);

        // Créer le bouton principal RS232
        JButton rs232Button = new JButton("RS232 ⬇️");
        rs232Button.setFocusPainted(false);
        rs232Button.setBackground(Color.WHITE);
        rs232Button.setBorder(BorderFactory.createCompoundBorder(
                new RoundRectBorder(10),
                BorderFactory.createEmptyBorder(5, 15, 5, 15)
        ));

        // Créer le menu popup
        JPopupMenu dropdownMenu = new JPopupMenu();
        dropdownMenu.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));

        // Ajouter les options du menu
        JMenuItem option1 = new JMenuItem("COM1");
        JMenuItem option2 = new JMenuItem("COM2");
        JMenuItem option3 = new JMenuItem("COM3");

        // Styliser les options du menu
        Font menuFont = new Font("SansSerif", Font.PLAIN, 12);
        option1.setFont(menuFont);
        option2.setFont(menuFont);
        option3.setFont(menuFont);

        // Ajouter les actions pour chaque option
        option1.addActionListener(e -> System.out.println("COM1 sélectionné"));
        option2.addActionListener(e -> System.out.println("COM2 sélectionné"));
        option3.addActionListener(e -> System.out.println("COM3 sélectionné"));

        // Ajouter les options au menu
        dropdownMenu.add(option1);
        dropdownMenu.add(option2);
        dropdownMenu.add(option3);

        // Ajouter l'action pour afficher le menu lors du clic
        rs232Button.addActionListener(e -> {
            dropdownMenu.show(rs232Button, 0, rs232Button.getHeight());
        });

        rs232Panel.add(rs232Button);
        return rs232Panel;
    }

    private void createMainContent() {
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);

        // Panel gauche (Hex codes)
        hexArea = new JTextArea();
        hexArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        hexArea.setText(getInitialHexCodes());
        hexArea.setEditable(false);

        // Panel droit avec CardLayout pour switcher entre les vues
        rightPanel = new JPanel();
        cardLayout = new CardLayout();
        rightPanel.setLayout(cardLayout);

        // Création des deux vues
        createMessageView();
        createGraphView();

        // Ajout des vues au panel droit
        rightPanel.add(messageView, "MESSAGE");
        rightPanel.add(graphView, "GRAPH");

        splitPane.setLeftComponent(new JScrollPane(hexArea));
        splitPane.setRightComponent(rightPanel);
        splitPane.setDividerLocation(400);

        add(splitPane, BorderLayout.CENTER);
    }

    private void createMessageView() {
        messageView = new JPanel(new BorderLayout());
        JTextArea infoArea = new JTextArea();
        infoArea.setFont(new Font("SansSerif", Font.PLAIN, 12));
        updateTrainInfo(infoArea);
        infoArea.setEditable(false);
        messageView.add(new JScrollPane(infoArea), BorderLayout.CENTER);
    }

    private void createGraphView() {
        graphView = new JPanel(new BorderLayout());

        // Création du graphique personnalisé
        SignalGraphPanel graphPanel = new SignalGraphPanel();
        graphView.add(graphPanel, BorderLayout.CENTER);

        // Ajout des filtres
        JPanel filterPanel = new JPanel();
        filterPanel.setLayout(new BoxLayout(filterPanel, BoxLayout.Y_AXIS));

        // Bouton Ajouter un filtre
        JPanel addFilterPanel = new JPanel(new BorderLayout());
        JButton addFilterBtn = new JButton("Ajouter un filtre ➕");
        addFilterBtn.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        addFilterPanel.add(addFilterBtn, BorderLayout.NORTH);

        // Filtres existants
        JPanel filtersPanel = new JPanel();
        filtersPanel.setLayout(new BoxLayout(filtersPanel, BoxLayout.Y_AXIS));
        addFilter(filtersPanel, "Filtre 1", true);
        addFilter(filtersPanel, "Filtre 2", false);

        filterPanel.add(addFilterPanel);
        filterPanel.add(filtersPanel);
        graphView.add(filterPanel, BorderLayout.SOUTH);
    }

    // Classe interne pour dessiner le graphique
    private class SignalGraphPanel extends JPanel {
        private List<Point> points;

        public SignalGraphPanel() {
            setBackground(Color.WHITE);
            initializePoints();
        }

        private void initializePoints() {
            points = new ArrayList<>();
            // Données d'exemple pour un signal digital
            points.add(new Point(0, 1));
            points.add(new Point(10, 1));
            points.add(new Point(10, 0));
            points.add(new Point(15, 0));
            points.add(new Point(15, 1));
            points.add(new Point(20, 1));
            points.add(new Point(20, 0));
            points.add(new Point(25, 0));
            points.add(new Point(25, 1));
            points.add(new Point(30, 1));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;

            // Améliorer la qualité du rendu
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int width = getWidth();
            int height = getHeight();
            int padding = 50;

            // Dessiner les axes
            g2d.setColor(Color.BLACK);
            g2d.drawLine(padding, height - padding, width - padding, height - padding); // Axe X
            g2d.drawLine(padding, padding, padding, height - padding); // Axe Y

            // Étiquettes des axes
            g2d.drawString("Time (ms)", width / 2, height - 10);
            g2d.drawString("Bit", 10, height / 2);

            // Dessiner les graduations sur l'axe X
            for (int i = 0; i <= 30; i += 5) {
                int x = padding + (i * (width - 2 * padding) / 30);
                g2d.drawLine(x, height - padding - 5, x, height - padding + 5);
                g2d.drawString(String.valueOf(i), x - 5, height - padding + 20);
            }

            // Dessiner les graduations sur l'axe Y
            for (int i = 0; i <= 1; i++) {
                int y = height - padding - (i * (height - 2 * padding) / 1);
                g2d.drawLine(padding - 5, y, padding + 5, y);
                g2d.drawString(String.valueOf(i), padding - 15, y + 5);
            }

            // Dessiner le signal
            g2d.setColor(Color.BLUE);
            g2d.setStroke(new BasicStroke(2));

            for (int i = 0; i < points.size() - 1; i++) {
                Point p1 = points.get(i);
                Point p2 = points.get(i + 1);

                int x1 = padding + (p1.x * (width - 2 * padding) / 30);
                int y1 = height - padding - (p1.y * (height - 2 * padding) / 1);
                int x2 = padding + (p2.x * (width - 2 * padding) / 30);
                int y2 = height - padding - (p2.y * (height - 2 * padding) / 1);

                g2d.drawLine(x1, y1, x2, y2);
            }
        }
    }

    private void addFilter(JPanel panel, String name, boolean checked) {
        JPanel filterRow = new JPanel(new BorderLayout());
        JCheckBox checkBox = new JCheckBox(name, checked);
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton settingsBtn = new JButton("⚙️");
        JButton deleteBtn = new JButton("✖");

        settingsBtn.setBorderPainted(false);
        deleteBtn.setBorderPainted(false);
        settingsBtn.setContentAreaFilled(false);
        deleteBtn.setContentAreaFilled(false);

        buttonPanel.add(settingsBtn);
        buttonPanel.add(deleteBtn);

        filterRow.add(checkBox, BorderLayout.WEST);
        filterRow.add(buttonPanel, BorderLayout.EAST);

        panel.add(filterRow);
    }

    private void showGraphView() {
        cardLayout.show(rightPanel, "GRAPH");
    }

    private void showMessageView() {
        cardLayout.show(rightPanel, "MESSAGE");
    }

    private String getInitialHexCodes() {
        return "7E 02 1A 01 0B 2D 5F 10 15 7E\n" +
                "7E 05 1A 00 14 FF 7E\n" +
                "7E 08 1A 01 04 01 00 25 7E\n" +
                "7E 10 1A 03 00 02 7E\n" +
                "7E 02 1B 01 0C 2E 60 12 18 7E\n" +
                "7E 05 1B 01 10 00 7E\n" +
                "7E 08 1B 00 03 03 01 2F 7E\n" +
                "7E 10 1B 03 00 03 7E\n" +
                "7E 02 1C 01 0D 2F 61 13 20 7E\n" +
                "7E 05 1C 00 12 FF 7E\n" +
                "7E 08 1C 01 04 02 00 20 7E\n" +
                "7E 10 1C 03 00 01 7E";
    }

    private void updateTrainInfo(JTextArea infoArea) {
        StringBuilder info = new StringBuilder();
        addTrainInfo(info, "1A", "0B 2D", "5F", 10, true, 20, "Green", 1, 0, 25);
        addTrainInfo(info, "1B", "0C 2E", "60", 12, false, 16, "Red", 3, 1, 47);
        addTrainInfo(info, "1C", "0D 2F", "61", 13, true, 18, "Green", 2, 0, 32);
        addTrainInfo(info, "1D", "0A 30", "62", 15, false, 20, "Red", 1, 0, 15);
        addTrainInfo(info, "1E", "0E 31", "63", 18, true, 22, "Green", 3, 0, 10);
        addTrainInfo(info, "1F", "0F 32", "64", 20, false, 24, "Green", 1, 0, 12);
        infoArea.setText(info.toString());
    }

    private void addTrainInfo(StringBuilder info, String trainId, String lat, String lon,
                              int speed, boolean emergency, int speedLimit, String signal,
                              int platforms, int occupied, int delay) {
        info.append(String.format("[Train ID: %s] Status: In service | Position: Latitude %s, Longitude %s | Speed: %d km/h\n",
                trainId, lat, lon, speed));
        info.append(String.format("[Alert] Speed limit set to %d km/h%s\n",
                speedLimit, emergency ? " due to emergency" : ", no emergency"));
        info.append(String.format("[Station Status] Signal: %s | Available Platforms: %d, Occupied: %d | Delay: %d seconds\n",
                signal, platforms, occupied, delay));
        info.append("[Control Request] Speed report requested\n\n");
    }

    // Custom round rectangle border for buttons
    private static class RoundRectBorder extends AbstractBorder {
        private int radius;

        RoundRectBorder(int radius) {
            this.radius = radius;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D)g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Color.LIGHT_GRAY);
            g2.drawRoundRect(x, y, width-1, height-1, radius, radius);
            g2.dispose();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            AnalyseTram app = new AnalyseTram();
            app.setVisible(true);
        });
    }
}