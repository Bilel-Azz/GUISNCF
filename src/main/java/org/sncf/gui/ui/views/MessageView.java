package org.sncf.gui.ui.views;

import org.sncf.gui.model.FilterRule;
import org.sncf.gui.serial.SerialTransmitter;
import org.sncf.gui.services.*;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Composant Swing permettant d'afficher, filtrer, exporter et annoter les trames
 * reçues depuis un port série ou générées en mode simulation.
 *
 * <p>Affiche les trames sous trois formes :</p>
 * <ul>
 *     <li>Bits binaires</li>
 *     <li>Hexadécimal</li>
 *     <li>Texte interprété (via dictionnaire ou ASCII brut)</li>
 * </ul>
 *
 * <p>Permet également de :</p>
 * <ul>
 *     <li>Basculer le mode simulation</li>
 *     <li>Ajouter au dictionnaire</li>
 *     <li>Exporter les trames en CSV ou JSON</li>
 *     <li>Appliquer des filtres colorés (bit, hex, texte)</li>
 * </ul>
 */
public class MessageView extends JPanel {
    private final JTextPane bitPane = createTextPane();
    private final JTextPane hexPane = createTextPane();
    private final JTextPane textPane = createTextPane();

    private final JButton toggleSimulationBtn;
    private final List<TrameService.TrameEntry> trames = new ArrayList<>();
    private List<FilterRule> currentFilters = new ArrayList<>();
    private final Highlighter.HighlightPainter syncPainter = new DefaultHighlighter.DefaultHighlightPainter(new Color(255, 200, 100, 128));

    private final MouseAdapter syncMouseAdapter = new MouseAdapter() {
        @Override
        public void mousePressed(java.awt.event.MouseEvent e) {
            JTextPane source = (JTextPane) e.getSource();
            try {
                int caretPos = source.getCaretPosition();
                int line = source.getDocument().getDefaultRootElement().getElementIndex(caretPos);
                syncHighlight(line);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    };

    // Services
    private final DatabaseManager db = new DatabaseManager();
    private final DictionaryService dictionaryService = new DictionaryService(db);
    private final TrameService trameService = new TrameService(db, dictionaryService);
    private final ExportService exportService = new ExportService(trameService);

    /**
     * Initialise la vue, les services internes (dictionnaire, export, base),
     * et construit l'interface utilisateur avec les boutons d'action.
     */
    public MessageView() {
        setLayout(new BorderLayout());
        bitPane.addMouseListener(syncMouseAdapter);
        hexPane.addMouseListener(syncMouseAdapter);
        textPane.addMouseListener(syncMouseAdapter);
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        toggleSimulationBtn = new JButton(getButtonLabel());
        toggleSimulationBtn.addActionListener(e -> toggleSimulation());

        JButton clearBtn = createIconTextButton("reini.png","Réinitialiser", "Réinitialiser");
        clearBtn.addActionListener(e -> clearMessages());

        JButton clearDbBtn = createIconTextButton("balai.png","Vider la base","Vider la base");
        clearDbBtn.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Voulez-vous vraiment supprimer toutes les trames de la base de données ?",
                    "Confirmation", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                try {
                    db.clearTrames();
                    JOptionPane.showMessageDialog(this, "Base de données vidée avec succès.");
                } catch (Exception ex) {
                    showError("Erreur lors de la suppression : " + ex.getMessage());
                }
            }
        });

        JButton exportBtn = createIconTextButton("envelope.png","Exporter", "Exporter");
        exportBtn.addActionListener(e -> openExportDialog());

        JButton viewDictBtn = createIconTextButton("dico.png","Dictionnaire", "Dictionnaire");
        viewDictBtn.addActionListener(e -> openDictionaryViewer());

        topPanel.add(viewDictBtn);
        topPanel.add(clearBtn);
        topPanel.add(clearDbBtn);
        topPanel.add(exportBtn);
        topPanel.add(toggleSimulationBtn);

