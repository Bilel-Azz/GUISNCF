// ToolbarPanel.java
package org.example.demo.ui;

import org.example.demo.ui.components.PortConfigSelectorPanel;
import org.example.demo.ui.components.RoundRectBorder;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

public class ToolbarPanel extends JPanel {

    public ToolbarPanel(Runnable onSendInit, Consumer<String> onTrameReceived,
                        Runnable onMessageClick, Runnable onGraphClick, Runnable onFilterClick) {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        leftPanel.setBackground(Color.WHITE);

        JLabel logo = new JLabel("∞ - ANALYSETRAM©");
        logo.setFont(new Font("Arial", Font.BOLD, 14));
        leftPanel.add(logo);

        JButton graphiqueBtn = createToolbarButton("Graphique", "📈");
        JButton messageBtn = createToolbarButton("Message", "💬");
        JButton filtreBtn = createToolbarButton("Filtre", "🔍");

        graphiqueBtn.addActionListener(e -> onGraphClick.run());
        messageBtn.addActionListener(e -> onMessageClick.run());
        filtreBtn.addActionListener(e -> onFilterClick.run());

        leftPanel.add(graphiqueBtn);
        leftPanel.add(messageBtn);
        leftPanel.add(filtreBtn);

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        rightPanel.setBackground(Color.WHITE);

        JButton protocolBtn = createToolbarButton("Protocol", "➕");
        rightPanel.add(protocolBtn);
        protocolBtn.addActionListener(e -> {
            JFrame parent = (JFrame) SwingUtilities.getWindowAncestor(this);
            new org.example.demo.ui.dialogs.AddPortConfigDialog(parent).setVisible(true);
        });

        PortConfigSelectorPanel configSelector = new PortConfigSelectorPanel(
                onSendInit,
                onTrameReceived
        );
        rightPanel.add(configSelector);

        add(leftPanel, BorderLayout.WEST);
        add(rightPanel, BorderLayout.EAST);
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
}