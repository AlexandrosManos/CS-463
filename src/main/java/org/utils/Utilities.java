package org.utils;

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
     * Formula: idf = log2(N / df)
     */
    public static double calculateIDF(int N, int df) {
        if (df <= 0) return 0.0;
        return Math.log((double) N / df) / Math.log(2);
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
}