package org.utils;

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


}