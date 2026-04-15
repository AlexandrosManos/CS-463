package org.example;

import gr.uoc.csd.hy463.NXMLFileReader;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
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
    private boolean DemoMode;
    private String FilePath;
    // Core fields
    private String collectionPath;
    private Set<String> stopWords;

    // Optimization test
    // Pre-compiling patterns and reuse a Matcher
    // https://www.baeldung.com/java-regex-performance
    // If we want to remove only the punctuation_mark
    private static final Pattern punctuation_mark = Pattern.compile("\\p{Punct}");
    private static final Pattern words_pattern = Pattern.compile("[^a-zα-ωίϊΐόάέύϋΰήώ0-9\\s]");
    // Pattern to identify and remove XML entities like &#x02013; or &amp;
    private static final Pattern xml_entity = Pattern.compile("&[#a-z0-9]+;");

    // Constructor > Initializes the fields and loads the stop words
    public WordCounter(boolean DemoMode, String FilePath, String collectionPath) {
        this.DemoMode = DemoMode;
        this.FilePath = FilePath;
        this.collectionPath = collectionPath;
        this.stopWords = new HashSet<>();
        loadStopWords();
    }
    // Optional parameter DemoMode
    public WordCounter(String collectionPath) {
        this.DemoMode = false;
        this.FilePath = null;
        this.collectionPath = collectionPath;
        this.stopWords = new HashSet<>();
        loadStopWords();
    }

    //Chooses between Single File Mode and full Collection Mode
    public void execute() {
        if (DemoMode) {
            System.out.println("Single File Mode (testing)...");
            File file = new File(FilePath);
            if (file.exists()) {
                countWords(file);
            } else {
                System.err.println("Error: File not found at " + FilePath);
            }
        } else {
            System.out.println("Collection Mode");
            processCollection(collectionPath);
        }
    }

    // Processes a single NXML file and prints the count of distinct words
    private void countWords(File nxmlFile) {
        // TreeMap ensures lexicographical order for the Vocabulary
        TreeMap<String, TermInfo> vocab = new TreeMap<>();
        System.out.println("==============Count Words=======================");

        try {
            NXMLFileReader xmlFile = new NXMLFileReader(nxmlFile);

            // Extract content from required tags
            processTag(xmlFile.getTitle(), "Title", vocab);
            processTag(xmlFile.getAbstr(), "Abstract", vocab);
            processTag(xmlFile.getBody(), "Body", vocab);
            processTag(xmlFile.getJournal(), "Journal", vocab);
            processTag(xmlFile.getPublisher(), "Publisher", vocab);

            if (xmlFile.getAuthors() != null) {
                for (String author : xmlFile.getAuthors()) {
                    processTag(author, "Authors", vocab);
                }
            }
            if (xmlFile.getCategories() != null) {
                for (String category : xmlFile.getCategories()) {
                    processTag(category, "Categories", vocab);
                }
            }

            // Output the results
            System.out.println("File: " + nxmlFile.getName());
            System.out.println("Distinct word count: " + vocab.size());
            System.out.println("================================================");

            for (Map.Entry<String, TermInfo> word : vocab.entrySet()) {
                System.out.print("Word: [" + word.getKey() + "] -> ");
                Map<String, Integer> freqs = word.getValue().getFreqs();
                freqs.forEach((tag, count) -> System.out.print(tag + ": " + count + " | "));
                System.out.println();
            }

        } catch (Exception e) {
            System.err.println("Error parsing NXML: " + e.getMessage());
        }
    }

    private void processTag(String content, String tagName, TreeMap<String, TermInfo> vocab) {
        if (content == null || content.trim().isEmpty()) return;

        //String cleanText = removePunctuation(content);
        String cleanText = content.toLowerCase();
        cleanText = xml_entity.matcher(cleanText).replaceAll(" ");
        cleanText = words_pattern.matcher(cleanText).replaceAll(" ");
        String[] tokens = cleanText.split("\\s+");

        for (String token : tokens) {
            if (!token.isEmpty() && !stopWords.contains(token)) {
                // If term doesn't exist, create a new TermInfo object
                //if (!vocab.containsKey(token)) {
                    //TermInfo newFolder = new TermInfo();
                    //vocab.put(token, newFolder);
                //}
                // Goated function
                vocab.putIfAbsent(token, new TermInfo());
                // Update the frequency for this specific tag
                vocab.get(token).Occurs(tagName);
            }
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

    public static void main(String[] args) {
        // Sample file and collection paths
        String testFile = "dataset/MiniCollection/diagnosis/Topic_1/0/1852545.nxml";
        String collectionDir = "dataset/MiniCollection";

        // Run for a SINGLE file (DemoMode = true)
        WordCounter counterSingle = new WordCounter(true, testFile, collectionDir);
        counterSingle.execute();

        // Uncomment the lines below to run for the ENTIRE collection --> flag
//         WordCounter counterAll = new WordCounter(collectionDir);
//         counterAll.execute();
    }
}

// Processes a single NXML file and prints the count of distinct words
//    private void countWords(File nxmlFile) {
//        Set<String> distinctWords = new HashSet<>();
//        // Instead of using String (immutable), Use StringBuilder (mutable)
//        StringBuilder fullText = new StringBuilder();
//        System.out.println("==============Count Words=======================");
//        try {
//            NXMLFileReader xmlFile = new NXMLFileReader(nxmlFile);
//
//            // Collect text from all relevant tags
//            if (xmlFile.getPMCID() != null) fullText.append(xmlFile.getPMCID()).append(" ");
//            if (xmlFile.getTitle() != null) fullText.append(xmlFile.getTitle()).append(" ");
//            if (xmlFile.getAbstr() != null) fullText.append(xmlFile.getAbstr()).append(" ");
//            if (xmlFile.getBody() != null) fullText.append(xmlFile.getBody()).append(" ");
//            if (xmlFile.getJournal() != null) fullText.append(xmlFile.getJournal()).append(" ");
//            if (xmlFile.getPublisher() != null) fullText.append(xmlFile.getPublisher()).append(" ");
//
//            if (xmlFile.getAuthors() != null) {
//                for (String author : xmlFile.getAuthors()) fullText.append(author).append(" ");
//            }
//            if (xmlFile.getCategories() != null) {
//                for (String category : xmlFile.getCategories()) fullText.append(category).append(" ");
//            }
//
//            // Clean up the text
//            String rawText = removePunctuation(fullText);
//
//            // Split into tokens based on whitespace
//            String[] tokens = rawText.split("\\s+");
//
//            // Add to the HashSet (ignoring empty strings and stop words)
//            for (String token : tokens) {
////                System.out.println(token);
//                if (!token.isEmpty() && !stopWords.contains(token)) {
//                    distinctWords.add(token);
//                }
//            }
//
//            // Output the results
//            System.out.println("File: " + nxmlFile.getName());
//            System.out.println("Distinct word count: " + distinctWords.size());
//            System.out.println("================================================");
//
//        } catch (Exception e) {
//            System.err.println("Error processing file: " + nxmlFile.getName());
//        }
//        /**
//         * Clean up the text, convert to lowercase and replace punctuation with spaces
//         *
//         * @param s
//         * @return
//         */
//        static String removePunctuation(StringBuilder s) {
//            String raw = s.toString().toLowerCase();
//            //https://stackoverflow.com/questions/6255329/php-and-regexp-to-accept-only-greek-characters-in-form
//            String delimReg = "[^a-zα-ωίϊΐόάέύϋΰήώ0-9\\s]";
//            raw = raw.replaceAll(delimReg, " ");
//            //s.replaceAll("\\p{Punct}",""); // removes punctuation
//            //String sOut = Normalizer.normalize(s, Normalizer.Form.NFD); // this will separate all of the accent marks from the characters.
//            //sOut = sOut.replaceAll("\\p{M}", ""); // exlcudes accents for all unicode
//            return raw;
//        }
