package org.sncf.gui.ui.dialogs;

import javax.swing.*;
import java.awt.*;
import java.sql.*;

/**
 * Fenêtre de dialogue permettant à l’utilisateur de modifier une configuration
 * de port série existante dans la base de données.
 * <p>
 * Elle affiche les champs suivants pour modification :
 * <ul>
 *     <li>Baudrate</li>
 *     <li>Parité</li>
 *     <li>Data bits</li>
 *     <li>Stop bits</li>
 * </ul>
 * Les valeurs sont pré-remplies depuis la base en fonction de l’ID fourni.
 * À la validation, la configuration est mise à jour dans la table {@code port_config}.
 */
public class EditPortConfigDialog extends JDialog {

    /**
     * Construit la boîte de dialogue d’édition d’une configuration série.
     *
     * @param parent    la fenêtre parente de la boîte de dialogue.
     * @param configId  l’identifiant de la configuration à modifier.
     */
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

        // Pré-remplissage des champs depuis la base de données
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