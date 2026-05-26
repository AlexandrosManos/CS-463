package org.evaluation;

import java.io.*;

public class IRQualityEvaluator {

    // topic number 1-30
    // zero is not used
    private TopicData[] topics = new TopicData[31];

    private static boolean debug = true;

    public IRQualityEvaluator() {
        for (int i = 1; i <= 30; i++) {
            topics[i] = new TopicData(i);
        }
    }

    public TopicData[] getTopics() {
        return topics;
    }

    public void loadResults(String resultsPath) {
        File file = new File(resultsPath);
        if (!file.exists()) {
            System.err.println("File not found: " + resultsPath);
            return;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split("\\s+");

                if (parts.length < 5) continue;

                int topicId = Integer.parseInt(parts[0]);
                String id = parts[2];
                int rank = Integer.parseInt(parts[3]);
                double score = Double.parseDouble(parts[4]);

                if (topicId >= 1 && topicId <= 30) {
                    topics[topicId].results.add(new Record(id, rank, score));
                }else {
                    System.err.println("Invalid topic id " + topicId);
                }
            }
            if (debug)
                System.out.println("Results loaded successfully from: " + resultsPath);
        } catch (Exception e) {
            System.err.println("Error reading results: " + e.getMessage());
        }
    }

    public void loadRels(String qrelsPath) {
        File file = new File(qrelsPath);
        if (!file.exists()) {
            System.err.println("File not found: " + qrelsPath);
            return;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split("\\s+");
                if (parts.length < 4) continue;

                int topicId = Integer.parseInt(parts[0]);
                String id = parts[2];
                int relevance = Integer.parseInt(parts[3]);

                if (topicId >= 1 && topicId <= 30) {
                    topics[topicId].qrels.add(new Rels(id, relevance));
                }
            }
            if (debug)
                System.out.println("Qrels loaded successfully from: " + qrelsPath);
        } catch (Exception e) {
            System.err.println("Error reading qrels: " + e.getMessage());
        }
    }

    public void fileExporter(String outputPath) {
        File file = new File(outputPath);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {

            for (int i = 1; i <= 30; i++) {
                TopicData topic = topics[i];

                if (topic.qrels.isEmpty()) {
                    continue;
                }

                double bpref = topic.Bpref();

                double avep = topic.AveP();
                double ndcg = topic.NDCG();

                String line = String.format(java.util.Locale.US, "%d\t%.4f\t%.4f\t%.4f",
                        topic.topicId, bpref, avep, ndcg);

                writer.write(line);
                writer.newLine();
            }

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    public static void main(String[] args) {

        IRQualityEvaluator evalSummary = new IRQualityEvaluator();
        IRQualityEvaluator evalDesc = new IRQualityEvaluator();

        // Optionally - maybe remove it later
        String resultPath = "results/";
        String summaryFile = "results_summary.txt";
        String descFile = "results_description.txt";
        String qrelsFile = "dataset/qrels.txt";

        System.out.println("Evaluating Summary Results");
        evalSummary.loadRels(qrelsFile);
        evalSummary.loadResults(resultPath+summaryFile);
        evalSummary.fileExporter(resultPath+"eval_"+summaryFile);
        System.out.println("\n");

        System.out.println("Evaluating Description Results");
        evalDesc.loadRels(qrelsFile);
        evalDesc.loadResults(resultPath+descFile);
        evalDesc.fileExporter(resultPath+"eval_"+descFile);


    }
}