package org.example;

import java.io.*;
import java.util.*;

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

    public void SavePartialIndex(int partID)
    {
        File tempDirectory = new File("CollectionIndex/temp/part" + partID);
        if (!tempDirectory.exists()) tempDirectory.mkdirs();

        File vocabularyFile = new File(tempDirectory, "VocabularyFile.txt");
        File postingFile = new File(tempDirectory, "PostingFile.bin");

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(vocabularyFile));
             RandomAccessFile raf = new RandomAccessFile(postingFile, "rw"))
        {
            for (Map.Entry<String, TermInfo> entry : vocab.entrySet())
            {
                String term = entry.getKey();
                TermInfo info = entry.getValue();
                long offset = raf.getFilePointer();

                for (Map.Entry<Integer, Posting> pEntry : info.GetPostings().entrySet()) {
                    Posting p = pEntry.getValue();
                    raf.writeInt(p.documentID);
                    raf.writeInt(p.GetTF());
                    int last = 0;
                    for (int pos : p.positions)
                    {
                        raf.writeInt(pos - last); // Delta Encoding
                        last = pos;
                    }
                }
                writer.write(term + "\t" + info.getDF() + "\t" + info.totalFreqs() + "\t" + offset);
                writer.newLine();
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void MergePartialIndexes(int totalParts)
    {
        if (totalParts <= 0)
            return;

        System.out.println("Starting K-Way Merge for " + totalParts + " parts...");
        File finalDirectory = new File("CollectionIndex");
        File finalVocabulary = new File(finalDirectory, "VocabularyFile.txt");
        File finalPostings = new File(finalDirectory, "PostingFile.bin");

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(finalVocabulary));
             RandomAccessFile finalRaf = new RandomAccessFile(finalPostings, "rw")) {

            writer.write("Term\tDocumentFrequency\tTermFrequency\tOffset");
            writer.newLine();

            //Open all partial vocabularies and posting files
            Scanner[] vocabScanners = new Scanner[totalParts];
            RandomAccessFile[] postingFiles = new RandomAccessFile[totalParts];
            String[] currentTerms = new String[totalParts];

            for (int i = 0; i < totalParts; i++) {
                File partDirectory = new File("CollectionIndex/temp/part" + (i + 1));
                vocabScanners[i] = new Scanner(new File(partDirectory, "VocabularyFile.txt"));
                postingFiles[i] = new RandomAccessFile(new File(partDirectory, "PostingFile.bin"), "r");
                if (vocabScanners[i].hasNextLine()) currentTerms[i] = vocabScanners[i].nextLine(); // Skip header if exists or read 1st
            }

            //K-Way Merge Loop
            while (true) {
                String minTerm = null;
                // Find the alphabetically smallest term among all currentTerms
                for (String termLine : currentTerms) {
                    if (termLine == null) continue;
                    String term = termLine.split("\t")[0];
                    if (minTerm == null || term.compareTo(minTerm) < 0) minTerm = term;
                }

                if (minTerm == null) break; // All files processed

                long finalOffset = finalRaf.getFilePointer();
                int totalDF = 0;
                int totalTF = 0;

                //Collect and merge postings for minTerm from all parts that have it
                for (int i = 0; i < totalParts; i++) {
                    if (currentTerms[i] != null && currentTerms[i].startsWith(minTerm + "\t")) {
                        String[] parts = currentTerms[i].split("\t");
                        int df = Integer.parseInt(parts[1]);
                        int tf = Integer.parseInt(parts[2]);
                        long offset = Long.parseLong(parts[3]);

                        totalDF += df;
                        totalTF += tf;

                        //Read from partial posting file and write to final
                        postingFiles[i].seek(offset);
                        //For each document in this partial posting
                        for (int d = 0; d < df; d++) {
                            int docID = postingFiles[i].readInt();
                            int docTF = postingFiles[i].readInt();
                            finalRaf.writeInt(docID);
                            finalRaf.writeInt(docTF);
                            for (int p = 0; p < docTF; p++) {
                                finalRaf.writeInt(postingFiles[i].readInt()); // Positions (already delta-encoded)
                            }
                        }

                        //Move to next line for this scanner
                        currentTerms[i] = vocabScanners[i].hasNextLine() ? vocabScanners[i].nextLine() : null;
                    }
                }

                //Write to final Vocabulary
                writer.write(minTerm + "\t" + totalDF + "\t" + totalTF + "\t" + finalOffset);
                writer.newLine();
            }

            //Close everything
            for (int i = 0; i < totalParts; i++) {
                vocabScanners[i].close();
                postingFiles[i].close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}