package org.sncf.gui;

import org.sncf.gui.ui.ToolbarPanel;
import org.sncf.gui.ui.views.FilterView;
import org.sncf.gui.ui.views.GraphView;
import org.sncf.gui.ui.views.MessageView;

import javax.swing.*;
import java.awt.*;
import java.net.URL;

/**
 * Classe principale de l'application ANALSETRAM©.
 *
 * <p>Cette classe hérite de {@link JFrame} et sert de conteneur principal
 * pour les différentes vues de l’interface utilisateur : messages, graphique et filtres.</p>
 *
 * <p>Elle intègre un système de navigation via {@link CardLayout} pour basculer entre les vues,
 * ainsi qu’une barre d’outils {@link ToolbarPanel} pour les interactions principales.</p>
 *
 * <p>Les trames reçues sont synchronisées entre la vue message {@link MessageView}
 * et la vue graphique {@link GraphView}, avec une gestion centralisée des filtres via {@link FilterView}.</p>
 */
public class AnalyseTram extends JFrame {
    private MessageView messageView;
    private GraphView graphView;
    private JPanel mainContentPanel;
    private CardLayout cardLayout;

    /**
     * Initialise l'interface principale de l'application,
     * configure la fenêtre, la barre d'outils, et les vues internes.
     */
    public AnalyseTram() {
        setupMainFrame();
        createToolbarAndMainView();
    }

    /**
     * Configure les propriétés principales de la fenêtre JFrame :
     * taille, titre, fermeture.
     */
    private void setupMainFrame() {
        setTitle("ANALYSETRAM©");
        setSize(1200, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
    }

    /**
     * Crée les vues principales (Message, Graphique, Filtres),
     * installe la barre d’outils et configure la logique de navigation.
     */
    private void createToolbarAndMainView() {
        messageView = new MessageView();
        graphView = new GraphView();

        FilterView filterView = new FilterView();
        filterView.setOnFiltersUpdated(filters -> {
            messageView.setCurrentFilters(filters);
            messageView.refreshWithFilters(filters);
            graphView.setFilters(filters);
        });


        cardLayout = new CardLayout();
        mainContentPanel = new JPanel(cardLayout);
        mainContentPanel.add(messageView, "MESSAGE");
        mainContentPanel.add(graphView, "GRAPH");
        mainContentPanel.add(filterView, "FILTER");

        ToolbarPanel toolbar = new ToolbarPanel(
                () -> {
                    messageView.clearMessages();
                    graphView.clear(); // 👈 AJOUT ICI
                },
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

    /**
     * Affiche la vue des messages dans le panneau principal.
     */
    private void showMessageView() {
        cardLayout.show(mainContentPanel, "MESSAGE");
    }

    /**
     * Affiche la vue graphique dans le panneau principal.
     */
    private void showGraphView() {
        cardLayout.show(mainContentPanel, "GRAPH");
    }

    /**
     * Affiche la vue des filtres dans le panneau principal.
     */
    private void showFilterView() {
        cardLayout.show(mainContentPanel, "FILTER");
    }

    /**
     * Point d’entrée de l’application.
     * <p>Configure le look & feel Swing, instancie la fenêtre principale
     * et rend l’interface visible à l'utilisateur.</p>
     *
     * @param args non utilisé.
     */
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