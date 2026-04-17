package org.example;

import java.util.*;


class TermInfo {
    private Map<String, Integer> freqs;
    // Document Frequency
    private int df;

    private Map<Integer, Posting> postings;

    public TermInfo() {
        this.postings = new HashMap<>();
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
    public void AddOccurrence(int docID, int position)
    {

        postings.putIfAbsent(docID, new Posting(docID));

        postings.get(docID).AddPosition(position);
    }
    public void Occurs(String tagName) {
        freqs.put(tagName, freqs.getOrDefault(tagName, 0) + 1);
    }

    public Map<String, Integer> getFreqs() {
        return freqs;
    }

    public int totalFreqs() {
        return freqs.values().stream().mapToInt(Integer::intValue).sum();
    }

    public Map<Integer, Posting> GetPostings() {
        return postings;
    }
}