package org.example;

import gr.uoc.csd.hy463.NXMLFileReader;
import mitos.stemmer.Stemmer;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class WordCounter {

    // Testing/Demo fields
    private boolean DemoMode = false;;
    private String FilePath = null;
    // Core fields
    private String collectionPath;
    private Set<String> stopWords;

    // Optimization test
    // Pre-compiled patterns
    // https://www.baeldung.com/java-regex-performance
    // If we want to remove only the punctuation_mark
    private static final Pattern punctuation_marks = Pattern.compile("\\p{Punct}");
    private static final Pattern words_pattern = Pattern.compile("[^a-zα-ωίϊΐόάέύϋΰήώ0-9\\s]");
    // Pattern to identify and remove XML entities like &#x02013; or &amp;
    private static final Pattern xml_entity = Pattern.compile("&[#a-z0-9]+;");

    // Global Vocabulary (Upgraded for B3/B4)
    private TreeMap<String, TermInfo> vocab;

    // Constructor > Initializes the fields and loads the stop words
    public WordCounter(boolean DemoMode, String FilePath, String collectionPath) {
        this.DemoMode = DemoMode;
        this.FilePath = FilePath;
        this.collectionPath = collectionPath;
        this.stopWords = new HashSet<>();
        this.vocab = new TreeMap<>();
        loadStopWords();
        Stemmer.Initialize();
    }
    // Optional parameter DemoMode
    public WordCounter(String collectionPath) {
        this.collectionPath = collectionPath;
        this.stopWords = new HashSet<>();
        this.vocab = new TreeMap<>();
        loadStopWords();
        Stemmer.Initialize();
    }

    //Chooses between Single File Mode and full Collection Mode
    public void execute() {
        if (DemoMode) {
            System.out.println("Single File Mode (testing)...");
            File file = new File(FilePath);
            if (file.exists()) {
                countWords(file);
                printVoc();
            } else {
                System.err.println("Error: File not found at " + FilePath);
            }
        } else {
            System.out.println("Collection Mode");
            processCollection(collectionPath);
            VocabularyFile();
        }
    }

    // Processes a single NXML file and prints the count of distinct words
    private void countWords(File nxmlFile) {
        Set<String> tempVoc = new HashSet<>();
        try {
            NXMLFileReader xmlFile = new NXMLFileReader(nxmlFile);

            // Extract content from required tags
            processTag(xmlFile.getTitle(), "Title", tempVoc);
            processTag(xmlFile.getAbstr(), "Abstract", tempVoc);
            processTag(xmlFile.getBody(), "Body", tempVoc);
            processTag(xmlFile.getJournal(), "Journal", tempVoc);
            processTag(xmlFile.getPublisher(), "Publisher", tempVoc);

            if (xmlFile.getAuthors() != null) {
                for (String author : xmlFile.getAuthors()) {
                    processTag(author, "Authors", tempVoc);
                }
            }
            if (xmlFile.getCategories() != null) {
                for (String category : xmlFile.getCategories()) {
                    processTag(category, "Categories", tempVoc);
                }
            }
            for (String word : tempVoc) {
                vocab.get(word).incrementDF();
            }
        } catch (Exception e) {
            System.err.println("Error parsing NXML: " + e.getMessage());
        }
    }

    private void processTag(String content, String tagName, Set<String> fileVoc) {
        if (content == null || content.trim().isEmpty()) return;

        //String cleanText = removePunctuation(content);
        String cleanText = content.toLowerCase();
        cleanText = xml_entity.matcher(cleanText).replaceAll(" ");
        cleanText = words_pattern.matcher(cleanText).replaceAll(" ");
        String[] tokens = cleanText.split("\\s+");

        for (String token : tokens) {
            if (!token.isEmpty() && !stopWords.contains(token)) {
                String stmTok = Stemmer.Stem(token);
                //System.out.println(token +"->" + Stemmer.Stem(token));
                // If term doesn't exist, create a new TermInfo object
                //if (!vocab.containsKey(token)) {
                    //TermInfo newFolder = new TermInfo();
                    //vocab.put(token, newFolder);
                //}
                // Goated function
                // --> B1
                // vocab.putIfAbsent(token, new TermInfo());
                // vocab.get(token).Occurs(tagName);

                vocab.putIfAbsent(stmTok, new TermInfo());
                // Update the frequency for this specific tag
                vocab.get(stmTok).Occurs(tagName);
                fileVoc.add(stmTok);

            }
        }
    }
    private void printVoc() {
        System.out.println("============== Vocabulary Output ===============");
        System.out.println("Distinct word count: " + vocab.size());

        for (Map.Entry<String, TermInfo> word : vocab.entrySet()) {
            TermInfo info = word.getValue();
            System.out.print("Word: [" + word.getKey() + "] -> DF: " + info.getDF() + " | Tags: ");
            info.getFreqs().forEach((tag, count) -> System.out.print(tag + ": " + count + " | "));
            System.out.println();
        }
    }
    /**
     * Clean up the text, convert to lowercase and replace punctuation with spaces
     *
     * @param s
     * @return
     */
    static String removePunctuation(String s) {
        //https://stackoverflow.com/questions/6255329/php-and-regexp-to-accept-only-greek-characters-in-form
        String delimReg = "[^a-zα-ωίϊΐόάέύϋΰήώ0-9\\s]";
        String raw = s.toLowerCase().replaceAll(delimReg, " ");
        raw = raw.replaceAll(delimReg, " ");
        //s.replaceAll("\\p{Punct}",""); // removes punctuation
        //String sOut = Normalizer.normalize(s, Normalizer.Form.NFD); // this will separate all of the accent marks from the characters.
        //sOut = sOut.replaceAll("\\p{M}", ""); // exlcudes accents for all unicode
        return raw;
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
                    .forEach(path -> countWords(path.toFile()));

            System.out.println("Collection processing completed.");
        } catch (Exception e) {
            System.err.println("Error while traversing the folder path.");
        }
    }

    // Loads the stop words into memory
    private void loadStopWords() {
        EditStopWords("/stopwords/stopwordsEn.txt");
        EditStopWords("/stopwords/stopwordsGr.txt");
    }

    // Reads a stop words file from the project's resources folder
    private void EditStopWords(String resourcePath) {
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is != null) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (!line.trim().isEmpty()) {
                            // Remove space and convert the text to lowercase
                            stopWords.add(line.trim().toLowerCase());
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error reading resource: " + resourcePath);
        }
    }
    // Create directory and write vocabulary file
    private void VocabularyFile() {
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
        // https://www.datacamp.com/doc/java/create-&-write-files
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(vocabFile))) {

            // Header
            writer.write("Term\tDocumentFrequency\tTermFrequency");
            writer.newLine();

            // Terms, df, tf
            for (Map.Entry<String, TermInfo> entry : vocab.entrySet()) {
                String term = entry.getKey();
                TermInfo info = entry.getValue();

                writer.write(term + "\t" + info.getDF() + "\t" + info.totalFreqs());
                writer.newLine();
            }
            System.out.println("VocabularyFile.txt file saved.");

        } catch (java.io.IOException e) {
            System.err.println("Error writing Vocabulary file: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        // Sample file and collection paths
        String testFile = "dataset/MiniCollection/diagnosis/Topic_1/0/1852545.nxml";
        String collectionDir = "dataset/MiniCollection";

        // Run for a SINGLE file (DemoMode = true)
//        WordCounter counterSingle = new WordCounter(true, testFile, collectionDir);
//        counterSingle.execute();

        // Uncomment the lines below to run for the ENTIRE collection --> flag
         WordCounter counterAll = new WordCounter(collectionDir);
         counterAll.execute();
    }
}
