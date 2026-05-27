package com.simulator.ui;

import javafx.application.Platform;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.layout.VBox;
import java.util.*;

public class WaveformMonitor extends VBox {
    private final LineChart<Number, Number> lineChart;
    private final Map<String, XYChart.Series<Number, Number>> activeTracks = new HashMap<>();
    private int timeStep = 0;
    private static final int MAX_VISIBLE_POINTS = 30;

    public WaveformMonitor() {
        final NumberAxis xAxis = new NumberAxis();
        final NumberAxis yAxis = new NumberAxis(-0.2, 1.2, 1.0);

        xAxis.setLabel("Simulation Ticks");
        xAxis.setAutoRanging(true);
        yAxis.setLabel("Logic State");
        yAxis.setTickLabelFormatter(new NumberAxis.DefaultFormatter(yAxis) {
            @Override public String toString(Number object) {
                return object.doubleValue() >= 0.9 ? "1" : "0";
            }
        });

        lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setTitle("Live Oscilloscope Logic Timeline");
        lineChart.setCreateSymbols(false); // Clean trace line rendering
        lineChart.setPrefHeight(200);

        this.getChildren().add(lineChart);
    }

    /**
     * Samples the state of components in the active environment workspace.
     */
    public void recordSample(List<GateView> views) {
        Platform.runLater(() -> {
            timeStep++;
            for (GateView view : views) {
                // Monitor Probes, Clocks, or Custom Labeled Items
                if (view.getGateModel() instanceof com.simulator.model.OutputProbe ||
                        view.getGateModel() instanceof com.simulator.model.ClockGate ||
                        (view.getCustomLabel() != null && !view.getCustomLabel().isEmpty())) {

                    String traceName = view.getLabelText();
                    XYChart.Series<Number, Number> series = activeTracks.get(traceName);

                    if (series == null) {
                        series = new XYChart.Series<>();
                        series.setName(traceName);
                        lineChart.getData().add(series);
                        activeTracks.put(traceName, series);
                    }

                    boolean state = view.getCurrentEvaluatedValue();
                    double numericState = state ? 1.0 : 0.0;

                    // Insert a previous value step point to preserve clean square edge transitions
                    if (!series.getData().isEmpty()) {
                        double prevVal = series.getData().get(series.getData().size() - 1).getYValue().doubleValue();
                        if (prevVal != numericState) {
                            series.getData().add(new XYChart.Data<>(timeStep - 0.01, prevVal));
                        }
                    }

                    series.getData().add(new XYChart.Data<>(timeStep, numericState));

                    // Keep sliding viewing window length bound to prevent memory leak
                    if (series.getData().size() > MAX_VISIBLE_POINTS * 2) {
                        series.getData().remove(0, 2);
                    }
                }
            }
        });
    }

    public void clear() {
        lineChart.getData().clear();
        activeTracks.clear();
        timeStep = 0;
    }
}