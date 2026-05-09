package org.example;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.stream.Stream;

public class WordCounter {

    // Testing/Demo fields
    private boolean DemoMode = false;
    private String FilePath = null;
    // Core fields
    private String collectionPath;

    private int docIdCounter = 1;

    private TextAnalyzer analyzer;
    private Indexer indexer;
    private DocumentParser parser;

    private int partID = 1;
    private double ramUsageBound = 0.01;
//    private int threshold = 10;
//    private int currentBatch = 0;

    // Constructor > Initializes the fields and loads the stop words
    public WordCounter(boolean DemoMode, String FilePath, String collectionPath) {
        this.DemoMode = DemoMode;
        this.FilePath = FilePath;
        this.collectionPath = collectionPath;

        this.analyzer = new TextAnalyzer();
        this.indexer = new Indexer();
        this.parser = new DocumentParser(this.analyzer, this.indexer);
    }

    // Optional parameter DemoMode
    public WordCounter(String collectionPath) {
        this.DemoMode = false;
        this.collectionPath = collectionPath;

        this.analyzer = new TextAnalyzer();
        this.indexer = new Indexer();
        this.parser = new DocumentParser(this.analyzer, this.indexer);
    }

    // Chooses between Single File Mode and full Collection Mode
    public void execute() {
        if (DemoMode) {
            System.out.println("Single File Mode (testing)...");
            File file = new File(FilePath);
            if (file.exists()) {
                parser.countWords(file, docIdCounter++, collectionPath);
                printVoc();
            } else {
                System.err.println("Error: File not found at " + FilePath);
            }
        } else {
            System.out.println("Collection Mode");
            processCollection(collectionPath);
            indexer.DocumentsFile();
        }
    }

    private void printVoc() {
        System.out.println("\n\n Vocabulary Output");
        System.out.println("Distinct word count: " + indexer.getVocab().size());

        for (Map.Entry<String, TermInfo> word : indexer.getVocab().entrySet()) {
            TermInfo info = word.getValue();
            System.out.print("Word: [" + word.getKey() + "] -> DF: " + info.getDF() + " | Tags: ");
            info.getFreqs().forEach((tag, count) -> System.out.print(tag + ": " + count + " | "));
            System.out.println();
        }
    }

    // Recursively traverses the collection directory and processes each file
    private void processCollection(String folderPath) {
        Path root = Paths.get(folderPath);

        if (!Files.exists(root)) {
            System.err.println("Error: Collection folder not found at " + folderPath);
            return;
        }

        // https://stackoverflow.com/questions/1844688/how-can-i-read-all-files-in-a-folder-from-java
        try (Stream<Path> paths = Files.walk(root)) {
            paths.filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".nxml"))
                    .forEach(path -> {
                        parser.countWords(path.toFile(), docIdCounter++, collectionPath);

                        // dynamic ram check
                        // https://stackoverflow.com/questions/12807797/java-get-available-memory

                        Runtime runtime = Runtime.getRuntime();
                        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
                        // Maximum Heap Size --> set to 6144 MiB
                        long maxMemory = runtime.maxMemory();
                        double memoryUsage = (double) usedMemory / maxMemory;

                        // if ram usage exceeds 80%, move to disk
                        if (memoryUsage >= ramUsageBound) {
//                            System.out.println("Memory at "
//                                    + (memoryUsage * 100) + "%. Saving partial index " + partID);

                            indexer.SavePartialIndex(partID++);
                            indexer.getVocab().clear();

                            // run garbage collector to reclaim the cleared memory
                            // https://stackoverflow.com/questions/1481178/how-to-force-garbage-collection-in-java
                            System.gc();
                        }
                });

            if (!indexer.getVocab().isEmpty())
            {
                indexer.SavePartialIndex(partID++);
            }

            indexer.MergePartialIndexes(partID - 1);
            indexer.DocumentsFile();

            System.out.println("Collection processing completed.");
        } catch (Exception e) {
            System.err.println("Error while traversing the folder path.");
        }
    }

    public static void main(String[] args) {
        // Sample file and collection paths
        String testFile = "dataset/MiniCollection/diagnosis/Topic_1/0/1852545.nxml";
        String collectionDir = "dataset/MiniCollection";
        String fullCollectionDir = "dataset/MedicalCollection";

        // Run for a SINGLE file (DemoMode = true)

        /*
         WordCounter counterSingle = new WordCounter(true, testFile, collectionDir);
         counterSingle.execute();
         */

        // Run the mini collection
        WordCounter counterAll = new WordCounter(collectionDir);

        // Uncomment the lines below to run for the Medical collection
//        WordCounter counterAll = new WordCounter(fullCollectionDir);
        long startTime = System.currentTimeMillis();
        try{
            counterAll.execute();
        }catch (Throwable t){
            t.printStackTrace();
        }finally {
            long endTime = System.currentTimeMillis();
            long totalTimeMs = endTime - startTime;

            long minutes = (totalTimeMs / 1000) / 60;
            long seconds = (totalTimeMs / 1000) % 60;
            long milliseconds = totalTimeMs % 1000;

            String formattedTime = String.format("%02d:%02d:%03d", minutes, seconds, milliseconds);

            System.out.println("Total Execution Time: " + formattedTime + " (minutes:second:millisecond)");
        }
    }
}