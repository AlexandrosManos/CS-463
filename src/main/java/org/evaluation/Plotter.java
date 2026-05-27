package org.evaluation;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

public class Plotter {

    // Reads TSV evaluation results and returns Data for the Plot class
    private static Plot.Data loadDataForPlot(String filePath, int metricColumnIndex) {
        List<Double> x = new ArrayList<>();
        List<Double> y = new ArrayList<>();

        File file = new File(filePath);
        if (!file.exists()) {
            System.err.println("File not found: " + filePath);
            return Plot.data();
        }

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split("\t");
                if (parts.length >= 4) {
                    double topicId = Double.parseDouble(parts[0]);
                    double metricValue = Double.parseDouble(parts[metricColumnIndex]);

                    x.add(topicId);
                    y.add(metricValue);
                }
            }
        } catch (Exception e) {
            System.err.println("Error reading " + filePath + ": " + e.getMessage());
        }

        return Plot.data().xy(x, y);
    }

    // Creates a comparison chart
    public static void createComparisonChart(String summaryFile, String descFile,
                                             int metricIndex, String metricName,
                                             String outputFileName) {


        Plot.Data summary = loadDataForPlot(summaryFile, metricIndex);
        Plot.Data description = loadDataForPlot(descFile, metricIndex);

        Plot.PlotOptions opts = Plot.plotOpts()
                .title(metricName + " Comparison: Summary vs Description")
                .width(900)
                .height(500)
                .padding(20)
                .bgColor(Color.WHITE)
                .legend(Plot.LegendFormat.BOTTOM)
                .grids(30, 10);

        Plot plot = Plot.plot(opts);

        plot.xAxis("Topic ID", Plot.axisOpts()
                .range(1, 30)
                .format(Plot.AxisFormat.NUMBER_INT));

        plot.yAxis("Score", Plot.axisOpts()
                .range(0.0, 1.0));

        plot.series("Summary Query", summary, Plot.seriesOpts()
                .color(Color.BLUE)
                .line(Plot.Line.SOLID)
                .lineWidth(2)
                .marker(Plot.Marker.CIRCLE)
                .markerSize(8));

        plot.series("Description Query", description, Plot.seriesOpts()
                .color(new Color(220, 20, 60))
                .line(Plot.Line.SOLID)
                .lineWidth(2)
                .marker(Plot.Marker.SQUARE)
                .markerSize(8));

        try {
            plot.save("results/" + outputFileName, "png");
            System.out.println("Saved plot to: results/" + outputFileName + ".png");
        } catch (Exception e) {
            System.err.println("Failed to save plot: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        String summaryResults = "results/eval_results_summary.txt";
        String descResults = "results/eval_results_description.txt";

        // 1 = Bpref, 2 = AveP, 3 = NDCG
        createComparisonChart(summaryResults, descResults, 1, "Bpref", "chart_bpref_comparison");
        createComparisonChart(summaryResults, descResults, 2, "AveP'", "chart_avep_comparison");
        createComparisonChart(summaryResults, descResults, 3, "NDCG'", "chart_ndcg_comparison");

        System.out.println("All plots generated successfully!");
    }
}