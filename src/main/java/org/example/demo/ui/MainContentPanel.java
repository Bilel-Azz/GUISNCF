package org.example.demo.ui;

import javax.swing.*;
import java.awt.*;
import org.example.demo.ui.views.GraphViewPanel;
import org.example.demo.ui.views.MessageViewPanel;

public class MainContentPanel extends JPanel {
    private JTextArea hexArea;
    private JPanel rightPanel;
    private CardLayout cardLayout;

    public MainContentPanel() {
        setLayout(new BorderLayout());

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);

        // Zone des codes hexadécimaux
        hexArea = new JTextArea();
        hexArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        hexArea.setText(getInitialHexCodes());
        hexArea.setEditable(false);
        JScrollPane leftScrollPane = new JScrollPane(hexArea);

        // Vue droite
        cardLayout = new CardLayout();
        rightPanel = new JPanel(cardLayout);

        MessageViewPanel messageView = new MessageViewPanel();
        GraphViewPanel graphView = new GraphViewPanel();

        rightPanel.add(messageView, "MESSAGE");
        rightPanel.add(graphView, "GRAPH");

        splitPane.setLeftComponent(leftScrollPane);
        splitPane.setRightComponent(rightPanel);
        splitPane.setDividerLocation(400);

        add(splitPane, BorderLayout.CENTER);
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