        add(topPanel, BorderLayout.NORTH);
        add(buildMainSplitPane(), BorderLayout.CENTER);
    }

    /**
     * Crée un bouton stylisé avec une icône et un texte alignés à gauche.
     * <p>
     * L’icône est chargée depuis le dossier <code>/icons/</code> dans le classpath, redimensionnée
     * en 16×16 pixels, puis affichée à gauche du texte. Le bouton utilise un style plat
     * avec fond blanc, police moderne, et une infobulle (tooltip) personnalisée.
     *
     * @param iconFileName le nom de fichier de l’icône (ex : <code>"export.png"</code>), située dans <code>/resources/icons</code>
     * @param text         le texte à afficher à droite de l’icône (ex : <code>"Exporter"</code>)
     * @param tooltip      le texte de l’info-bulle affichée au survol du bouton
     * @return un bouton Swing prêt à être intégré dans une interface utilisateur
     * @throws IllegalArgumentException si l’icône spécifiée n’est pas trouvée dans le classpath
     */
    private JButton createIconTextButton(String iconFileName, String text, String tooltip) {
        URL iconUrl = getClass().getResource("/icons/" + iconFileName);
        if (iconUrl == null) {
            throw new IllegalArgumentException("Icon not found: " + iconFileName);
        }

        ImageIcon icon = new ImageIcon(iconUrl);
        Image scaled = icon.getImage().getScaledInstance(16, 16, Image.SCALE_SMOOTH);
        ImageIcon scaledIcon = new ImageIcon(scaled);

        JButton button = new JButton(text, scaledIcon);
        button.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        button.setFocusPainted(false);
        button.setBackground(Color.WHITE);
        button.setToolTipText(tooltip);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        return button;
    }

    private void syncHighlight(int lineIndex) {
        try {
            Highlighter[] highlighters = {
                    bitPane.getHighlighter(),
                    hexPane.getHighlighter(),
                    textPane.getHighlighter()
            };
            JTextPane[] panes = {bitPane, hexPane, textPane};

            // Effacer anciens surlignements synchronisés
            for (Highlighter h : highlighters) h.removeAllHighlights();

            String[] bitLines = bitPane.getText().split("\n");
            String[] hexLines = hexPane.getText().split("\n");
            String[] textLines = textPane.getText().split("\n");

            if (lineIndex < bitLines.length) {
                int bitStart = getLineOffset(bitPane, lineIndex);
                highlighters[0].addHighlight(bitStart, bitStart + bitLines[lineIndex].length(), syncPainter);
            }

            if (lineIndex < hexLines.length) {
                int hexStart = getLineOffset(hexPane, lineIndex);
                highlighters[1].addHighlight(hexStart, hexStart + hexLines[lineIndex].length(), syncPainter);
            }

            if (lineIndex < textLines.length) {
                int textStart = getLineOffset(textPane, lineIndex);
                highlighters[2].addHighlight(textStart, textStart + textLines[lineIndex].length(), syncPainter);
            }

        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    private int getLineOffset(JTextPane pane, int line) throws BadLocationException {
        String[] lines = pane.getText().split("\n");
        int offset = 0;
        for (int i = 0; i < line; i++) {
            offset += lines[i].length() + 1;
        }
        return offset;
    }

    /**
     * Ouvre une fenêtre affichant le dictionnaire actuel contenant
     * les correspondances hexadécimal → texte définies par l'utilisateur.
     *
     * <p>Les entrées sont affichées sous forme de tableau avec deux colonnes :
     * <ul>
     *   <li>La séquence hexadécimale (ex : "4A 5E 34")</li>
     *   <li>La traduction associée (ex : "DEBUT")</li>
     * </ul>
     *
     * <p>Cette vue est en lecture seule (non modifiable).
     */
    private void openDictionaryViewer() {
        List<DictionaryService.DictionaryEntry> entries = dictionaryService.getAllEntries();

        String[] columns = {"Hexadécimal", "Traduction texte"};
        String[][] data = new String[entries.size()][2];

        for (int i = 0; i < entries.size(); i++) {
            DictionaryService.DictionaryEntry entry = entries.get(i);
            data[i][0] = entry.hexPattern;
            data[i][1] = entry.traduction;
        }

        JTable table = new JTable(data, columns);
        table.setFillsViewportHeight(true);
        table.setFont(new Font("Monospaced", Font.PLAIN, 12));
        table.setRowHeight(22);
        JScrollPane scrollPane = new JScrollPane(table);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(scrollPane, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton addBtn = new JButton("Ajouter une entrée");
        JButton deleteBtn = new JButton("Supprimer la sélection");
        deleteBtn.setEnabled(false);

        bottomPanel.add(addBtn);
        bottomPanel.add(deleteBtn);
        panel.add(bottomPanel, BorderLayout.SOUTH);

        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "📖 Dictionnaire hex → texte", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setContentPane(panel);
        dialog.setSize(500, 350);
        dialog.setLocationRelativeTo(this);

        // Gestion sélection
        table.getSelectionModel().addListSelectionListener(e -> {
            deleteBtn.setEnabled(table.getSelectedRow() != -1);
        });

        // Action suppression
        deleteBtn.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow != -1) {
                String hexToDelete = (String) table.getValueAt(selectedRow, 0);
                int confirm = JOptionPane.showConfirmDialog(dialog,
                        "Supprimer l'entrée \"" + hexToDelete + "\" du dictionnaire ?",
                        "Confirmation", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    dictionaryService.deleteEntry(hexToDelete);
                    dialog.dispose();
                    openDictionaryViewer();
                    recalculateDisplayedTranslations();
                }
            }
        });

        // Action ajout
        addBtn.addActionListener(e -> {
            String hex = JOptionPane.showInputDialog(dialog, "Hexadécimal (ex: 4A 5E 34 6C)");
            if (hex == null || hex.trim().isEmpty()) return;

            String label = JOptionPane.showInputDialog(dialog, "Traduction associée");
            if (label == null || label.trim().isEmpty()) return;

            try {
                dictionaryService.addEntry(hex, label);
                recalculateDisplayedTranslations();
                dialog.dispose();
                openDictionaryViewer();
            } catch (Exception ex) {
                showError("Erreur : " + ex.getMessage());
            }
        });

        dialog.setVisible(true);
    }

    /**
     * Recalcule les traductions texte de toutes les trames affichées
     * en fonction du dictionnaire mis à jour.
     * Met aussi à jour les données en base.
     */
    private void recalculateDisplayedTranslations() {
        List<TrameService.TrameEntry> updated = new ArrayList<>();
        for (TrameService.TrameEntry old : trames) {
            String newText = dictionaryService.convertHexToText(old.hex);
            updated.add(new TrameService.TrameEntry(old.bits, old.hex, newText));
            try {
                db.updateText(old.hex, newText);
            } catch (Exception e) {
                System.err.println("❌ Erreur mise à jour BDD : " + e.getMessage());
            }
        }
        trames.clear();
        trames.addAll(updated);
        refreshWithFilters(currentFilters);
    }

    /**
     * Crée un {@link JTextPane} avec style monospaced et non éditable.
     */
    private JTextPane createTextPane() {
        JTextPane pane = new JTextPane();
        pane.setFont(new Font("Monospaced", Font.PLAIN, 12));
        pane.setEditable(false);
        return pane;
    }

    /**
     * Construit le panneau principal contenant les trois zones d’affichage.
     */
    private JSplitPane buildMainSplitPane() {
        JSplitPane leftSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                wrapWithLabel("Bits reçus", bitPane),
                wrapWithLabel("Traduction hexadécimale", hexPane));
        leftSplit.setResizeWeight(0.5);

        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                leftSplit,
                wrapWithLabel("Traduction texte", textPane));
        mainSplit.setResizeWeight(0.5);
        return mainSplit;
    }

    /**
     * Construit un panneau avec un label et un JTextPane.
     */
    private JPanel wrapWithLabel(String label, JTextPane pane) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JLabel(label), BorderLayout.NORTH);
        panel.add(new JScrollPane(pane), BorderLayout.CENTER);
        return panel;
    }

    /**
     * Met à jour les filtres actuellement actifs et rafraîchit l’affichage en conséquence.
     *
     * @param filters liste de {@link FilterRule} à appliquer.
     */
    public void setCurrentFilters(List<FilterRule> filters) {
        this.currentFilters = filters;
    }

    /**
     * Ajoute une trame binaire (bits) à l'affichage, et la convertit
     * automatiquement en hexadécimal et en texte.
     *
     * @param bits la chaîne de bits à interpréter.
     */
    public void appendMessage(String bits) {
        TrameService.TrameEntry entry = trameService.processBits(bits);
        trames.add(entry);
        appendText(bitPane, entry.bits + "\n");
        appendText(hexPane, entry.hex + "\n");
        appendText(textPane, entry.text + "\n");
        trameService.saveTrame(entry);
    }

    /**
     * Efface les zones d'affichage (mais ne vide pas la liste interne des trames).
     */
    public void clearMessages() {
        bitPane.setText("");
        hexPane.setText("");
        textPane.setText("");
    }

    private enum FilterType {
        BITS, HEX, TEXT
    }

    private FilterType detectFilterType(String pattern) {
        String cleaned = pattern.replaceAll("\\s+", "").replace("*", "").toUpperCase();

        if (cleaned.matches("[01]+")) {
            return FilterType.BITS;
        } else if (cleaned.matches("([0-9A-F]{2})+")) {
            return FilterType.HEX;
        } else {
            return FilterType.TEXT;
        }
    }

    private List<int[]> findPatternMatches(String input, String pattern) {
        String cleanedInput = input.replaceAll("\\s+", "").toUpperCase();
        String cleanedPattern = pattern.replaceAll("\\s+", "").toUpperCase();
        String regex = Pattern.quote(cleanedPattern).replace("\\*", ".*?");
        Pattern compiled = Pattern.compile(regex);
        Matcher matcher = compiled.matcher(cleanedInput);

        List<int[]> ranges = new ArrayList<>();
        while (matcher.find()) {
            ranges.add(new int[]{matcher.start(), matcher.end()});
        }
        return ranges;
    }

    /**
     * Bascule l’état du mode simulation et met à jour le bouton.
     */
    private void toggleSimulation() {
        boolean current = SerialTransmitter.isSimulationMode();
        if (current) SerialTransmitter.stopSimulation();
        SerialTransmitter.setSimulationMode(!current);
        toggleSimulationBtn.setText(getButtonLabel());
    }

    /**
     * Retourne le texte du bouton simulation selon l’état actuel.
     */
    private String getButtonLabel() {
        return SerialTransmitter.isSimulationMode() ? "Simulation: ON ⏸" : "Simulation: OFF ▶";
    }

    /**
     * Ajoute du texte dans une zone et scrolle automatiquement vers le bas.
     */
    private void appendText(JTextPane pane, String text) {
        try {
            StyledDocument doc = pane.getStyledDocument();
            doc.insertString(doc.getLength(), text, null);
            pane.setCaretPosition(doc.getLength());
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    /**
     * Affiche une boîte de dialogue pour paramétrer et lancer l’export des trames.
     */
    private void openExportDialog() {
        String[] sources = {"Trames affichées", "Trames de la base de données"};
        String[] formats = {"CSV", "JSON"};
        JCheckBox onlyFiltered = new JCheckBox("Seulement les trames filtrées");

        JComboBox<String> sourceBox = new JComboBox<>(sources);
        JComboBox<String> formatBox = new JComboBox<>(formats);
        JPanel panel = new JPanel(new GridLayout(3, 2));
        panel.add(new JLabel("Exporter depuis :"));
        panel.add(sourceBox);
        panel.add(new JLabel("Format :"));
        panel.add(formatBox);
        panel.add(new JLabel(" "));
        panel.add(onlyFiltered);

        int result = JOptionPane.showConfirmDialog(this, panel, "Exporter les trames", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            boolean fromDb = sourceBox.getSelectedIndex() == 1;
            boolean toCsv = formatBox.getSelectedIndex() == 0;
            boolean onlyMatches = onlyFiltered.isSelected();
            exportService.export(trames, fromDb, toCsv, onlyMatches, currentFilters);
        }
    }

    /**
     * Calcule l’offset en caractères d’un token hexadécimal dans une ligne.
     */
    private int getHexOffset(String[] hexTokens, int index) {
        int offset = 0;
        for (int i = 0; i < index; i++) {
            offset += hexTokens[i].length() + 1; // +1 pour l’espace
        }
        return offset;
    }

    private boolean isHexWithWildcard(String pattern) {
        String clean = pattern.replaceAll("\\s+", "").toUpperCase();
        return clean.matches("([0-9A-F]{2}|\\*)+");
    }

    /**
     * Rafraîchit les trames affichées avec les filtres courants,
     * en appliquant les surlignages appropriés.
     *
     * @param filters filtres à appliquer pour le surlignage (bits, hex, texte).
     */
    public void refreshWithFilters(List<FilterRule> filters) {
        bitPane.setText("");
        hexPane.setText("");
        textPane.setText("");

        Highlighter bitsHighlighter = bitPane.getHighlighter();
        Highlighter hexHighlighter = hexPane.getHighlighter();
        Highlighter textHighlighter = textPane.getHighlighter();
        bitsHighlighter.removeAllHighlights();
        hexHighlighter.removeAllHighlights();
        textHighlighter.removeAllHighlights();

        List<TrameService.TrameEntry> filteredTrames = trameService.filterTrames(trames, filters);

        try {
            for (TrameService.TrameEntry entry : filteredTrames) {
                int bitOffset = bitPane.getDocument().getLength();
                int hexOffset = hexPane.getDocument().getLength();
                int textOffset = textPane.getDocument().getLength();

                appendText(bitPane, entry.bits + "\n");
                appendText(hexPane, entry.hex + "\n");
                appendText(textPane, entry.text + "\n");

                String[] hexTokens = entry.hex.split(" ");

                for (FilterRule rule : filters) {
                    FilterType type = detectFilterType(rule.pattern);

                    switch (type) {
                        case BITS -> {
                            List<int[]> matches = findPatternMatches(entry.bits, rule.pattern);
                            for (int[] range : matches) {
                                int start = range[0], end = range[1];
                                bitsHighlighter.addHighlight(bitOffset + start, bitOffset + end,
                                        new DefaultHighlighter.DefaultHighlightPainter(rule.color));

                                int startByte = start / 8;
                                int endByte = (end - 1) / 8;
                                for (int i = startByte; i <= endByte && i < hexTokens.length; i++) {
                                    int hexStart = hexOffset + getHexOffset(hexTokens, i);
                                    hexHighlighter.addHighlight(hexStart, hexStart + hexTokens[i].length(),
                                            new DefaultHighlighter.DefaultHighlightPainter(rule.color));

                                    if (i < entry.text.length()) {
                                        textHighlighter.addHighlight(textOffset + i, textOffset + i + 1,
                                                new DefaultHighlighter.DefaultHighlightPainter(rule.color));
                                    }
                                }
                            }
                        }

                        case HEX -> {
                            String cleanHex = entry.hex.replaceAll("\\s+", "").toUpperCase();
                            List<int[]> matches = findPatternMatches(cleanHex, rule.pattern);
                            for (int[] range : matches) {
                                int startByte = range[0] / 2;
                                int endByte = (range[1] - 1) / 2;
                                for (int i = startByte; i <= endByte && i < hexTokens.length; i++) {
                                    int hexStart = hexOffset + getHexOffset(hexTokens, i);
                                    hexHighlighter.addHighlight(hexStart, hexStart + hexTokens[i].length(),
                                            new DefaultHighlighter.DefaultHighlightPainter(rule.color));

                                    int bitIdx = i * 8;
                                    int bitEnd = Math.min(bitIdx + 8, entry.bits.length());
                                    bitsHighlighter.addHighlight(bitOffset + bitIdx, bitOffset + bitEnd,
                                            new DefaultHighlighter.DefaultHighlightPainter(rule.color));

                                    if (i < entry.text.length()) {
                                        textHighlighter.addHighlight(textOffset + i, textOffset + i + 1,
                                                new DefaultHighlighter.DefaultHighlightPainter(rule.color));
                                    }
                                }
                            }
                        }

                        case TEXT -> {
                            List<int[]> matches = findPatternMatches(entry.text, rule.pattern);
                            for (int[] range : matches) {
                                int start = range[0], end = range[1];
                                for (int i = start; i < end && i < entry.text.length(); i++) {
                                    textHighlighter.addHighlight(textOffset + i, textOffset + i + 1,
                                            new DefaultHighlighter.DefaultHighlightPainter(rule.color));

                                    int hexStart = hexOffset + getHexOffset(hexTokens, i);
                                    hexHighlighter.addHighlight(hexStart, hexStart + hexTokens[i].length(),
                                            new DefaultHighlighter.DefaultHighlightPainter(rule.color));

                                    int bitIdx = i * 8;
                                    int bitEnd = Math.min(bitIdx + 8, entry.bits.length());
                                    bitsHighlighter.addHighlight(bitOffset + bitIdx, bitOffset + bitEnd,
                                            new DefaultHighlighter.DefaultHighlightPainter(rule.color));
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Affiche une boîte de dialogue d'erreur.
     *
     * @param message message à afficher.
     */
    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Erreur", JOptionPane.ERROR_MESSAGE);
    }
}