package org.search;

import gr.uoc.csd.hy463.Topic;
import gr.uoc.csd.hy463.TopicsReader;
import org.example.TextAnalyzer;
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
    private TextAnalyzer analyzer = new TextAnalyzer(true);

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

    public static class ResultsData
    {
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

        public int getDocID()
        {
            return docID;
        }

        public double getNorm()
        {
            return norm;
        }

        public String getPath()
        {
            return path;
        }

        public String getDetails()
        {
            return details;
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
//            String line = reader.readLine(); // skip header
            String line;
            while ((line = reader.readLine()) != null)
            {
                String[] parts = line.split("\t");
                int id = Integer.parseInt(parts[0]);
                String path = parts[1];
                // Safety feature
                double norm = Double.parseDouble(parts[2].replace(",", "."));
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
    public List<ResultsData> vsmSeach(String query, String type)
    {
        List<String> tokens = analyzer.analyze(query);
        if (tokens.isEmpty())
            return new ArrayList<>();

        // Count term frequencies within the query
        Map<String, Integer> queryFreqs = new HashMap<>();
        for (String t : tokens) queryFreqs.put(t, queryFreqs.getOrDefault(t, 0) + 1);

        Map<Integer, Double> docScores = new HashMap<>();
        Map<Integer, StringBuilder> docDetails = new HashMap<>();
        double queryNormSum = 0;

        for (String term : queryFreqs.keySet()) {
            VocabularyData entry = localVocab.get(term);
            if (entry == null) {
                System.out.println("Term [" + term + "] is OOV");
                String correctedTerm = handleOOV(term);
                if (correctedTerm == null)
                    continue;
                else
                    entry = localVocab.get(correctedTerm);
            }

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
                    // Code for snippet ...
//                    List<Integer> docPositions = new ArrayList<>();
//                    int realPos = 0;

                    DocumentData docData = localDocs.get(docID);

                    if (type != null && !type.isEmpty()) {
                        if (!docData.path.toLowerCase().contains(type.toLowerCase())) {
                            if (docData.path.contains("MiniCollection")){
                                for (int p = 0; p < freqInDoc; p++) raf.readInt();
                                continue;
                            }
                        }
                    }

                    for (int p = 0; p < freqInDoc; p++) {
                        raf.readInt(); // Skip positional data
//                        realPos += raf.readInt();
//                        docPositions.add(realPos);
                    }

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
            DocumentData docData = localDocs.get(docID);

            if (docData == null) {
                System.err.println("Warning: Document ID " + docID + " not found in localDocs!");
                continue;
            }
            double docNorm =docData.norm;

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
        return results;
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

    public void automatedEvaluation(String xmlPath, boolean summary)
    {
        File outputDir = new File("results");
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        File outputFile = new File(outputDir,"results.txt");
        String runName = "CS_463";

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
            System.out.println("Starting batch evaluation... Writing to results_summary.txt");
            ArrayList<Topic> topics = TopicsReader.readTopics(xmlPath);

            for (Topic topic : topics) {
                String topicNumber = String.valueOf(topic.getNumber());

                String text = summary ? topic.getSummary() : topic.getDescription();

                List<ResultsData> results = vsmSeach(text, "");
                int limit = Math.min(results.size(), 1000);

                for (int i = 0; i < limit; i++) {
                    ResultsData doc = results.get(i);

                    String fullPath = doc.getPath();
                    String pmcid = new File(fullPath).getName().replace(".nxml", "");

                    double score = doc.score;

                    int rank = i + 1;

                    // FORMAT: TOPIC_NO Q0 PMCID RANK SCORE RUN_NAME
                    writer.write(topicNumber + " Q0 " + pmcid + " " + rank + " " + String.format(Locale.US, "%.6f", score) + " " + runName);
                    writer.newLine();
                }
                System.out.println("Topic " + topicNumber + " processed (" + limit + " results written).");
            }

            System.out.println("Successfully generated results_summary.txt!");

        } catch (Exception e) {
            System.err.println("Error during automated evaluation: " + e.getMessage());
            e.printStackTrace();
        }
    }


    private String handleOOV(String oovTerm) {
        String bestMatch = null;
        int minDistance = Integer.MAX_VALUE;
        int thres;

        if(oovTerm == null || oovTerm.isEmpty())
            return null;

        // for bigger terms we are more tolerant
        thres = oovTerm.length()/3 + 1;

        for (String vocabTerm : localVocab.keySet()) {
            if (Math.abs(vocabTerm.length() - oovTerm.length()) > thres) {
                continue;
            }
            int distance = Utilities.editDistance(oovTerm, vocabTerm);
            if (distance < minDistance) {
                minDistance = distance;
                bestMatch = vocabTerm;
            }

            if (minDistance <= 1) break;
        }

        if (bestMatch != null && minDistance <= thres) {
            System.out.println("Out of Vocabulary term [" + oovTerm + "] Replaced with [" + bestMatch + "]" +
                    "with edit Distance: [" + minDistance + "]");
            return bestMatch;
        } else {
            System.out.println("Out of Vocabulary term [" + oovTerm + "] has no close match found.");
            return null;
        }
    }


    public static void main(String[] args)
    {
        Searcher searcher = new Searcher();
        Scanner scanner = new Scanner(System.in);

        System.out.println("--- Search Engine ---");
        long startTime;
        long endTime;
        while (true)
        {
            System.out.println("1. Simple Search");
            System.out.println("2. Evaluate Topics");
            System.out.println("3. Exit");
            System.out.print("Type the number of the option: ");
            String input = scanner.nextLine();
            if (input.equals("1")) {
                System.out.print("\nEnter term to search (or 'exit'): ");
                input = scanner.nextLine();
                if (input.equals("exit"))
                    break;
                if (!input.trim().isEmpty()){
                    startTime = System.currentTimeMillis();
                    searcher.vsmSeach(input, "");
                    endTime = System.currentTimeMillis();
                    System.out.println("Simple Search completed in: " + (endTime - startTime) + " ms.");
                }
            } else if (input.equals("2")) {
                System.out.println("1) Use Summary to Create Query");
                System.out.println("2) Use Description to Create Query");
                System.out.print("Type the number of the option: ");
                input = scanner.nextLine();
                boolean summary = true;
                 if (input.equals("2")) {
                     summary = false;
                }else if (!input.equals("1")){
                    System.out.println("Invalid choice.. using summary...");
                }
                String topicPath = "dataset/topics.xml";
                File topics = new File(topicPath);
                if (topics.exists()) {
                    startTime = System.currentTimeMillis();
                    searcher.automatedEvaluation(topicPath, summary);
                    endTime = System.currentTimeMillis();
                    System.out.println(" Total evaluation time: " + (endTime - startTime) + " ms.");
                } else {
                    System.err.println("File [" + topicPath + "] not found.");
                }
            }else if (input.equals("3")) {
                System.out.println("Exiting...");
                break;
            }else {
                System.out.println("Invalid option. Try again.");
            }

        }
        scanner.close();
    }
}