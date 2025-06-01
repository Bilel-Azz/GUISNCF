package org.sncf.gui.ui.dialogs;
import org.sncf.gui.services.DatabaseManager;

import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;

/**
 * Fenêtre de dialogue permettant à l'utilisateur d'ajouter une nouvelle configuration
 * de port série dans la base de données locale.
 * <p>
 * Cette fenêtre propose un formulaire avec les champs :
 * <ul>
 *     <li>Baudrate</li>
 *     <li>Parité</li>
 *     <li>Data bits</li>
 *     <li>Stop bits</li>
 * </ul>
 * Lors de la validation, la configuration est insérée dans la table {@code port_config}.
 */
public class AddPortConfigDialog extends JDialog {

    /**
     * Construit et initialise la boîte de dialogue d’ajout de configuration série.
     *
     * @param parent la fenêtre principale à laquelle cette boîte est liée.
     */
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

        // Bouton "Ajouter"
        addButton.addActionListener(e -> {
            // Récupération des valeurs des champs
            // Connexion à la base de données SQLite
            // Insertion dans la table port_config
            // Fermeture de la boîte de dialogue et message de confirmation
            try {
                int baudrate = Integer.parseInt(baudrateField.getText());
                String parity = parityBox.getSelectedItem().toString();
                int databits = Integer.parseInt(databitsField.getText());
                int stopbits = Integer.parseInt(stopbitsField.getText());

                Connection conn = DriverManager.getConnection(DatabaseManager.getDbUrl());

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