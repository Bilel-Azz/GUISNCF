package org.example.demo.ui.dialogs;

import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;

public class AddPortConfigDialog extends JDialog {
    public AddPortConfigDialog(JFrame parent) {
        super(parent, "Nouvelle configuration", true);
        setSize(350, 300);
        setLocationRelativeTo(parent);
        setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 10, 5, 10);

        // Champs
        JTextField baudrateField = new JTextField();
        JComboBox<String> parityBox = new JComboBox<>(new String[]{"none", "even", "odd"});
        JTextField databitsField = new JTextField();
        JTextField stopbitsField = new JTextField();

        String[] labels = {"Baudrate:", "Parité:", "Data bits:", "Stop bits:"};
        Component[] fields = {baudrateField, parityBox, databitsField, stopbitsField};

        for (int i = 0; i < labels.length; i++) {
            gbc.gridx = 0;
            gbc.gridy = i;
            add(new JLabel(labels[i]), gbc);

            gbc.gridx = 1;
            add(fields[i], gbc);
        }

        // Bouton Ajouter
        JButton addButton = new JButton("Ajouter");
        gbc.gridx = 0;
        gbc.gridy = labels.length;
        gbc.gridwidth = 2;
        add(addButton, gbc);

        addButton.addActionListener(e -> {
            try {
                int baudrate = Integer.parseInt(baudrateField.getText());
                String parity = parityBox.getSelectedItem().toString();
                int databits = Integer.parseInt(databitsField.getText());
                int stopbits = Integer.parseInt(stopbitsField.getText());

                Connection conn = DriverManager.getConnection("jdbc:sqlite:bdd.db");

                String sql = "INSERT INTO port_config (baudrate, parity, databits, stopbits) VALUES (?, ?, ?, ?)";
                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setInt(1, baudrate);
                stmt.setString(2, parity);
                stmt.setInt(3, databits);
                stmt.setInt(4, stopbits);
                stmt.executeUpdate();

                conn.close();
                JOptionPane.showMessageDialog(this, "Configuration ajoutée !");
                dispose();

            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Erreur : " + ex.getMessage());
            }
        });
    }
}
