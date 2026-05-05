package org.utils;

import gr.uoc.csd.hy463.NXMLFileReader;

import java.io.File;
import java.util.Arrays;

/**
 * Utility class providing static methods for Information Retrieval calculations.
 * Source: Vector Space Model principles.
 */
public class Utilities
{

    /**
     * Calculates the Logarithmic Term Frequency (TF).
     * Since maxFreq is not available in documents.txt, we use the standard log formula.
     * Formula: tf = 1 + log2(freq)
     */
    public static double calculateTF(int freq) {
        if (freq <= 0) return 0.0;
        return 1 + (Math.log(freq) / Math.log(2));
    }

    /**
     * Calculates the Inverse Document Frequency (IDF).
     * Formula: idf = log2(N / df) + 0.1
     */
    public static double calculateIDF(int N, int df) {
        if (df <= 0) return 0.0;
        return Math.log((double) N / df) / Math.log(2) + 0.1;
    }

    /**
     * Calculates the weight of a term.
     * Formula: Weight = tf * idf
     */
    public static double calculateWeight(double tf, double idf) {
        return tf * idf;
    }

    /**
     * Calculates the Cosine Similarity between a document and a query.
     * Formula: dotProduct / (normD * normQ)
     */
    public static double calculateCosineSimilarity(double dotProduct, double normD, double normQ) {
        if (normD == 0 || normQ == 0) return 0.0;
        return dotProduct / (normD * normQ);
    }

    // https://www.baeldung.com/java-levenshtein-distance
    public static int costOfSubstitution(char a, char b) {
        return a == b ? 0 : 1;
    }

    public static int min(int a, int b, int c) {
        return Math.min(a, Math.min(b, c));
    }

    public static int editDistance(String x, String y) {
        int[][] dp = new int[x.length() + 1][y.length() + 1];

        for (int i = 0; i <= x.length(); i++) {
            for (int j = 0; j <= y.length(); j++) {
                if (i == 0) {
                    dp[i][j] = j;
                }
                else if (j == 0) {
                    dp[i][j] = i;
                }
                else {
                    dp[i][j] = min(
                            dp[i - 1][j - 1] + costOfSubstitution(x.charAt(i - 1), y.charAt(j - 1)),
                            dp[i - 1][j] + 1,
                            dp[i][j - 1] + 1
                    );
                }
            }
        }

        return dp[x.length()][y.length()];
    }

    public String getSnippet(String filePath, String query) {
        if (query == null || query.isEmpty()) return "Empty query";
        query = query.toLowerCase().trim();

        int index = -1;
        int matched = 0;

        try {
            File f = new File(filePath);
            if (!f.exists()) {
                System.out.println("File " + filePath + " does not exist");
                return "Could not load snippet.";
            }

            NXMLFileReader xmlFile = new NXMLFileReader(f);
            String content = xmlFile.getBody();

            if (content == null || content.trim().isEmpty()) {
                content = xmlFile.getAbstr();
            }

            if (content == null) {
                System.out.println("No content in XML file[" + filePath + "]");
                return "Could not load snippet.";
            }

            // normalize spaces to create a single-line text
            content = content.replaceAll("\\s+", " ");
            String lowerContent = content.toLowerCase();

            if (!query.isEmpty()) {
                index = lowerContent.indexOf(query);
                if (index != -1) {
                    matched = query.length();
                }
            }

            if (index == -1) {
                String[] queryWords = query.split("\\s+");
                for (String word : queryWords) {

                    if (word.length() <= 2) continue;

                    index = lowerContent.indexOf(word);
                    if (index != -1) {
                        matched = word.length();
                        break;
                    }
                }
            }

            if (index != -1) {
                // 50 chars before and 50 chars after the match
                int start = Math.max(0, index - 50);
                int end = Math.min(content.length(), index + matched + 50);
                return content.substring(start, end).trim() + "...";
            } else {
                return "Not exact match.";
            }

        } catch (Exception e) {
            return "Could not load snippet.";
        }
    }
}