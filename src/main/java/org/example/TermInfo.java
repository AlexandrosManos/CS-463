package org.example;

import java.util.*;


class TermInfo {
    private Map<String, Integer> freqs;
    // Document Frequency
    private int df;

    public TermInfo() {
        this.freqs = new HashMap<>();
        this.df = 0;
    }
    public void incrementDF() {
        this.df++;
    }
    public int getDF() {
        return df;
    }
    // add οccurrence
    public void Occurs(String tagName) {
        freqs.put(tagName, freqs.getOrDefault(tagName, 0) + 1);
    }

    public Map<String, Integer> getFreqs() {
        return freqs;
    }

    public int totalFreqs() {
        return freqs.values().stream().mapToInt(Integer::intValue).sum();
    }
}