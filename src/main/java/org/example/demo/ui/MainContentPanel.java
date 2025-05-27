package org.example.demo.ui;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import org.example.demo.ui.views.GraphViewPanel;
import org.example.demo.ui.views.MessageViewPanel;
import javax.swing.text.DefaultHighlighter;

public class MainContentPanel extends JPanel {
    private JTextArea hexArea;
    private JPanel rightPanel;
    private CardLayout cardLayout;
    private JToggleButton messageViewButton;
    private JToggleButton graphViewButton;
    private static final Color BACKGROUND_COLOR = new Color(245, 245, 245);
    private static final Color HEADER_COLOR = new Color(60, 63, 65);
    private static final Color ACCENT_COLOR = new Color(78, 110, 142);
    private static final Font HEADER_FONT = new Font("Segoe UI", Font.BOLD, 14);
    private static final Font CONTENT_FONT = new Font("Consolas", Font.PLAIN, 13);

    public MainContentPanel() {
        setLayout(new BorderLayout(5, 5));
        setBackground(BACKGROUND_COLOR);
        setBorder(new EmptyBorder(10, 10, 10, 10));

        // Header panel with view toggle buttons
        JPanel headerPanel = createHeaderPanel();
        add(headerPanel, BorderLayout.NORTH);

        // Main content with split pane
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerSize(5);
        splitPane.setBorder(null);
        splitPane.setBackground(BACKGROUND_COLOR);
        splitPane.setContinuousLayout(true);

        // Left panel - Hex codes with header
        JPanel leftPanel = new JPanel(new BorderLayout(0, 5));
        leftPanel.setBackground(BACKGROUND_COLOR);
        leftPanel.setBorder(new EmptyBorder(5, 0, 0, 5));

        JLabel hexLabel = new JLabel("Hex Codes");
        hexLabel.setFont(HEADER_FONT);
        hexLabel.setForeground(HEADER_COLOR);
        hexLabel.setBorder(new EmptyBorder(0, 5, 5, 0));
        leftPanel.add(hexLabel, BorderLayout.NORTH);

        // Hex area with improved styling
        hexArea = new JTextArea();
        hexArea.setFont(CONTENT_FONT);
        hexArea.setText(getInitialHexCodes());
        hexArea.setEditable(false);
        hexArea.setBackground(Color.WHITE);
        hexArea.setBorder(new EmptyBorder(5, 5, 5, 5));
        hexArea.setLineWrap(true);

        JScrollPane leftScrollPane = new JScrollPane(hexArea);
        leftScrollPane.setBorder(new CompoundBorder(
                new LineBorder(new Color(220, 220, 220), 1),
                new EmptyBorder(2, 2, 2, 2)
        ));
        leftPanel.add(leftScrollPane, BorderLayout.CENTER);

        // Add search functionality
        JPanel searchPanel = createSearchPanel();
        leftPanel.add(searchPanel, BorderLayout.SOUTH);

        // Right panel with card layout
        cardLayout = new CardLayout();
        rightPanel = new JPanel(cardLayout);
        rightPanel.setBorder(new CompoundBorder(
                new LineBorder(new Color(220, 220, 220), 1),
                new EmptyBorder(5, 5, 5, 5)
        ));
        rightPanel.setBackground(Color.WHITE);

        MessageViewPanel messageView = new MessageViewPanel();
        GraphViewPanel graphView = new GraphViewPanel();

        rightPanel.add(messageView, "MESSAGE");
        rightPanel.add(graphView, "GRAPH");

        splitPane.setLeftComponent(leftPanel);
        splitPane.setRightComponent(rightPanel);
        splitPane.setDividerLocation(400);

        add(splitPane, BorderLayout.CENTER);

        // Status bar
        JPanel statusBar = createStatusBar();
        add(statusBar, BorderLayout.SOUTH);
    }

    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BorderLayout());
        headerPanel.setBackground(BACKGROUND_COLOR);
        headerPanel.setBorder(new EmptyBorder(0, 0, 10, 0));

        // Title
        JLabel titleLabel = new JLabel("Data Analyzer");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        titleLabel.setForeground(HEADER_COLOR);
        headerPanel.add(titleLabel, BorderLayout.WEST);

        // View toggle buttons
        JPanel viewTogglePanel = new JPanel();
        viewTogglePanel.setBackground(BACKGROUND_COLOR);
        viewTogglePanel.setLayout(new FlowLayout(FlowLayout.RIGHT));

        ButtonGroup viewGroup = new ButtonGroup();

        messageViewButton = new JToggleButton("Message View");
        messageViewButton.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        messageViewButton.setSelected(true);
        messageViewButton.setFocusPainted(false);
        messageViewButton.addActionListener(e -> cardLayout.show(rightPanel, "MESSAGE"));

        graphViewButton = new JToggleButton("Graph View");
        graphViewButton.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        graphViewButton.setFocusPainted(false);
        graphViewButton.addActionListener(e -> cardLayout.show(rightPanel, "GRAPH"));

        viewGroup.add(messageViewButton);
        viewGroup.add(graphViewButton);

        viewTogglePanel.add(messageViewButton);
        viewTogglePanel.add(graphViewButton);

        headerPanel.add(viewTogglePanel, BorderLayout.EAST);

        return headerPanel;
    }

    private JPanel createSearchPanel() {
        JPanel searchPanel = new JPanel(new BorderLayout(5, 0));
        searchPanel.setBackground(BACKGROUND_COLOR);
        searchPanel.setBorder(new EmptyBorder(5, 0, 0, 0));

        JTextField searchField = new JTextField();
        searchField.setBorder(new CompoundBorder(
                new LineBorder(new Color(200, 200, 200), 1),
                new EmptyBorder(5, 5, 5, 5)
        ));
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        JButton searchButton = new JButton("Search");
        searchButton.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        searchButton.setFocusPainted(false);
        searchButton.addActionListener(e -> searchHexCodes(searchField.getText()));

        searchPanel.add(searchField, BorderLayout.CENTER);
        searchPanel.add(searchButton, BorderLayout.EAST);

        return searchPanel;
    }

    private JPanel createStatusBar() {
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBorder(new CompoundBorder(
                new MatteBorder(1, 0, 0, 0, new Color(220, 220, 220)),
                new EmptyBorder(5, 10, 5, 10)
        ));
        statusBar.setBackground(BACKGROUND_COLOR);

        JLabel statusLabel = new JLabel("Ready");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statusLabel.setForeground(new Color(100, 100, 100));

        statusBar.add(statusLabel, BorderLayout.WEST);

        return statusBar;
    }

    private void searchHexCodes(String searchText) {
        if (searchText == null || searchText.isEmpty()) {
            return;
        }

        String content = hexArea.getText();
        hexArea.requestFocusInWindow();

        // Reset any previous highlights
        hexArea.getHighlighter().removeAllHighlights();

        try {
            // Simple search implementation
            int index = content.indexOf(searchText);
            if (index >= 0) {
                hexArea.setCaretPosition(index);
                hexArea.getHighlighter().addHighlight(
                        index,
                        index + searchText.length(),
                        new DefaultHighlighter.DefaultHighlightPainter(new Color(255, 255, 0, 128))
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
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
}