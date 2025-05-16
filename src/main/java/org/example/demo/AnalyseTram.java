// AnalyseTram.java
package org.example.demo;

import org.example.demo.ui.ToolbarPanel;
import org.example.demo.ui.views.FilterView;
import org.example.demo.ui.views.GraphView;
import org.example.demo.ui.views.MessageView;

import javax.swing.*;
import java.awt.*;

public class AnalyseTram extends JFrame {
    private MessageView messageView;
    private GraphView graphView;
    private JPanel mainContentPanel;
    private CardLayout cardLayout;

    public AnalyseTram() {
        setupMainFrame();
        createToolbarAndMainView();
    }

    private void setupMainFrame() {
        setTitle("ANALYSETRAM©");
        setSize(1200, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
    }

    private void createToolbarAndMainView() {
        messageView = new MessageView();
        graphView = new GraphView();

        FilterView filterView = new FilterView();
        filterView.setOnFiltersUpdated(filters -> messageView.refreshWithFilters(filters));  // 👈 ICI

        cardLayout = new CardLayout();
        mainContentPanel = new JPanel(cardLayout);
        mainContentPanel.add(messageView, "MESSAGE");
        mainContentPanel.add(graphView.asScrollableView(), "GRAPH");
        mainContentPanel.add(filterView, "FILTER");

        ToolbarPanel toolbar = new ToolbarPanel(
                () -> messageView.clearMessages(),
                bitLine -> {
                    messageView.appendMessage(bitLine);
                    graphView.appendBits(bitLine);
                },
                this::showMessageView,
                this::showGraphView,
                this::showFilterView
        );

        add(toolbar, BorderLayout.NORTH);
        add(mainContentPanel, BorderLayout.CENTER);

        showMessageView();
    }

    private void showMessageView() {
        cardLayout.show(mainContentPanel, "MESSAGE");
    }

    private void showGraphView() {
        cardLayout.show(mainContentPanel, "GRAPH");
    }

    private void showFilterView() {
        cardLayout.show(mainContentPanel, "FILTER");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            AnalyseTram app = new AnalyseTram();
            app.setVisible(true);
        });
    }
}