package com.simulator.ui;

import com.simulator.model.*;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Pos;
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

    public SimulationManager(List<GateView> allGateViews, TableView<boolean[]> truthTable) {
        this.allGateViews = allGateViews;
        this.truthTable = truthTable;
    }

    public void startClock() {
        clockTimer = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            // We do NOT manually call view.toggle() here anymore.
            // Instead, we pass true to signal that this update tick is driven by a clock event.
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
        evaluateSimulationEngine(false);
    }

    /**
     * Driven by the central master timer timeline.
     * Prepares and commits clock state transitions alongside the sequential network.
     */
    public void triggerSynchronousClockTick() {
        evaluateSimulationEngine(true);
    }

    /**
     * Unified Two-Phase Commit Engine execution routine.
     */
    private void evaluateSimulationEngine(boolean isClockTickEvent) {
        // PHASE 1: Prepare/Capture Phase
        // Prepares the state transitions based on conditions BEFORE states change.
        for (GateView view : allGateViews) {
            Gate model = view.getGateModel();
            if (isClockTickEvent && model instanceof ClockGate) {
                ((ClockGate) model).prepareToggle();
            }
            if (model instanceof DFlipFlop) {
                ((DFlipFlop) model).prepareNextState();
            }
        }

        // PHASE 2: Commit Phase
        // Applies all pending state transitions simultaneously to eliminate timing drift.
        for (GateView view : allGateViews) {
            Gate model = view.getGateModel();
            if (isClockTickEvent && model instanceof ClockGate) {
                ((ClockGate) model).commitToggle();
            }
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

            // Reset tracking flags for this sub-pass execution loop
            for (GateView view : allGateViews) {
                if (view.getGateModel() != null) {
                    view.getGateModel().resetEvaluation();
                }
            }

            // Cascade signals down the network path
            for (GateView view : allGateViews) {
                boolean previousValue = view.getCurrentEvaluatedValue();
                view.update();
                boolean currentVal = view.getCurrentEvaluatedValue();

                if (previousValue != currentVal) {
                    settled = false;
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

        // 1. Classify Components
        for (GateView view : allGateViews) {
            if (view.getGateModel() instanceof InputSwitch || view.getGateModel() instanceof ClockGate) {
                inputs.add(view);
            } else if (view.getGateModel() instanceof OutputProbe) {
                outputs.add(view);
            } else {
                // Treat intermediate logic gates as potential state variables if they are part of feedback loops
                stateFeedbackGates.add(view);
            }
        }

        if (inputs.isEmpty() || outputs.isEmpty()) return;

        // Limit state tracking to a reasonable number to avoid exponential table explosions (e.g., max 4 state lines)
        int maxTrackedStates = Math.min(stateFeedbackGates.size(), 3);
        List<GateView> trackedStates = stateFeedbackGates.subList(0, maxTrackedStates);

        int numInputs = inputs.size();
        int numStates = trackedStates.size();
        int totalStateColumns = numInputs + numStates;
        int numOutputs = outputs.size();

        // 2. Cache current live states to restore later
        Map<Gate, Boolean> originalSwitchStates = new HashMap<>();
        Map<GateView, Boolean> originalInternalStates = new HashMap<>();

        for (GateView inputView : inputs) {
            if (inputView.getGateModel() instanceof InputSwitch) {
                InputSwitch sw = (InputSwitch) inputView.getGateModel();
                originalSwitchStates.put(sw, sw.getOutput());
            }
        }
        for (GateView stateView : allGateViews) {
            originalInternalStates.put(stateView, stateView.getCurrentEvaluatedValue());
        }

        // 3. Create Columns
        // External Input Columns
        for (int i = 0; i < numInputs; i++) {
            final int colIndex = i;
            String colName = inputs.get(i).getLabelText().replace(": 0", "").replace(": 1", "");
            TableColumn<boolean[], Boolean> col = new TableColumn<>(colName + " (In)");
            col.setCellValueFactory(cellData -> new javafx.beans.property.SimpleBooleanProperty(cellData.getValue()[colIndex]));
            col.setCellFactory(c -> new TruthTableCell());
            truthTable.getColumns().add(col);
        }

        // Present State Columns Q(t)
        for (int i = 0; i < numStates; i++) {
            final int colIndex = i + numInputs;
            String colName = trackedStates.get(i).getLabelText();
            TableColumn<boolean[], Boolean> col = new TableColumn<>(colName + " (t)");
            col.setCellValueFactory(cellData -> new javafx.beans.property.SimpleBooleanProperty(cellData.getValue()[colIndex]));
            col.setCellFactory(c -> new TruthTableCell());
            truthTable.getColumns().add(col);
        }

        // Next State/Output Columns Q(t+1)
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

            // Assign Inputs for this row combination
            for (int j = 0; j < numInputs; j++) {
                boolean state = ((i >> (totalStateColumns - 1 - j)) & 1) == 1;
                Gate model = inputs.get(j).getGateModel();
                if (model instanceof InputSwitch) ((InputSwitch) model).setState(state);
                rowData[j] = state;
            }

            // Force Present State Q(t) for this row combination directly into internal gate memory registers
            for (int j = 0; j < numStates; j++) {
                boolean state = ((i >> (numStates - 1 - j)) & 1) == 1;
                Gate model = trackedStates.get(j).getGateModel();
                if (model instanceof AbstractGate) {
                    ((AbstractGate) model).forceStableState(state); // Requires an exposed method to seed values
                }
                rowData[j + numInputs] = state;
            }

            // Run multi-pass circuit convergence to get the resulting Next State transitions
            triggerCircuitUpdate();

            // Sample outputs/next states
            for (int k = 0; k < numOutputs; k++) {
                boolean state = ((OutputProbe) outputs.get(k).getGateModel()).getResult();
                rowData[k + totalStateColumns] = state;
            }
            allRowsData.add(rowData);
        }

        truthTable.setItems(javafx.collections.FXCollections.observableArrayList(allRowsData));

        // 5. Restore live runtime environment state
        for (Map.Entry<Gate, Boolean> entry : originalSwitchStates.entrySet()) {
            ((InputSwitch) entry.getKey()).setState(entry.getValue());
        }
        for (Map.Entry<GateView, Boolean> entry : originalInternalStates.entrySet()) {
            Gate model = entry.getKey().getGateModel();
            if (model instanceof AbstractGate) {
                ((AbstractGate) model).forceStableState(entry.getValue());
            }
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
