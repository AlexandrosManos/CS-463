package org.search;

import org.example.TextAnalyzer; // Κάνε import τον Analyzer από το πακέτο του
import org.utils.Utilities;

import java.io.*;
import java.util.*;

public class Searcher
{
    private static class VocabularyData
    {
        int df;
        int tf;
        long offset;

        VocabularyData(int df, int tf, long offset) {
            this.df = df;
            this.tf = tf;
            this.offset = offset;
        }
    }

    private Map<String, VocabularyData> localVocab = new TreeMap<>();
    private TextAnalyzer analyzer = new TextAnalyzer();

    private static class DocumentData
    {
        String path;
        double norm;

        DocumentData(String path, double norm)
        {
            this.path = path;
            this.norm = norm;
        }
    }

    private static class ResultsData {
        int docID;
        double score;
        double norm;
        String path;
        String details;

        ResultsData(int docID, double score, double norm, String path, String details) {
            this.docID = docID;
            this.score = score;
            this.norm = norm;
            this.path = path;
            this.details = details;
        }
    }


    private Map<Integer, DocumentData> localDocs = new HashMap<>();
    private int totalDocuments = 0;

    private void LoadVocabulary() {
        File vocabFile = new File("CollectionIndex/VocabularyFile.txt");
        try (BufferedReader reader = new BufferedReader(new FileReader(vocabFile)))
        {
            String line = reader.readLine();
            //For each line aka word we take its properties(tf, df, offset)
            while ((line = reader.readLine()) != null)
            {
                String[] parts = line.split("\t");
                if (parts.length < 4) continue;

                String term = parts[0];
                int df = Integer.parseInt(parts[1]);
                int tf = Integer.parseInt(parts[2]);
                long offset = Long.parseLong(parts[3]);

                localVocab.put(term, new VocabularyData(df, tf, offset));
            }
            System.out.println("Vocabulary loaded: " + localVocab.size() + " terms.");
        } catch (IOException e) {
            System.err.println("Error loading vocabulary: " + e.getMessage());
        }
    }

    // Load each doc and store its info(id, path, norm)
    private void LoadDocuments()
    {
        File docFile = new File("CollectionIndex/DocumentsFile.txt");
        try (BufferedReader reader = new BufferedReader(new FileReader(docFile)))
        {
            String line = reader.readLine(); // skip header
            while ((line = reader.readLine()) != null)
            {
                String[] parts = line.split("\t");
                int id = Integer.parseInt(parts[0]);
                String path = parts[1];
                double norm = Double.parseDouble(parts[2]);
                localDocs.put(id, new DocumentData(path, norm));
                totalDocuments++;
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    public Searcher()
    {
        LoadVocabulary();
        LoadDocuments();
    }

    // Search based on VSM using Cosine Similarity
    public void vsmSeach(String query)
    {
        List<String> tokens = analyzer.analyze(query);
        if (tokens.isEmpty()) return;

        // Count term frequencies within the query
        Map<String, Integer> queryFreqs = new HashMap<>();
        for (String t : tokens) queryFreqs.put(t, queryFreqs.getOrDefault(t, 0) + 1);

        Map<Integer, Double> docScores = new HashMap<>();
        Map<Integer, StringBuilder> docDetails = new HashMap<>();
        double queryNormSum = 0;

        for (String term : queryFreqs.keySet()) {
            VocabularyData entry = localVocab.get(term);
            if (entry == null) continue;

            // Calculate Query Weight
            double tfq = Utilities.calculateTF(queryFreqs.get(term));
            double idf = Utilities.calculateIDF(totalDocuments, entry.df);
            double weightQ = Utilities.calculateWeight(tfq, idf);

            // Sum of squares for Query Vector Normalization
            queryNormSum += Math.pow(weightQ, 2);

            try (RandomAccessFile raf = new RandomAccessFile("CollectionIndex/PostingFile.bin", "r")) {
                raf.seek(entry.offset);

                // Process all documents containing this term
                for (int i = 0; i < entry.df; i++) {
                    int docID = raf.readInt();
                    int freqInDoc = raf.readInt();
                    for (int p = 0; p < freqInDoc; p++) raf.readInt(); // Skip positional data

                    // Calculate Document Weight
                    double tfD = Utilities.calculateTF(freqInDoc);
                    double weightD = Utilities.calculateWeight(tfD, idf);

                    docScores.put(docID, docScores.getOrDefault(docID, 0.0) + (weightD * weightQ));
                    docDetails.putIfAbsent(docID, new StringBuilder());
                    docDetails.get(docID).append(String.format("[%s|tf:%.2f,idf:%.2f] ", term, tfD, idf));
                }
            } catch (IOException e) { e.printStackTrace(); }
        }

        // Calculate final Query Vector Norm
        double queryNorm = Math.sqrt(queryNormSum);
        List<ResultsData> results = new ArrayList<>();

        for (Map.Entry<Integer, Double> entry : docScores.entrySet()) {
            int docID = entry.getKey();
            double docNorm = localDocs.get(docID).norm;

            // Cosine Similarity
            double score = Utilities.calculateCosineSimilarity(entry.getValue(), docNorm, queryNorm);

            // Store results for display
            results.add(new ResultsData(
                    docID,
                    score,
                    docNorm,
                    localDocs.get(docID).path,
                    docDetails.get(docID).toString()
            ));
        }

        // Sort by score
        results.sort((a, b) -> Double.compare(b.score, a.score));

        displayResults(results, query);
    }

    private void displayResults(List<ResultsData> results, String query) {
        System.out.println("\n" + "=".repeat(120));
        System.out.println("SEARCH RESULTS FOR: \"" + query + "\"");
        System.out.println("=".repeat(120));

        System.out.printf("%-5s | %-6s | %-10s | %-8s | %-70s | %s\n",
                "Rank", "DocID", "Score", "NormD", "Term Details (TF, IDF)", "File Path");
        System.out.println("-".repeat(140));

        int limit = Math.min(10, results.size());
        for (int i = 0; i < limit; i++) {
            ResultsData res = results.get(i);
            System.out.printf("%-5d | %-6d | %-10.6f | %-8.4f | %-70s | %s\n",
                    (i + 1), res.docID, res.score, res.norm, res.details, res.path);
        }
        System.out.println("=".repeat(120) + "\n");
    }


    public static void main(String[] args)
    {
        Searcher searcher = new Searcher();
        Scanner scanner = new Scanner(System.in);

        System.out.println("--- Simple Search---");
        while (true)
        {
            System.out.print("\nEnter term to search (or 'exit'): ");
            String input = scanner.nextLine();
            if (input.equalsIgnoreCase("exit"))
                break;
            if (input.trim().isEmpty())
                continue;

            searcher.vsmSeach(input);
        }
        scanner.close();
    }
}