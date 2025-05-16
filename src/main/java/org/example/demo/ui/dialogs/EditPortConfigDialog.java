package org.example.demo.ui.dialogs;

import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class EditPortConfigDialog extends JDialog {

    public EditPortConfigDialog(JFrame parent, int configId) {
        super(parent, "Modifier la configuration", true);
        setSize(350, 300);
        setLocationRelativeTo(parent);
        setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 10, 5, 10);

        // Champs du formulaire
        JTextField baudrateField = new JTextField();
        JComboBox<String> parityBox = new JComboBox<>(new String[]{"none", "even", "odd"});
        JTextField databitsField = new JTextField();
        JTextField stopbitsField = new JTextField();

        String[] labels = {"Baudrate:", "Parité:", "Data bits:", "Stop bits:"};
        Component[] fields = {baudrateField, parityBox, databitsField, stopbitsField};

        // Charger les données actuelles
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:bdd.db")) {
            PreparedStatement ps = conn.prepareStatement("SELECT * FROM port_config WHERE id = ?");
            ps.setInt(1, configId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                baudrateField.setText(String.valueOf(rs.getInt("baudrate")));
                parityBox.setSelectedItem(rs.getString("parity"));
                databitsField.setText(String.valueOf(rs.getInt("databits")));
                stopbitsField.setText(String.valueOf(rs.getInt("stopbits")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Erreur lors du chargement des données.");
            dispose();
        }

        // Placement des champs
        for (int i = 0; i < labels.length; i++) {
            gbc.gridx = 0;
            gbc.gridy = i;
            add(new JLabel(labels[i]), gbc);

            gbc.gridx = 1;
            add(fields[i], gbc);
        }

        // Bouton de sauvegarde
        JButton saveBtn = new JButton("Sauvegarder");
        gbc.gridx = 0;
        gbc.gridy = labels.length;
        gbc.gridwidth = 2;
        add(saveBtn, gbc);

        saveBtn.addActionListener(e -> {
            try (Connection conn = DriverManager.getConnection("jdbc:sqlite:bdd.db")) {
                PreparedStatement ps = conn.prepareStatement(
                        "UPDATE port_config SET baudrate=?, parity=?, databits=?, stopbits=? WHERE id=?");
                ps.setInt(1, Integer.parseInt(baudrateField.getText()));
                ps.setString(2, parityBox.getSelectedItem().toString());
                ps.setInt(3, Integer.parseInt(databitsField.getText()));
                ps.setInt(4, Integer.parseInt(stopbitsField.getText()));
                ps.setInt(5, configId);
                ps.executeUpdate();
                JOptionPane.showMessageDialog(this, "Configuration mise à jour !");
                dispose();
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Erreur : " + ex.getMessage());
            }
        });
    }
}
