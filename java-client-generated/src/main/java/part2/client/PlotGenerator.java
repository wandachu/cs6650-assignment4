package part2.client;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import javax.swing.*;
import java.awt.*;
import java.io.FileReader;
import java.io.IOException;

import static part2.client.api.MultithreadApi.outputFileName;

public class PlotGenerator extends JFrame {
    public PlotGenerator() {
        initUI();
    }

    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            var ex = new PlotGenerator();
            ex.setVisible(true);
        });
    }

    private void initUI() {
        XYDataset dataset = createDataset();
        JFreeChart chart = createChart(dataset);

        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        chartPanel.setBackground(Color.WHITE);
        add(chartPanel);

        pack();
        setTitle("Plot of Throughput");
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    private JFreeChart createChart(XYDataset dataset) {
        JFreeChart chart = ChartFactory.createXYLineChart(
                "Throughput of GET/POST Requests (Go Server)",
                "Time (second)",
                "Throughput per second",
                dataset,
                PlotOrientation.VERTICAL,
                true,
                true,
                false
        );

        XYPlot plot = chart.getXYPlot();

        var renderer = new XYLineAndShapeRenderer();
        renderer.setSeriesPaint(0, Color.RED);
        renderer.setSeriesStroke(0, new BasicStroke(2.0f));

        plot.setRenderer(renderer);
        plot.setBackgroundPaint(Color.white);

        plot.setRangeGridlinesVisible(true);
        plot.setRangeGridlinePaint(Color.BLACK);

        plot.setDomainGridlinesVisible(true);
        plot.setDomainGridlinePaint(Color.BLACK);

        chart.getLegend().setFrame(BlockBorder.NONE);

        chart.setTitle(new TextTitle("Throughput of GET/POST Requests (Go Server)",
                        new Font("Serif", java.awt.Font.BOLD, 18)
                )
        );

        return chart;
    }

    private XYDataset createDataset() {
        var series = new XYSeries("Throughput");
        series.add(0, 0);

        long startingTimeInMillis = 0;
        long cutoffTimeInMillis = 0;

        try (FileReader reader = new FileReader(outputFileName);
             CSVParser csvParser = CSVFormat.DEFAULT.parse(reader)) {
            int countOfRequests = 0;
            for (CSVRecord csvRecord : csvParser) {
                long requestSentTimestamp = Long.parseLong(csvRecord.get(0));
                long requestLatency = Long.parseLong(csvRecord.get(2));
                if (startingTimeInMillis == 0) {
                    startingTimeInMillis = requestSentTimestamp;
                    cutoffTimeInMillis = startingTimeInMillis + 1000;
                }
                if (requestSentTimestamp + requestLatency <= cutoffTimeInMillis) { // belongs to this data point
                    countOfRequests++;
                } else { // need to start a new data point
                    // Add the previous data point to the dataset
                    series.add((double) (cutoffTimeInMillis - startingTimeInMillis) / 1000, countOfRequests);
                    // Reset
                    cutoffTimeInMillis += 1000;
                    countOfRequests = 1; // include the current item
                }

            }
            // add the last data point
            series.add((double) (cutoffTimeInMillis - startingTimeInMillis) / 1000, countOfRequests);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        var dataset = new XYSeriesCollection();
        dataset.addSeries(series);

        return dataset;
    }
}
