package org.example.demo.ui.views;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import org.example.demo.ui.components.SignalGraphPanel;

public class GraphViewPanel extends JPanel {
    public GraphViewPanel() {
        setLayout(new BorderLayout());

        SignalGraphPanel graphPanel = new SignalGraphPanel();
        add(graphPanel, BorderLayout.CENTER);

        JPanel filterPanel = new JPanel();
        filterPanel.setLayout(new BoxLayout(filterPanel, BoxLayout.Y_AXIS));

        JPanel addFilterPanel = new JPanel(new BorderLayout());
        JButton addFilterBtn = new JButton("Ajouter un filtre ➕");
        addFilterBtn.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        addFilterPanel.add(addFilterBtn, BorderLayout.NORTH);

        JPanel filtersPanel = new JPanel();
        filtersPanel.setLayout(new BoxLayout(filtersPanel, BoxLayout.Y_AXIS));
        addFilter(filtersPanel, "Filtre 1", true);
        addFilter(filtersPanel, "Filtre 2", false);

        addFilterBtn.addActionListener(new ActionListener() {
            int filterCount = 3;
            @Override
            public void actionPerformed(ActionEvent e) {
                addFilter(filtersPanel, "Filtre " + filterCount++, false);
                filtersPanel.revalidate();
                filtersPanel.repaint();
            }
        });

        filterPanel.add(addFilterPanel);
        filterPanel.add(filtersPanel);
        add(filterPanel, BorderLayout.SOUTH);
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
}
