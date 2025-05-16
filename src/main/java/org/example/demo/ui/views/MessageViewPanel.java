package org.example.demo.ui.views;

import javax.swing.*;
import java.awt.*;
import org.example.demo.model.TrainInfo;

public class MessageViewPanel extends JPanel {
    public MessageViewPanel() {
        setLayout(new BorderLayout());

        JTextArea infoArea = new JTextArea();
        infoArea.setFont(new Font("SansSerif", Font.PLAIN, 12));
        infoArea.setEditable(false);

        updateTrainInfo(infoArea);
        add(new JScrollPane(infoArea), BorderLayout.CENTER);
    }

    private void updateTrainInfo(JTextArea infoArea) {
        StringBuilder info = new StringBuilder();

        // Données simulées, à remplacer par des objets TrainInfo si besoin
        info.append(TrainInfo.generate("1A", "0B 2D", "5F", 10, true, 20, "Green", 1, 0, 25));
        info.append(TrainInfo.generate("1B", "0C 2E", "60", 12, false, 16, "Red", 3, 1, 47));
        info.append(TrainInfo.generate("1C", "0D 2F", "61", 13, true, 18, "Green", 2, 0, 32));
        info.append(TrainInfo.generate("1D", "0A 30", "62", 15, false, 20, "Red", 1, 0, 15));
        info.append(TrainInfo.generate("1E", "0E 31", "63", 18, true, 22, "Green", 3, 0, 10));
        info.append(TrainInfo.generate("1F", "0F 32", "64", 20, false, 24, "Green", 1, 0, 12));

        infoArea.setText(info.toString());
    }
}
