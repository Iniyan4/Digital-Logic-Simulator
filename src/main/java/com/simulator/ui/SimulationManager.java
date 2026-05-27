package com.simulator.ui;

import com.simulator.model.*;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SimulationManager {

    private final List<GateView> allGateViews;
    private Timeline clockTimer;
    private final TableView<boolean[]> truthTable;
    private final javafx.scene.layout.Pane rootPane;
    private double currentFrequencyHz = 1.0;
    private WaveformMonitor waveformMonitor;

    public void setClockFrequency(double hz) {
        this.currentFrequencyHz = hz;
        if (clockTimer != null && clockTimer.getStatus() == Animation.Status.RUNNING) {
            clockTimer.stop();
            startClock(); // Restart with new speed definitions
        }
    }

    public SimulationManager(List<GateView> allGateViews, TableView<boolean[]> truthTable, javafx.scene.layout.Pane rootPane, WaveformMonitor monitor) {
        this.allGateViews = allGateViews;
        this.truthTable = truthTable;
        this.rootPane = rootPane;
        this.waveformMonitor = monitor;
    }

    public void startClock() {
        // Calculate period: 1 / frequency
        double periodSeconds = 1.0 / currentFrequencyHz;

        clockTimer = new Timeline(new KeyFrame(Duration.seconds(periodSeconds), event -> {
            triggerSynchronousClockTick();
        }));
        clockTimer.setCycleCount(Animation.INDEFINITE);
        clockTimer.play();
    }

    /**
     * Call this when an interactive UI component updates (like clicking a Switch).
     * Clocks remain unchanged, but the D-FF state pipeline handles signal propagation.
     */
    public void triggerCircuitUpdate() {
        evaluateSimulationEngine();
    }

    public void triggerSynchronousClockTick() {
        // FIX: Pre-commit clock changes before evaluating the rest of the circuit.
        // This allows edge-detectors (like D-FFs) to see the rising edge during Phase 1.
        for (GateView view : allGateViews) {
            Gate model = view.getGateModel();
            if (model instanceof ClockGate) {
                ClockGate cg = (ClockGate) model;
                cg.prepareToggle();
                cg.commitToggle();
                view.update(); // Update the UI color immediately
            }
        }
        evaluateSimulationEngine();
        if (waveformMonitor != null) {
            waveformMonitor.recordSample(allGateViews);
        }
    }

    /**
     * Unified Two-Phase Commit Engine execution routine.
     */
    private void evaluateSimulationEngine() { // <-- Removed the boolean parameter
        // PHASE 1: Prepare/Capture Phase
        for (GateView view : allGateViews) {
            Gate model = view.getGateModel();
            if (model instanceof DFlipFlop) {
                ((DFlipFlop) model).prepareNextState();
            }
        }

        // PHASE 2: Commit Phase
        for (GateView view : allGateViews) {
            Gate model = view.getGateModel();
            if (model instanceof DFlipFlop) {
                ((DFlipFlop) model).commitState();
            }
        }

        // PHASE 3: Combinational Settling Pass
        boolean settled = false;
        int passes = 0;
        int maxPasses = 64;

        while (!settled && passes < maxPasses) {
            settled = true;
            passes++;

            for (GateView view : allGateViews) {
                if (view.getGateModel() != null) {
                    view.getGateModel().resetEvaluation();
                }
            }

            for (GateView view : allGateViews) {
                boolean previousValue = view.getCurrentEvaluatedValue();
                view.update();
                boolean currentVal = view.getCurrentEvaluatedValue();

                if (previousValue != currentVal) {
                    settled = false;
                }
            }

            for (Node node : rootPane.getChildren()) {
                if (node instanceof Wire) {
                    ((Wire) node).updateSignalColor();
                }
            }
        }
    }

//    public void triggerCircuitUpdate() {
//        // PHASE 1: Capture Phase (Evaluate clock conditions and queue up state transformations)
//        for (GateView view : allGateViews) {
//            if (view.getGateModel() instanceof DFlipFlop) {
//                ((DFlipFlop) view.getGateModel()).prepareNextState();
//            }
//        }
//
//        // PHASE 2: Commit Phase (Simultaneously apply changes to avoid timing skew)
//        for (GateView view : allGateViews) {
//            if (view.getGateModel() instanceof DFlipFlop) {
//                ((DFlipFlop) view.getGateModel()).commitState();
//            }
//        }
//
//        // PHASE 3: Combinational Settling Pass (Multi-pass loop for feedback propagation)
//        boolean settled = false;
//        int passes = 0;
//        int maxPasses = 64;
//
//        while (!settled && passes < maxPasses) {
//            settled = true;
//            passes++;
//
//            // Reset execution flags for this pass iteration
//            for (GateView view : allGateViews) {
//                if (view.getGateModel() != null) {
//                    view.getGateModel().resetEvaluation();
//                }
//            }
//
//            // Propagate signals across the canvas network
//            for (GateView view : allGateViews) {
//                boolean previousValue = view.getCurrentEvaluatedValue();
//                view.update();
//                boolean currentVal = view.getCurrentEvaluatedValue();
//
//                if (previousValue != currentVal) {
//                    settled = false; // Keep tracking until values stabilize
//                }
//            }
//        }
//    }

    /**
     * Clears the truth table UI.
     */
    public void clearTruthTable() {
        truthTable.getColumns().clear();
        truthTable.getItems().clear();
    }

    public void generateTruthTable() {
        clearTruthTable();

        List<GateView> inputs = new ArrayList<>();
        List<GateView> outputs = new ArrayList<>();
        List<GateView> stateFeedbackGates = new ArrayList<>();

        // 1. Classify Components (Only track explicit DFlipFlops as sequential states)
        for (GateView view : allGateViews) {
            if (view.getGateModel() instanceof InputSwitch || view.getGateModel() instanceof ClockGate) {
                inputs.add(view);
            } else if (view.getGateModel() instanceof OutputProbe) {
                outputs.add(view);
            } else if (view.getGateModel() instanceof DFlipFlop) {
                stateFeedbackGates.add(view);
            }
        }

        if (inputs.isEmpty() || outputs.isEmpty()) return;

        int maxTrackedStates = Math.min(stateFeedbackGates.size(), 3);
        List<GateView> trackedStates = stateFeedbackGates.subList(0, maxTrackedStates);

        int numInputs = inputs.size();
        int numStates = trackedStates.size();
        int totalStateColumns = numInputs + numStates;
        int numOutputs = outputs.size();

        // 2. Cache current live states deeply
        Map<InputSwitch, Boolean> originalSwitchStates = new HashMap<>();
        Map<AbstractGate, Boolean> originalCombinationalStates = new HashMap<>();
        Map<DFlipFlop, Object> originalDFFStates = new HashMap<>();

        for (GateView inputView : inputs) {
            if (inputView.getGateModel() instanceof InputSwitch) {
                InputSwitch sw = (InputSwitch) inputView.getGateModel();
                originalSwitchStates.put(sw, sw.getOutput());
            }
        }

        for (GateView view : allGateViews) {
            Gate model = view.getGateModel();
            if (model instanceof DFlipFlop) {
                originalDFFStates.put((DFlipFlop) model, ((DFlipFlop) model).captureStateSnapshot());
            } else if (model instanceof AbstractGate) {
                originalCombinationalStates.put((AbstractGate) model, view.getCurrentEvaluatedValue());
            }
        }

        // 3. Create Columns
        for (int i = 0; i < numInputs; i++) {
            final int colIndex = i;
            String colName = inputs.get(i).getLabelText().replace(": 0", "").replace(": 1", "");
            TableColumn<boolean[], Boolean> col = new TableColumn<>(colName + " (In)");
            col.setCellValueFactory(cellData -> new javafx.beans.property.SimpleBooleanProperty(cellData.getValue()[colIndex]));
            col.setCellFactory(c -> new TruthTableCell());
            truthTable.getColumns().add(col);
        }

        for (int i = 0; i < numStates; i++) {
            final int colIndex = i + numInputs;
            String colName = trackedStates.get(i).getLabelText();
            TableColumn<boolean[], Boolean> col = new TableColumn<>(colName + " (t)");
            col.setCellValueFactory(cellData -> new javafx.beans.property.SimpleBooleanProperty(cellData.getValue()[colIndex]));
            col.setCellFactory(c -> new TruthTableCell());
            truthTable.getColumns().add(col);
        }

        for (int i = 0; i < numOutputs; i++) {
            final int colIndex = i + totalStateColumns;
            String colName = outputs.get(i).getLabelText();
            TableColumn<boolean[], Boolean> col = new TableColumn<>(colName + " (t+1)");
            col.setCellValueFactory(cellData -> new javafx.beans.property.SimpleBooleanProperty(cellData.getValue()[colIndex]));
            col.setCellFactory(c -> new TruthTableCell());
            truthTable.getColumns().add(col);
        }

        // 4. Generate State Permutation Rows
        int numRows = (int) Math.pow(2, totalStateColumns);
        List<boolean[]> allRowsData = new ArrayList<>();

        for (int i = 0; i < numRows; i++) {
            boolean[] rowData = new boolean[totalStateColumns + numOutputs];

            // Assign Inputs
            for (int j = 0; j < numInputs; j++) {
                boolean state = ((i >> (totalStateColumns - 1 - j)) & 1) == 1;
                Gate model = inputs.get(j).getGateModel();
                if (model instanceof InputSwitch) ((InputSwitch) model).setState(state);
                rowData[j] = state;
            }

            // Seeding Present States directly into registers
            for (int j = 0; j < numStates; j++) {
                boolean state = ((i >> (numStates - 1 - j)) & 1) == 1;
                DFlipFlop dff = (DFlipFlop) trackedStates.get(j).getGateModel();

                // Force the internal state registers to match the truth table permutation row
                dff.restoreStateSnapshot(new boolean[]{false, false, state, state, state});
                rowData[j + numInputs] = state;
            }

            // Evaluate the permutation state changes
            triggerCircuitUpdate();

            // Sample outputs
            for (int k = 0; k < numOutputs; k++) {
                boolean state = ((OutputProbe) outputs.get(k).getGateModel()).getResult();
                rowData[k + totalStateColumns] = state;
            }
            allRowsData.add(rowData);
        }

        truthTable.setItems(javafx.collections.FXCollections.observableArrayList(allRowsData));

        // 5. Clean Environment Restoration Pass
        for (Map.Entry<InputSwitch, Boolean> entry : originalSwitchStates.entrySet()) {
            entry.getKey().setState(entry.getValue());
        }
        for (Map.Entry<AbstractGate, Boolean> entry : originalCombinationalStates.entrySet()) {
            entry.getKey().forceStableState(entry.getValue());
        }
        for (Map.Entry<DFlipFlop, Object> entry : originalDFFStates.entrySet()) {
            entry.getKey().restoreStateSnapshot(entry.getValue());
        }

        triggerCircuitUpdate();
    }

    private static class TruthTableCell extends TableCell<boolean[], Boolean> {
        @Override
        protected void updateItem(Boolean item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) setText(null);
            else {
                setText(item ? "1" : "0");
                setAlignment(Pos.CENTER);
            }
        }
    }
}
