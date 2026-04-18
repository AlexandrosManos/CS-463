package org.example;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import mitos.stemmer.Stemmer;

public class TextAnalyzer {

    // Core fields
    private Set<String> stopWords;

    // Optimization test
    // Pre-compiled patterns
    // https://www.baeldung.com/java-regex-performance
    // If we want to remove only the punctuation_mark
    private static final Pattern punctuation_marks = Pattern.compile("\\p{Punct}");
    private static final Pattern words_pattern = Pattern.compile("[^a-zα-ωίϊΐόάέύϋΰήώ0-9\\s]");
    // Pattern to identify and remove XML entities like &#x02013; or &amp;
    private static final Pattern xml_entity = Pattern.compile("&[#a-z0-9]+;");

    public TextAnalyzer() {
        this.stopWords = new HashSet<>();
        loadStopWords();
        Stemmer.Initialize();
    }

    public List<String> analyze(String content) {
        // Already checked in DocumentParser, but we can keep it here for safety if this
        // method is used elsewhere
        // if (content == null || content.trim().isEmpty())
        // return new ArrayList<>();
        List<String> validTokens = new ArrayList<>();
        // String cleanText = removePunctuation(content);
        String cleanText = content.toLowerCase();
        cleanText = xml_entity.matcher(cleanText).replaceAll(" ");
        cleanText = words_pattern.matcher(cleanText).replaceAll(" ");
        String[] tokens = cleanText.split("\\s+");

        for (String token : tokens) {
            if (!token.isEmpty() && !stopWords.contains(token)) {
                String stmTok = Stemmer.Stem(token);
                validTokens.add(stmTok);
            }
        }
        return validTokens;
    }

    /**
     * Clean up the text, convert to lowercase and replace punctuation with spaces
     *
     * @param s
     * @return
     */
    static String removePunctuation(String s) {
        // https://stackoverflow.com/questions/6255329/php-and-regexp-to-accept-only-greek-characters-in-form
        String delimReg = "[^a-zα-ωίϊΐόάέύϋΰήώ0-9\\s]";
        String raw = s.toLowerCase().replaceAll(delimReg, " ");
        raw = raw.replaceAll(delimReg, " ");
        // s.replaceAll("\\p{Punct}",""); // removes punctuation
        // String sOut = Normalizer.normalize(s, Normalizer.Form.NFD); // this will
        // separate all of the accent marks from the characters.
        // sOut = sOut.replaceAll("\\p{M}", ""); // exlcudes accents for all unicode
        return raw;
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

}