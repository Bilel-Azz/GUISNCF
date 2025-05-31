package org.sncf.gui.services;

import org.sncf.gui.model.FilterRule;

import javax.swing.*;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.List;

/**
 * Service responsable de l'export des trames vers un fichier local.
 * Permet l'export au format CSV ou JSON, depuis la mémoire ou la base de données,
 * avec ou sans filtrage.
 */
public class ExportService {
    private final TrameService trameService;

    /**
     * Crée un nouveau service d'export.
     *
     * @param trameService le service de trames utilisé pour accéder ou filtrer les données à exporter.
     */
    public ExportService(TrameService trameService) {
        this.trameService = trameService;
    }

    /**
     * Lance l’export des trames vers un fichier local, selon les options sélectionnées.
     * Une boîte de dialogue permet à l’utilisateur de choisir le fichier de destination.
     *
     * @param inMemoryTrames liste des trames en mémoire si l'export ne se fait pas depuis la base.
     * @param fromDb         true pour exporter les trames depuis la base de données, false pour utiliser celles en mémoire.
     * @param toCsv          true pour exporter au format CSV, false pour exporter au format JSON.
     * @param onlyFiltered   true pour n’exporter que les trames filtrées, false pour toutes les trames.
     * @param filters        liste des règles de filtrage à appliquer si {@code onlyFiltered} est true.
     */
    public void export(List<TrameService.TrameEntry> inMemoryTrames, boolean fromDb, boolean toCsv, boolean onlyFiltered, List<FilterRule> filters) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Choisir un fichier de destination");
        int userSelection = fileChooser.showSaveDialog(null);
        if (userSelection != JFileChooser.APPROVE_OPTION) return;

        try (PrintWriter writer = new PrintWriter(new FileWriter(fileChooser.getSelectedFile()))) {
            List<TrameService.TrameEntry> exportList = fromDb
                    ? trameService.loadAllFromDb()
                    : inMemoryTrames;

            if (onlyFiltered) {
                exportList = trameService.filterTrames(exportList, filters);
            }

            if (toCsv) {
                writer.println("bits,hex,text");
                for (TrameService.TrameEntry t : exportList) {
                    writer.printf("\"%s\",\"%s\",\"%s\"\n", t.bits, t.hex, t.text);
                }
            } else {
                writer.println("[");
                for (int i = 0; i < exportList.size(); i++) {
                    TrameService.TrameEntry t = exportList.get(i);
                    writer.printf("  {\"bits\": \"%s\", \"hex\": \"%s\", \"text\": \"%s\"}%s\n",
                            escape(t.bits), escape(t.hex), escape(t.text),
                            (i < exportList.size() - 1 ? "," : ""));
                }
                writer.println("]");
            }

            JOptionPane.showMessageDialog(null, "Export réussi !");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Erreur export : " + e.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Échappe les caractères spéciaux d’une chaîne (guillemets, antislash) pour une sortie JSON valide.
     *
     * @param input la chaîne à échapper.
     * @return chaîne sécurisée pour une inclusion dans du JSON.
     */
    private String escape(String input) {
        return input.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}