// FilterView.java
package org.example.demo.ui.views;

import org.example.demo.model.FilterRule;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class FilterView extends JPanel {
    private final JPanel filterListPanel;
    private Consumer<List<FilterRule>> onFiltersUpdated;

    public void setOnFiltersUpdated(Consumer<List<FilterRule>> consumer) {
        this.onFiltersUpdated = consumer;
    }

    public FilterView() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        JLabel title = new JLabel("🎛️ Gestion des filtres personnalisés");
        title.setFont(new Font("SansSerif", Font.BOLD, 16));
        title.setBorder(new EmptyBorder(10, 10, 10, 10));

        JButton addFilterButton = new JButton("➕ Nouveau filtre");
        addFilterButton.addActionListener(e -> newFilterDialog());

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(Color.WHITE);
        topPanel.add(title, BorderLayout.WEST);
        topPanel.add(addFilterButton, BorderLayout.EAST);

        filterListPanel = new JPanel();
        filterListPanel.setLayout(new BoxLayout(filterListPanel, BoxLayout.Y_AXIS));
        filterListPanel.setBackground(Color.WHITE);

        JScrollPane scrollPane = new JScrollPane(filterListPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setBackground(Color.WHITE);

        add(topPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        loadFilters();
    }

    private void loadFilters() {
        filterListPanel.removeAll();

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:bdd.db")) {
            String query = "SELECT * FROM custom_filter";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(query);

            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("name");
                String pattern = rs.getString("pattern");
                String color = rs.getString("color");

                JCheckBox checkBox = new JCheckBox(name + " → pattern: " + pattern);
                checkBox.setBackground(Color.WHITE);
                checkBox.setForeground(Color.decode(color));
                checkBox.setSelected(true);
                checkBox.addActionListener(e -> triggerUpdate());

                JButton editBtn = new JButton("✏️");
                editBtn.setMargin(new Insets(2, 5, 2, 5));
                editBtn.addActionListener(e -> editFilterDialog(id, name, pattern, color));

                JPanel row = new JPanel(new BorderLayout());
                row.setBackground(Color.WHITE);
                row.add(checkBox, BorderLayout.CENTER);
                row.add(editBtn, BorderLayout.EAST);

                filterListPanel.add(row);
            }

            filterListPanel.revalidate();
            filterListPanel.repaint();

            triggerUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void triggerUpdate() {
        if (onFiltersUpdated != null) {
            onFiltersUpdated.accept(getActiveFilters());
        }
    }

    private List<FilterRule> getActiveFilters() {
        List<FilterRule> result = new ArrayList<>();
        for (Component comp : filterListPanel.getComponents()) {
            if (comp instanceof JPanel panel) {
                Component[] children = panel.getComponents();
                if (children.length > 0 && children[0] instanceof JCheckBox cb && cb.isSelected()) {
                    String[] split = cb.getText().split("→ pattern: ");
                    if (split.length == 2) {
                        String pattern = split[1];
                        String colorHex = String.format("#%02x%02x%02x",
                                cb.getForeground().getRed(),
                                cb.getForeground().getGreen(),
                                cb.getForeground().getBlue());
                        result.add(new FilterRule(pattern, colorHex));
                    }
                }
            }
        }
        return result;
    }

    private void newFilterDialog() {
        JTextField nameField = new JTextField();
        JTextField patternField = new JTextField();
        JTextField colorField = new JTextField("#FF0000");

        JPanel panel = new JPanel(new GridLayout(0, 1));
        panel.add(new JLabel("Nom du filtre:"));
        panel.add(nameField);
        panel.add(new JLabel("Pattern (bits ou hex):"));
        panel.add(patternField);
        panel.add(new JLabel("Couleur hexadécimale (#RRGGBB):"));
        panel.add(colorField);

        int result = JOptionPane.showConfirmDialog(this, panel, "Ajouter un filtre", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            try (Connection conn = DriverManager.getConnection("jdbc:sqlite:bdd.db")) {
                PreparedStatement ps = conn.prepareStatement("INSERT INTO custom_filter (name, pattern, color) VALUES (?, ?, ?)");
                ps.setString(1, nameField.getText());
                ps.setString(2, patternField.getText());
                ps.setString(3, colorField.getText());
                ps.executeUpdate();
                loadFilters();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private void editFilterDialog(int id, String name, String pattern, String color) {
        JTextField nameField = new JTextField(name);
        JTextField patternField = new JTextField(pattern);
        JTextField colorField = new JTextField(color);

        JPanel panel = new JPanel(new GridLayout(0, 1));
        panel.add(new JLabel("Nom du filtre:"));
        panel.add(nameField);
        panel.add(new JLabel("Pattern (bits ou hex):"));
        panel.add(patternField);
        panel.add(new JLabel("Couleur hexadécimale (#RRGGBB):"));
        panel.add(colorField);

        int result = JOptionPane.showConfirmDialog(this, panel, "Modifier le filtre", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            try (Connection conn = DriverManager.getConnection("jdbc:sqlite:bdd.db")) {
                PreparedStatement ps = conn.prepareStatement("UPDATE custom_filter SET name=?, pattern=?, color=? WHERE id=?");
                ps.setString(1, nameField.getText());
                ps.setString(2, patternField.getText());
                ps.setString(3, colorField.getText());
                ps.setInt(4, id);
                ps.executeUpdate();
                loadFilters();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
