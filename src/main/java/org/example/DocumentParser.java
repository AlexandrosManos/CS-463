package org.example;

import gr.uoc.csd.hy463.NXMLFileReader;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class DocumentParser {

    private TextAnalyzer analyzer;
    private Indexer indexer;

    public DocumentParser(TextAnalyzer analyzer, Indexer indexer) {
        this.analyzer = analyzer;
        this.indexer = indexer;
    }

    public void countWords(File nxmlFile, int currentDocId, String collectionPath) {
        // A temporary set to track which words appeared
        Set<String> tempVoc = new HashSet<>();
        Map<String, Integer> fileTermFrequencies = new HashMap<>();
        AtomicInteger positionCounter = new AtomicInteger(0);
        try {
            NXMLFileReader xmlFile = new NXMLFileReader(nxmlFile);

            // Extract content and pass the temporary set
            processTag(xmlFile.getTitle(), "Title", tempVoc, fileTermFrequencies, currentDocId, positionCounter);
            processTag(xmlFile.getAbstr(), "Abstract", tempVoc, fileTermFrequencies, currentDocId, positionCounter);
            processTag(xmlFile.getBody(), "Body", tempVoc, fileTermFrequencies, currentDocId, positionCounter);
            processTag(xmlFile.getJournal(), "Journal", tempVoc, fileTermFrequencies, currentDocId, positionCounter);
            processTag(xmlFile.getPublisher(), "Publisher", tempVoc, fileTermFrequencies, currentDocId,
                    positionCounter);

            if (xmlFile.getAuthors() != null) {
                for (String author : xmlFile.getAuthors()) {
                    processTag(author, "Authors", tempVoc, fileTermFrequencies, currentDocId, positionCounter);
                }
            }
            if (xmlFile.getCategories() != null) {
                for (String category : xmlFile.getCategories()) {
                    processTag(category, "Categories", tempVoc, fileTermFrequencies, currentDocId, positionCounter);
                }
            }
            // Once the file is fully processed, increment the DF
            for (String word : tempVoc) {
                indexer.getVocab().get(word).incrementDF();
            }

            double sumOfSquares = 0;
            for (int tf : fileTermFrequencies.values()) {
                double weight = 1 + (Math.log(tf) / Math.log(2));
                sumOfSquares += Math.pow(weight, 2);
            }
            double norm = Math.sqrt(sumOfSquares);

            String relPath = nxmlFile.getPath().replace(collectionPath, "");
            indexer.getDocumentList().add(new DocInfo(currentDocId, relPath, norm));
        } catch (Exception e) {
            System.err.println("Error parsing NXML: " + e.getMessage());
        }
    }

    private void processTag(String content, String tagName, Set<String> tempVoc,
            Map<String, Integer> fileTermFrequencies, int currentDocId, AtomicInteger positionCounter) {
        if (content == null || content.trim().isEmpty())
            return;

        List<String> tokens = analyzer.analyze(content);

        for (String stmTok : tokens) {
            int currentPos = positionCounter.incrementAndGet();

            // System.out.println(token +"->" + Stemmer.Stem(token));
            // If term doesn't exist, create a new TermInfo object
            // if (!vocab.containsKey(token)) {
            // TermInfo newFolder = new TermInfo();
            // vocab.put(token, newFolder);
            // }
            // Goated function
            // --> B1
            // vocab.putIfAbsent(token, new TermInfo());
            // vocab.get(token).Occurs(tagName);

            indexer.getVocab().putIfAbsent(stmTok, new TermInfo());

            // Update the frequency for this specific tag
            indexer.getVocab().get(stmTok).Occurs(tagName);

            // Map the term to the current Document ID and record its exact position
            indexer.getVocab().get(stmTok).AddOccurrence(currentDocId, currentPos);

            fileTermFrequencies.put(stmTok, fileTermFrequencies.getOrDefault(stmTok, 0) + 1);
            tempVoc.add(stmTok);

        }
    }
}