package org.example.demo.ui;

import org.example.demo.ui.components.PortConfigSelectorPanel;
import org.example.demo.ui.components.RoundRectBorder;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.util.function.Consumer;

public class ToolbarPanel extends JPanel {
    // Couleurs cohérentes avec MainContentPanel
    private static final Color BACKGROUND_COLOR = new Color(245, 245, 245);
    private static final Color HEADER_COLOR = new Color(60, 63, 65);
    private static final Color ACCENT_COLOR = new Color(78, 110, 142);
    private static final Color BUTTON_HOVER_COLOR = new Color(230, 235, 240);
    private static final Color BUTTON_ACTIVE_COLOR = new Color(220, 225, 230);

    // Polices
    private static final Font LOGO_FONT = new Font("Segoe UI", Font.BOLD, 16);
    private static final Font BUTTON_FONT = new Font("Segoe UI", Font.PLAIN, 13);

    // Boutons de navigation
    private JButton graphiqueBtn;
    private JButton messageBtn;
    private JButton filtreBtn;

    // État actif
    private JButton activeButton;

    public ToolbarPanel(Runnable onSendInit, Consumer<String> onTrameReceived,
                        Runnable onMessageClick, Runnable onGraphClick, Runnable onFilterClick) {
        setLayout(new BorderLayout());
        setBackground(BACKGROUND_COLOR);
        setBorder(new CompoundBorder(
                new MatteBorder(0, 0, 1, 0, new Color(220, 220, 220)),
                new EmptyBorder(8, 15, 8, 15)
        ));

        // Panel gauche avec logo et boutons de navigation
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.X_AXIS));
        leftPanel.setBackground(BACKGROUND_COLOR);
        leftPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        // Logo avec style amélioré
        JLabel logo = new JLabel("∞ ANALYSETRAM©");
        logo.setFont(LOGO_FONT);
        logo.setForeground(HEADER_COLOR);
        logo.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 25));
        leftPanel.add(logo);

        // Séparateur vertical
        JSeparator separator = new JSeparator(JSeparator.VERTICAL);
        separator.setPreferredSize(new Dimension(1, 24));
        separator.setMaximumSize(new Dimension(1, 24));
        separator.setForeground(new Color(220, 220, 220));
        leftPanel.add(separator);
        leftPanel.add(Box.createRigidArea(new Dimension(20, 0)));

        // Boutons de navigation avec style amélioré
        graphiqueBtn = createToolbarButton("Graphique", "📈");
        messageBtn = createToolbarButton("Message", "💬");
        filtreBtn = createToolbarButton("Filtre", "🔍");

        // Ajouter des écouteurs d'événements
        graphiqueBtn.addActionListener(e -> {
            setActiveButton(graphiqueBtn);
            onGraphClick.run();
        });

        messageBtn.addActionListener(e -> {
            setActiveButton(messageBtn);
            onMessageClick.run();
        });

        filtreBtn.addActionListener(e -> {
            setActiveButton(filtreBtn);
            onFilterClick.run();
        });

        // Ajouter des tooltips
        graphiqueBtn.setToolTipText("Afficher la vue graphique");
        messageBtn.setToolTipText("Afficher la vue message");
        filtreBtn.setToolTipText("Ouvrir les options de filtrage");

        leftPanel.add(graphiqueBtn);
        leftPanel.add(Box.createRigidArea(new Dimension(5, 0)));
        leftPanel.add(messageBtn);
        leftPanel.add(Box.createRigidArea(new Dimension(5, 0)));
        leftPanel.add(filtreBtn);

        // Panel droit avec bouton de protocole et sélecteur de configuration
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.X_AXIS));
        rightPanel.setBackground(BACKGROUND_COLOR);

        

        // Sélecteur de configuration de port avec style amélioré
        PortConfigSelectorPanel configSelector = new PortConfigSelectorPanel(
                onSendInit,
                onTrameReceived
        );
        configSelector.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));

        rightPanel.add(Box.createHorizontalGlue());
        rightPanel.add(Box.createRigidArea(new Dimension(15, 0)));
        rightPanel.add(configSelector);

        add(leftPanel, BorderLayout.WEST);
        add(rightPanel, BorderLayout.EAST);

        // Définir le bouton Message comme actif par défaut
        setActiveButton(messageBtn);
    }

    /**
     * Crée un bouton de navigation pour la barre d'outils
     */
    private JButton createToolbarButton(String text, String icon) {
        JButton button = new JButton(text + " " + icon);
        button.setFont(BUTTON_FONT);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setOpaque(true);
        button.setBackground(BACKGROUND_COLOR);
        button.setForeground(HEADER_COLOR);
        button.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));

        // Ajouter des effets de survol
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (button != activeButton) {
                    button.setBackground(BUTTON_HOVER_COLOR);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (button != activeButton) {
                    button.setBackground(BACKGROUND_COLOR);
                }
            }
        });

        return button;
    }

    /**
     * Crée un bouton d'action (comme le bouton Protocol)
     */
    private JButton createActionButton(String text, String icon) {
        JButton button = new JButton(text + " " + icon);
        button.setFont(BUTTON_FONT);
        button.setFocusPainted(false);
        button.setBackground(ACCENT_COLOR);
        button.setForeground(Color.WHITE);
        button.setBorder(BorderFactory.createCompoundBorder(
                new RoundRectBorder(6, ACCENT_COLOR),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)
        ));

        // Ajouter des effets de survol
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(ACCENT_COLOR.darker());
                button.setBorder(BorderFactory.createCompoundBorder(
                        new RoundRectBorder(6, ACCENT_COLOR.darker()),
                        BorderFactory.createEmptyBorder(6, 12, 6, 12)
                ));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(ACCENT_COLOR);
                button.setBorder(BorderFactory.createCompoundBorder(
                        new RoundRectBorder(6, ACCENT_COLOR),
                        BorderFactory.createEmptyBorder(6, 12, 6, 12)
                ));
            }
        });

        return button;
    }

    /**
     * Définit le bouton actif et met à jour l'interface en conséquence
     */
    private void setActiveButton(JButton button) {
        // Réinitialiser l'ancien bouton actif
        if (activeButton != null) {
            activeButton.setBackground(BACKGROUND_COLOR);
            activeButton.setForeground(HEADER_COLOR);
            activeButton.setBorderPainted(false);
        }

        // Définir le nouveau bouton actif
        activeButton = button;
        activeButton.setBackground(BUTTON_ACTIVE_COLOR);
        activeButton.setForeground(ACCENT_COLOR);

        // Ajouter une bordure inférieure pour indiquer le bouton actif
        activeButton.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 2, 0, ACCENT_COLOR),
                BorderFactory.createEmptyBorder(6, 12, 4, 12)
        ));
        activeButton.setBorderPainted(true);
    }
}