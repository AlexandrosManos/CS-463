package org.evaluation;

import java.util.ArrayList;
import java.util.List;

public class TopicData {
    public int topicId;
    public List<Record> results;
    public List<Rels> qrels;

    public TopicData(int topicId) {
        this.topicId = topicId;
        this.results = new ArrayList<>();
        this.qrels = new ArrayList<>();
    }


    public int getRelevance(String id) {
        for (Rels q : qrels) {
            if (q.id.equals(id)) {
                return q.relevance;
            }
        }
        // If the document was never evaluated
        return -1;
    }


    public double Bpref() {

        // R is the number of judged relevant documents
        int R = 0;

        // N is the number of judged irrelevant documents
        int N = 0;
        for (Rels q : qrels) {
            if (q.relevance >= 1) {
                R++;
            } else if (q.relevance == 0) {
                N++;
            }
        }

        // avoid 0/0
        if (R == 0) return 0.0;

        double bprefSum = 0.0;
        int nonRel = 0;
        double denom = Math.min(R, N);

        for (Record res : results) {
            int rel = getRelevance(res.id);

            if (rel == 0) {
                nonRel++;
            } else if (rel >= 1) {
                if(denom == 0.0) {
                    bprefSum += 1.0;
                }else{

                    bprefSum += (1.0 - Math.min(nonRel, denom) / denom);
                }
            }
        }

        return bprefSum / R;
    }
}