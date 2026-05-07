package org.search;

import org.utils.Utilities;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import java.util.List;

public class SearcherGUI extends JFrame
{
    private JTextField queryField;
    private JTextField typeField;
    private JPanel resultsPanel;
    private Searcher searcher;

    public SearcherGUI()
    {
        searcher = new Searcher();
        setTitle("Medical Search Engine");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 700);
        setLayout(new BorderLayout());

        // Top Panel -> Input
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        topPanel.add(new JLabel("Query:"));
        queryField = new JTextField(20);
        topPanel.add(queryField);

        topPanel.add(new JLabel("Type:"));
        typeField = new JTextField(15);
        topPanel.add(typeField);

        JButton searchButton = new JButton("Search");
        searchButton.setFocusPainted(false);
        searchButton.addActionListener(e -> performSearch());
        topPanel.add(searchButton);

        add(topPanel, BorderLayout.NORTH);

        // Bottom Panel -> Results
        resultsPanel = new JPanel();
        resultsPanel.setLayout(new BoxLayout(resultsPanel, BoxLayout.Y_AXIS));
        JScrollPane scrollPane = new JScrollPane(resultsPanel);
        add(scrollPane, BorderLayout.CENTER);

        setVisible(true);
    }

    private void performSearch()
    {
        String query = typeField.getText();
        String type = typeField.getText().trim();
        if (query.trim().isEmpty() ||  type.trim().isEmpty())
            return;

        resultsPanel.removeAll();

        List<Searcher.ResultsData> results = searcher.vsmSeach(query, type);

        for (int i = 0; i < Math.min(20, results.size()); i++) {
            resultsPanel.add(createResultCard(results.get(i), i + 1, query));
            resultsPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        }

        resultsPanel.revalidate();
        resultsPanel.repaint();
    }

    private JPanel createResultCard(Searcher.ResultsData data, int rank, String query)
    {
        JPanel card = new JPanel(new BorderLayout());
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY),
                new EmptyBorder(10, 10, 10, 10)));
        card.setBackground(Color.WHITE);
        card.setMaximumSize(new Dimension(850, 120));
        card.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Rank Label
        JLabel rankLabel = new JLabel(rank + ". ");
        rankLabel.setFont(new Font("Arial", Font.BOLD, 14));
        card.add(rankLabel, BorderLayout.WEST);

        // Content
        JPanel infoPanel = new JPanel(new GridLayout(3, 1));
        infoPanel.setOpaque(false);

        JLabel pathLabel = new JLabel(data.path);
        pathLabel.setForeground(Color.BLUE);

        String snippetText = Utilities.getSnippet(data.path, query);
        JLabel snippetLabel = new JLabel("<html><i>" + snippetText + "</i></html>");

        JLabel scoreLabel = new JLabel(String.format("Score: %.6f", data.score));
        scoreLabel.setFont(new Font("Arial", Font.PLAIN, 11));

        infoPanel.add(pathLabel);
        infoPanel.add(snippetLabel);
        infoPanel.add(scoreLabel);

        card.add(infoPanel, BorderLayout.CENTER);

        // Functionality to open each card
        card.addMouseListener(new java.awt.event.MouseAdapter()
        {
            public void mouseClicked(java.awt.event.MouseEvent evt)
            {
                openFile(data.path);
            }
        });

        return card;
    }

    private void openFile(String path)
    {
        try {
            File file = new File(path);
            if (file.exists())
            {
                Desktop.getDesktop().open(file);
            }
            else
            {
                JOptionPane.showMessageDialog(this, "File not found: " + path);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error opening file: " + e.getMessage());
        }
    }

    public static void main(String[] args)
    {
        SwingUtilities.invokeLater(SearcherGUI::new);
    }
}
