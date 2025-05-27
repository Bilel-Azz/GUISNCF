package org.example.demo.ui.views;

import org.example.demo.model.FilterRule;
import org.example.demo.ui.components.RoundRectBorder;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class FilterView extends JPanel {
    // Couleurs cohérentes avec le reste de l'application
    private static final Color BACKGROUND_COLOR = new Color(245, 245, 245);
    private static final Color ACCENT_COLOR = new Color(78, 110, 142);
    private static final Color CARD_BACKGROUND = Color.WHITE;
    private static final Color BORDER_COLOR = new Color(220, 220, 220);
    private static final Color HOVER_COLOR = new Color(240, 240, 240);

    // Polices
    private static final Font TITLE_FONT = new Font("Segoe UI", Font.BOLD, 18);
    private static final Font FILTER_FONT = new Font("Segoe UI", Font.PLAIN, 14);
    private static final Font BUTTON_FONT = new Font("Segoe UI", Font.PLAIN, 13);

    // Composants UI
    private final JPanel filterListPanel;
    private final JButton addFilterButton =new JButton("+ Nouveau filtre");
    private final JLabel emptyStateLabel;

    // Callback
    private Consumer<List<FilterRule>> onFiltersUpdated;

    public void setOnFiltersUpdated(Consumer<List<FilterRule>> consumer) {
        this.onFiltersUpdated = consumer;
    }

    public FilterView() {
        setLayout(new BorderLayout(0, 15));
        setBackground(BACKGROUND_COLOR);
        setBorder(new EmptyBorder(20, 20, 20, 20));

        // En-tête avec titre et bouton d'ajout
        JPanel headerPanel = createHeaderPanel();

        // Panneau de recherche
        JPanel searchPanel = createSearchPanel();

        // Panneau pour contenir l'en-tête et la recherche
        JPanel topPanel = new JPanel(new BorderLayout(0, 10));
        topPanel.setBackground(BACKGROUND_COLOR);
        topPanel.add(headerPanel, BorderLayout.NORTH);
        topPanel.add(searchPanel, BorderLayout.CENTER);

        // Panneau principal pour la liste des filtres
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(BACKGROUND_COLOR);

        // Panneau pour la liste des filtres
        filterListPanel = new JPanel();
        filterListPanel.setLayout(new BoxLayout(filterListPanel, BoxLayout.Y_AXIS));
        filterListPanel.setBackground(CARD_BACKGROUND);
        filterListPanel.setBorder(new EmptyBorder(5, 0, 5, 0));

        // Message d'état vide
        emptyStateLabel = new JLabel("Aucun filtre défini. Cliquez sur 'Nouveau filtre' pour commencer.", JLabel.CENTER);
        emptyStateLabel.setFont(FILTER_FONT);
        emptyStateLabel.setForeground(new Color(120, 120, 120));
        emptyStateLabel.setBorder(new EmptyBorder(30, 10, 30, 10));

        // Scroll pane pour la liste des filtres
        JScrollPane scrollPane = new JScrollPane(filterListPanel);
        scrollPane.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDER_COLOR, 1),
                new EmptyBorder(5, 5, 5, 5)
        ));
        scrollPane.setBackground(CARD_BACKGROUND);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        // Ajouter les composants au panneau principal
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // Ajouter les panneaux à la vue principale
        add(topPanel, BorderLayout.NORTH);
        add(mainPanel, BorderLayout.CENTER);

        // Charger les filtres depuis la base de données
        loadFilters();

        // Ajouter un bouton d'aide en bas
        JPanel helpPanel = createHelpPanel();
        add(helpPanel, BorderLayout.SOUTH);
    }

    /**
     * Crée le panneau d'en-tête avec le titre et le bouton d'ajout
     */
    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(BACKGROUND_COLOR);
        headerPanel.setBorder(new EmptyBorder(0, 0, 10, 0));

        // Titre avec icône
        JLabel titleLabel = new JLabel("Gestion des filtres personnalisés");
        titleLabel.setFont(TITLE_FONT);
        titleLabel.setForeground(ACCENT_COLOR);
        titleLabel.setBorder(new EmptyBorder(0, 5, 0, 0));

        // Bouton d'ajout de filtre
        addFilterButton.setFont(BUTTON_FONT);
        addFilterButton.setFocusPainted(false);
        addFilterButton.setBackground(ACCENT_COLOR);
        addFilterButton.setForeground(Color.BLACK);
        addFilterButton.setBorder(BorderFactory.createCompoundBorder(
                new RoundRectBorder(6, ACCENT_COLOR),
                BorderFactory.createEmptyBorder(8, 15, 8, 15)
        ));

        // Ajouter des effets de survol
        addFilterButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                addFilterButton.setBackground(ACCENT_COLOR.darker());
                addFilterButton.setBorder(BorderFactory.createCompoundBorder(
                        new RoundRectBorder(6, ACCENT_COLOR.darker()),
                        BorderFactory.createEmptyBorder(8, 15, 8, 15)
                ));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                addFilterButton.setBackground(ACCENT_COLOR);
                addFilterButton.setBorder(BorderFactory.createCompoundBorder(
                        new RoundRectBorder(6, ACCENT_COLOR),
                        BorderFactory.createEmptyBorder(8, 15, 8, 15)
                ));
            }
        });

        // Ajouter l'action d'ajout de filtre
        addFilterButton.addActionListener(e -> newFilterDialog());

        headerPanel.add(titleLabel, BorderLayout.WEST);
        headerPanel.add(addFilterButton, BorderLayout.EAST);

        return headerPanel;
    }

    /**
     * Crée le panneau de recherche
     */
    private JPanel createSearchPanel() {
        JPanel searchPanel = new JPanel(new BorderLayout());
        searchPanel.setBackground(BACKGROUND_COLOR);
        searchPanel.setBorder(new EmptyBorder(0, 0, 15, 0));

        JTextField searchField = new JTextField();
        searchField.setFont(FILTER_FONT);
        searchField.setBorder(BorderFactory.createCompoundBorder(
                new RoundRectBorder(6, BORDER_COLOR),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        searchField.putClientProperty("JTextField.placeholderText", "Rechercher un filtre...");

        // Ajouter l'action de recherche
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                filterItems(searchField.getText());
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                filterItems(searchField.getText());
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                filterItems(searchField.getText());
            }
        });

        searchPanel.add(searchField, BorderLayout.CENTER);

        return searchPanel;
    }

    /**
     * Crée le panneau d'aide
     */
    private JPanel createHelpPanel() {
        JPanel helpPanel = new JPanel(new BorderLayout());
        helpPanel.setBackground(BACKGROUND_COLOR);
        helpPanel.setBorder(new EmptyBorder(15, 0, 0, 0));

        JPanel infoCard = new JPanel(new BorderLayout());
        infoCard.setBackground(new Color(240, 248, 255));
        infoCard.setBorder(BorderFactory.createCompoundBorder(
                new RoundRectBorder(8, new Color(200, 220, 240)),
                BorderFactory.createEmptyBorder(12, 15, 12, 15)
        ));

        JLabel infoLabel = new JLabel("<html><b>Astuce :</b> Les filtres vous permettent de mettre en évidence des motifs spécifiques dans les trames. " +
                "Utilisez des motifs hexadécimaux (ex: 7E) ou binaires (ex: 01101110) pour définir vos filtres.</html>");
        infoLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        infoLabel.setForeground(new Color(50, 80, 120));

        infoCard.add(infoLabel, BorderLayout.CENTER);
        helpPanel.add(infoCard, BorderLayout.CENTER);

        return helpPanel;
    }

    /**
     * Filtre les éléments de la liste en fonction du texte de recherche
     */
    private void filterItems(String searchText) {
        for (Component comp : filterListPanel.getComponents()) {
            if (comp instanceof JPanel panel) {
                Component[] children = panel.getComponents();
                if (children.length > 0 && children[0] instanceof JPanel leftPanel) {
                    Component[] leftChildren = leftPanel.getComponents();
                    if (leftChildren.length > 0 && leftChildren[0] instanceof JCheckBox) {
                        if (children.length > 1 && children[1] instanceof JPanel centerPanel) {
                            Component[] centerChildren = centerPanel.getComponents();
                            if (centerChildren.length > 0 && centerChildren[0] instanceof JLabel nameLabel) {
                                String filterText = nameLabel.getText().toLowerCase();
                                boolean visible = searchText.isEmpty() || filterText.contains(searchText.toLowerCase());
                                panel.setVisible(visible);
                            }
                        }
                    }
                }
            }
        }
        filterListPanel.revalidate();
        filterListPanel.repaint();
    }

    /**
     * Charge les filtres depuis la base de données
     */
    private void loadFilters() {
        filterListPanel.removeAll();

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:bdd.db")) {
            String query = "SELECT * FROM custom_filter ORDER BY name";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(query);

            boolean hasFilters = false;

            while (rs.next()) {
                hasFilters = true;
                int id = rs.getInt("id");
                String name = rs.getString("name");
                String pattern = rs.getString("pattern");
                String color = rs.getString("color");

                // Créer un panneau pour chaque filtre
                JPanel filterCard = createFilterCard(id, name, pattern, color);
                filterListPanel.add(filterCard);

                // Ajouter un séparateur
                JSeparator separator = new JSeparator(JSeparator.HORIZONTAL);
                separator.setForeground(BORDER_COLOR);
                separator.setBackground(CARD_BACKGROUND);
                filterListPanel.add(separator);
            }

            // Afficher un message si aucun filtre n'est trouvé
            if (!hasFilters) {
                filterListPanel.add(emptyStateLabel);
            }

            filterListPanel.revalidate();
            filterListPanel.repaint();

            triggerUpdate();

        } catch (SQLException e) {
            showError("Erreur lors du chargement des filtres: " + e.getMessage());
        }
    }

    /**
     * Crée une carte pour un filtre
     */
    private JPanel createFilterCard(int id, String name, String pattern, String color) {
        JPanel card = new JPanel(new BorderLayout(10, 0));
        card.setBackground(CARD_BACKGROUND);
        card.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Panneau gauche avec case à cocher
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setBackground(CARD_BACKGROUND);

        JCheckBox checkBox = new JCheckBox();
        checkBox.setBackground(CARD_BACKGROUND);
        checkBox.setSelected(true);
        checkBox.addActionListener(e -> triggerUpdate());

        leftPanel.add(checkBox, BorderLayout.CENTER);

        // Panneau central avec informations du filtre
        JPanel centerPanel = new JPanel(new GridLayout(2, 1, 0, 3));
        centerPanel.setBackground(CARD_BACKGROUND);

        JLabel nameLabel = new JLabel(name);
        nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        nameLabel.setForeground(Color.decode(color));

        JLabel patternLabel = new JLabel("Pattern: " + pattern);
        patternLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        patternLabel.setForeground(new Color(100, 100, 100));

        centerPanel.add(nameLabel);
        centerPanel.add(patternLabel);

        // Panneau droit avec boutons d'action
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        rightPanel.setBackground(CARD_BACKGROUND);

        // Bouton d'édition
        JButton editButton = new JButton("Modifier");
        editButton.setFont(BUTTON_FONT);
        editButton.setFocusPainted(false);
        editButton.setMargin(new Insets(4, 8, 4, 8));
        editButton.addActionListener(e -> editFilterDialog(id, name, pattern, color));

        // Bouton de suppression
        JButton deleteButton = new JButton("Supprimer");
        deleteButton.setFont(BUTTON_FONT);
        deleteButton.setFocusPainted(false);
        deleteButton.setMargin(new Insets(4, 8, 4, 8));
        deleteButton.addActionListener(e -> deleteFilter(id));

        rightPanel.add(editButton);
        rightPanel.add(deleteButton);

        // Ajouter les panneaux à la carte
        card.add(leftPanel, BorderLayout.WEST);
        card.add(centerPanel, BorderLayout.CENTER);
        card.add(rightPanel, BorderLayout.EAST);

        // Ajouter des effets de survol
        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                card.setBackground(HOVER_COLOR);
                leftPanel.setBackground(HOVER_COLOR);
                centerPanel.setBackground(HOVER_COLOR);
                rightPanel.setBackground(HOVER_COLOR);
                checkBox.setBackground(HOVER_COLOR);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                card.setBackground(CARD_BACKGROUND);
                leftPanel.setBackground(CARD_BACKGROUND);
                centerPanel.setBackground(CARD_BACKGROUND);
                rightPanel.setBackground(CARD_BACKGROUND);
                checkBox.setBackground(CARD_BACKGROUND);
            }
        });

        return card;
    }

    /**
     * Supprime un filtre
     */
    private void deleteFilter(int id) {
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Êtes-vous sûr de vouloir supprimer ce filtre ?",
                "Confirmation de suppression",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            try (Connection conn = DriverManager.getConnection("jdbc:sqlite:bdd.db")) {
                PreparedStatement ps = conn.prepareStatement("DELETE FROM custom_filter WHERE id = ?");
                ps.setInt(1, id);
                ps.executeUpdate();
                loadFilters();
            } catch (SQLException e) {
                showError("Erreur lors de la suppression du filtre: " + e.getMessage());
            }
        }
    }

    /**
     * Notifie les changements de filtres
     */
    private void triggerUpdate() {
        if (onFiltersUpdated != null) {
            onFiltersUpdated.accept(getActiveFilters());
        }
    }

    /**
     * Récupère les filtres actifs
     */
    private List<FilterRule> getActiveFilters() {
        List<FilterRule> result = new ArrayList<>();

        for (Component comp : filterListPanel.getComponents()) {
            if (comp instanceof JPanel panel) {
                Component[] children = panel.getComponents();
                if (children.length > 0 && children[0] instanceof JPanel leftPanel) {
                    Component[] leftChildren = leftPanel.getComponents();
                    if (leftChildren.length > 0 && leftChildren[0] instanceof JCheckBox cb && cb.isSelected()) {
                        if (children.length > 1 && children[1] instanceof JPanel centerPanel) {
                            Component[] centerChildren = centerPanel.getComponents();
                            if (centerChildren.length > 1) {
                                JLabel nameLabel = (JLabel) centerChildren[0];
                                JLabel patternLabel = (JLabel) centerChildren[1];

                                String pattern = patternLabel.getText().replace("Pattern: ", "");
                                String colorHex = String.format("#%02x%02x%02x",
                                        nameLabel.getForeground().getRed(),
                                        nameLabel.getForeground().getGreen(),
                                        nameLabel.getForeground().getBlue());

                                result.add(new FilterRule(pattern, colorHex));
                            }
                        }
                    }
                }
            }
        }

        return result;
    }

    /**
     * Affiche la boîte de dialogue pour ajouter un nouveau filtre
     */
    private void newFilterDialog() {
        // Créer un panneau pour le formulaire
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        // Champ de nom
        JLabel nameLabel = new JLabel("Nom du filtre:");
        nameLabel.setFont(FILTER_FONT);
        gbc.gridx = 0;
        gbc.gridy = 0;
        formPanel.add(nameLabel, gbc);

        JTextField nameField = new JTextField(20);
        nameField.setFont(FILTER_FONT);
        nameField.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDER_COLOR, 1),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        gbc.gridx = 0;
        gbc.gridy = 1;
        formPanel.add(nameField, gbc);

        // Champ de pattern
        JLabel patternLabel = new JLabel("Pattern (bits ou hex):");
        patternLabel.setFont(FILTER_FONT);
        gbc.gridx = 0;
        gbc.gridy = 2;
        formPanel.add(patternLabel, gbc);

        JTextField patternField = new JTextField(20);
        patternField.setFont(FILTER_FONT);
        patternField.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDER_COLOR, 1),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        gbc.gridx = 0;
        gbc.gridy = 3;
        formPanel.add(patternField, gbc);

        // Champ de couleur
        JLabel colorLabel = new JLabel("Couleur:");
        colorLabel.setFont(FILTER_FONT);
        gbc.gridx = 0;
        gbc.gridy = 4;
        formPanel.add(colorLabel, gbc);

        JPanel colorPanel = new JPanel(new BorderLayout(10, 0));
        colorPanel.setBorder(BorderFactory.createEmptyBorder());

        JTextField colorField = new JTextField("#3366CC");
        colorField.setFont(FILTER_FONT);
        colorField.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDER_COLOR, 1),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));

        JButton colorPickerButton = new JButton("Choisir");
        colorPickerButton.setFont(BUTTON_FONT);
        colorPickerButton.setFocusPainted(false);
        colorPickerButton.addActionListener(e -> {
            Color initialColor = Color.decode(colorField.getText());
            Color selectedColor = JColorChooser.showDialog(this, "Choisir une couleur", initialColor);
            if (selectedColor != null) {
                String hex = String.format("#%02x%02x%02x",
                        selectedColor.getRed(),
                        selectedColor.getGreen(),
                        selectedColor.getBlue());
                colorField.setText(hex);
                colorField.setForeground(selectedColor);
            }
        });

        colorPanel.add(colorField, BorderLayout.CENTER);
        colorPanel.add(colorPickerButton, BorderLayout.EAST);

        gbc.gridx = 0;
        gbc.gridy = 5;
        formPanel.add(colorPanel, gbc);

        // Afficher la boîte de dialogue
        int result = JOptionPane.showConfirmDialog(
                this,
                formPanel,
                "Ajouter un nouveau filtre",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        // Traiter le résultat
        if (result == JOptionPane.OK_OPTION) {
            String name = nameField.getText().trim();
            String pattern = patternField.getText().trim();
            String color = colorField.getText().trim();

            if (name.isEmpty() || pattern.isEmpty() || color.isEmpty()) {
                showError("Tous les champs sont obligatoires.");
                return;
            }

            try {
                // Valider la couleur
                Color.decode(color);

                // Ajouter le filtre à la base de données
                try (Connection conn = DriverManager.getConnection("jdbc:sqlite:bdd.db")) {
                    PreparedStatement ps = conn.prepareStatement("INSERT INTO custom_filter (name, pattern, color) VALUES (?, ?, ?)");
                    ps.setString(1, name);
                    ps.setString(2, pattern);
                    ps.setString(3, color);
                    ps.executeUpdate();
                    loadFilters();
                    showSuccess("Filtre ajouté avec succès.");
                } catch (SQLException e) {
                    showError("Erreur lors de l'ajout du filtre: " + e.getMessage());
                }
            } catch (NumberFormatException e) {
                showError("Format de couleur invalide. Utilisez le format #RRGGBB.");
            }
        }
    }

    /**
     * Affiche la boîte de dialogue pour modifier un filtre existant
     */
    private void editFilterDialog(int id, String name, String pattern, String color) {
        // Créer un panneau pour le formulaire
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        // Champ de nom
        JLabel nameLabel = new JLabel("Nom du filtre:");
        nameLabel.setFont(FILTER_FONT);
        gbc.gridx = 0;
        gbc.gridy = 0;
        formPanel.add(nameLabel, gbc);

        JTextField nameField = new JTextField(name, 20);
        nameField.setFont(FILTER_FONT);
        nameField.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDER_COLOR, 1),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        gbc.gridx = 0;
        gbc.gridy = 1;
        formPanel.add(nameField, gbc);

        // Champ de pattern
        JLabel patternLabel = new JLabel("Pattern (bits ou hex):");
        patternLabel.setFont(FILTER_FONT);
        gbc.gridx = 0;
        gbc.gridy = 2;
        formPanel.add(patternLabel, gbc);

        JTextField patternField = new JTextField(pattern, 20);
        patternField.setFont(FILTER_FONT);
        patternField.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDER_COLOR, 1),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        gbc.gridx = 0;
        gbc.gridy = 3;
        formPanel.add(patternField, gbc);

        // Champ de couleur
        JLabel colorLabel = new JLabel("Couleur:");
        colorLabel.setFont(FILTER_FONT);
        gbc.gridx = 0;
        gbc.gridy = 4;
        formPanel.add(colorLabel, gbc);

        JPanel colorPanel = new JPanel(new BorderLayout(10, 0));
        colorPanel.setBorder(BorderFactory.createEmptyBorder());

        JTextField colorField = new JTextField(color);
        colorField.setFont(FILTER_FONT);
        colorField.setForeground(Color.decode(color));
        colorField.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDER_COLOR, 1),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));

        JButton colorPickerButton = new JButton("Choisir");
        colorPickerButton.setFont(BUTTON_FONT);
        colorPickerButton.setFocusPainted(false);
        colorPickerButton.addActionListener(e -> {
            Color initialColor = Color.decode(colorField.getText());
            Color selectedColor = JColorChooser.showDialog(this, "Choisir une couleur", initialColor);
            if (selectedColor != null) {
                String hex = String.format("#%02x%02x%02x",
                        selectedColor.getRed(),
                        selectedColor.getGreen(),
                        selectedColor.getBlue());
                colorField.setText(hex);
                colorField.setForeground(selectedColor);
            }
        });

        colorPanel.add(colorField, BorderLayout.CENTER);
        colorPanel.add(colorPickerButton, BorderLayout.EAST);

        gbc.gridx = 0;
        gbc.gridy = 5;
        formPanel.add(colorPanel, gbc);

        // Afficher la boîte de dialogue
        int result = JOptionPane.showConfirmDialog(
                this,
                formPanel,
                "Modifier le filtre",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        // Traiter le résultat
        if (result == JOptionPane.OK_OPTION) {
            String newName = nameField.getText().trim();
            String newPattern = patternField.getText().trim();
            String newColor = colorField.getText().trim();

            if (newName.isEmpty() || newPattern.isEmpty() || newColor.isEmpty()) {
                showError("Tous les champs sont obligatoires.");
                return;
            }

            try {
                // Valider la couleur
                Color.decode(newColor);

                // Mettre à jour le filtre dans la base de données
                try (Connection conn = DriverManager.getConnection("jdbc:sqlite:bdd.db")) {
                    PreparedStatement ps = conn.prepareStatement("UPDATE custom_filter SET name=?, pattern=?, color=? WHERE id=?");
                    ps.setString(1, newName);
                    ps.setString(2, newPattern);
                    ps.setString(3, newColor);
                    ps.setInt(4, id);
                    ps.executeUpdate();
                    loadFilters();
                    showSuccess("Filtre mis à jour avec succès.");
                } catch (SQLException e) {
                    showError("Erreur lors de la mise à jour du filtre: " + e.getMessage());
                }
            } catch (NumberFormatException e) {
                showError("Format de couleur invalide. Utilisez le format #RRGGBB.");
            }
        }
    }

    /**
     * Affiche un message d'erreur
     */
    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Erreur", JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Affiche un message de succès
     */
    private void showSuccess(String message) {
        JOptionPane.showMessageDialog(this, message, "Succès", JOptionPane.INFORMATION_MESSAGE);
    }
}
