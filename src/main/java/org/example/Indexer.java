package org.example;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class Indexer {

    // Global Vocabulary (Upgraded for B3/B4)
    private TreeMap<String, TermInfo> vocab;
    private List<DocInfo> documentList;

    public Indexer() {
        this.vocab = new TreeMap<>();
        this.documentList = new ArrayList<>();
    }

    public TreeMap<String, TermInfo> getVocab() {
        return vocab;
    }

    public List<DocInfo> getDocumentList() {
        return documentList;
    }

    // Create directory and write vocabulary file
    public void VocabularyFile() {
        File dir = new File("CollectionIndex");
        if (!dir.exists()) {
            if (dir.mkdir()) {
                System.out.println("Directory created.");
            }
        } else {
            // Debug
            System.out.println("Directory CollectionIndex already exists, continue...");
        }

        File vocabFile = new File(dir, "VocabularyFile.txt");
        File postingFile = new File(dir, "PostingFile.txt");
        // https://www.datacamp.com/doc/java/create-&-write-files
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(vocabFile));
                RandomAccessFile raf = new RandomAccessFile(postingFile, "rw")) {

            // Header
            writer.write("Term\tDocumentFrequency\tTermFrequency\tOffset");
            writer.newLine();

            // Terms, df, tf
            for (Map.Entry<String, TermInfo> entry : vocab.entrySet()) {
                String term = entry.getKey();
                TermInfo info = entry.getValue();

                // Get the current byte position (offset) before writing postings
                long currentOffset = raf.getFilePointer();
                // Write postings for this term to the binary file
                // Structure: [DocID]->[TF]->[Positions]
                for (Map.Entry<Integer, Posting> pEntry : info.GetPostings().entrySet()) {
                    Posting p = pEntry.getValue();

                    raf.writeInt(p.documentID);
                    raf.writeInt(p.GetTF());

                    int last = 0;
                    for (int pos : p.positions) {
                        int offset = pos - last;
                        raf.writeInt(offset);
                        last = pos;
                    }
                }

                // Updated Vocabulary record with the Offset
                writer.write(term + "\t" + info.getDF() + "\t" + info.totalFreqs() + "\t" + currentOffset);
                writer.newLine();
            }
            System.out.println("VocabularyFile.txt file saved.");

        } catch (java.io.IOException e) {
            System.err.println("Error writing Vocabulary file: " + e.getMessage());
        }
    }

    public void DocumentsFile() {
        File dir = new File("CollectionIndex");
        File docFile = new File(dir, "DocumentsFile.txt");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(docFile))) {
            for (DocInfo doc : documentList) {
                writer.write(doc.documentID + "\t" + doc.filePath + "\t" + String.format("%.4f", doc.documentNorm));
                writer.newLine();
            }
            System.out.println("DocumentsFile.txt saved.");
        } catch (IOException e) {
            System.err.println("Error writing Documents file: " + e.getMessage());
        }
    }
}