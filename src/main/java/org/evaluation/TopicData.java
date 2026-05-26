package org.evaluation;

import java.util.ArrayList;
import java.util.Collections;
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

    // Filter out unjudged documents
    public List<Record> getResults() {
        List<Record> resList = new ArrayList<>();
        for (Record res : results) {
            if (getRelevance(res.id) != -1) {
                resList.add(res);
            }
        }
        return resList;
    }


    public double AveP() {
        // the total number of relevant documents
        int R = 0;

        for (Rels q : qrels) {
            if (q.relevance >= 1) R++;
        }

        // no relevant documents --> AveP = 0
        if (R == 0) return 0.0;

        List<Record> resList = getResults();

        double sumPrecision = 0.0;
        int relevantFoundSoFar = 0;

        for (int i = 0; i < resList.size(); i++) {
            int rel = getRelevance(resList.get(i).id);

            if (rel >= 1) {
                relevantFoundSoFar++;
                double precisionAtK = (double) relevantFoundSoFar / (i + 1);
                sumPrecision += precisionAtK;
            }
        }

        return sumPrecision / R;
    }

    public double NDCG() {

        List<Record> resList = getResults();

        if (resList.isEmpty()) return 0.0;

        double dcg = 0.0;
        // // dg(r) = g(r) for r <= 2, and dg(r) = g(r) / log2(r) for r > 2
        for (int i = 0; i < resList.size(); i++) {
            int rel = getRelevance(resList.get(i).id);
            int r = i + 1;

            if (r <= 2) {
                dcg += rel;
            } else {
                dcg += rel / (Math.log(r) / Math.log(2));
            }
        }

        List<Integer> idealRelevances = new ArrayList<>();
        for (Rels q : qrels) {
            if (q.relevance > 0) {
                idealRelevances.add(q.relevance);
            }
        }
        idealRelevances.sort(Collections.reverseOrder());

        if (idealRelevances.isEmpty() || dcg == 0.0) return 0.0;

        double idcg = 0.0;
        for (int i = 0; i < idealRelevances.size(); i++) {
            int rank = i + 1;
            int rel = idealRelevances.get(i);

            if (rank <= 2) {
                idcg += rel;
            } else {
                idcg += rel / (Math.log(rank) / Math.log(2));
            }
        }

        return dcg / idcg;
    }

}