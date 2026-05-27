package com.simulator.ui;

import com.simulator.data.CustomGateTemplate;
import com.simulator.data.GateSaveData;
import com.simulator.data.TemplateManager;
import com.simulator.data.WireSaveData;
import com.simulator.model.*;
import javafx.scene.Node;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Line;

import java.util.*;

public class WorkspaceManager {
    private final Pane rootPane;
    private final SimulatorApp app;
    private final List<GateView> allGateViews;
    private final List<Node> selectedItems = new ArrayList<>();
    private final TemplateManager templateManager;

    // Wiring state
    private boolean isWiring = false;
    private ConnectionPin startPin = null;
    private Line tempWire = null;

    public WorkspaceManager(Pane rootPane, SimulatorApp app, List<GateView> allGateViews, TemplateManager templateManager) {
        this.rootPane = rootPane;
        this.app = app;
        this.allGateViews = allGateViews;
        this.templateManager = templateManager;

        setupHandlers();
    }

    private void setupHandlers() {
        // Drag Over
        rootPane.setOnDragOver(event -> {
            if (event.getGestureSource() instanceof PaletteIcon && event.getDragboard().hasString()) {
                event.acceptTransferModes(TransferMode.COPY);
            }
            event.consume();
        });

        // Drag Dropped
        rootPane.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasString()) {
                createGate(db.getString(), event.getX(), event.getY());
                success = true;
            }
            event.setDropCompleted(success);
            event.consume();
        });

        // Wiring Preview
        rootPane.setOnMouseMoved(event -> {
            if (isWiring && tempWire != null) {
                tempWire.setEndX(event.getX());
                tempWire.setEndY(event.getY());
            }
        });

        // Deselect All on background click
        rootPane.setOnMouseClicked(event -> deselectAll());
    }

    //Gate Creation

    public GateView createGate(String gateType, double x, double y) {
        if (gateType.startsWith("CUSTOM:")) {
            createCustomGateFromTemplate(gateType.substring("CUSTOM:".length()), x, y);
            return null;
        }
        double newX = x - (GateView.GATE_WIDTH / 2.0);
        double newY = y - (GateView.GATE_HEIGHT / 2.0);
        return createGate(gateType, newX, newY, UUID.randomUUID().toString());
    }

    public GateView createGate(String gateType, double x, double y, String id) {
        Gate model;
        String baseLabel;

        switch (gateType) {
            case "SWITCH": model = new InputSwitch(); baseLabel = "SWITCH"; break;
            case "CLOCK_GATE": model = new ClockGate(); baseLabel = "CLOCK"; break;
            case "PROBE": model = new OutputProbe(); baseLabel = "PROBE"; break;
            case "AND_GATE": model = new AndGate(); baseLabel = "AND"; break;
            case "OR_GATE": model = new OrGate(); baseLabel = "OR"; break;
            case "NOT_GATE": model = new NotGate(); baseLabel = "NOT"; break;
            case "NAND_GATE": model = new NandGate(); baseLabel = "NAND"; break;
            case "NOR_GATE": model = new NorGate(); baseLabel = "NOR"; break;
            case "XOR_GATE": model = new XorGate(); baseLabel = "XOR"; break;
            case "XNOR_GATE": model = new XnorGate(); baseLabel = "XNOR"; break;
            case "D_FLIP_FLOP": model = new DFlipFlop(); baseLabel = "D-FF"; break;
            default: return null;
        }

        GateView view = new GateView(model, baseLabel, app, gateType, id);
        view.setLayoutX(x);
        view.setLayoutY(y);
        rootPane.getChildren().add(view);
        allGateViews.add(view);
        return view;
    }

    private void createCustomGateFromTemplate(String gateName, double dropX, double dropY) {
        CustomGateTemplate template = templateManager.getTemplate(gateName);
        if (template == null) return;

        List<GateView> newGates = new ArrayList<>();
        List<Wire> newWires = new ArrayList<>();
        Map<String, GateView> oldIdMap = new HashMap<>();

        double baseX = Double.MAX_VALUE, baseY = Double.MAX_VALUE;
        for (GateSaveData d : template.getInternalGates()) {
            if (d.getX() < baseX) baseX = d.getX();
            if (d.getY() < baseY) baseY = d.getY();
        }

        for (GateSaveData d : template.getInternalGates()) {
            GateView newGate = createGate(d.getGateType(), dropX + (d.getX() - baseX), dropY + (d.getY() - baseY), UUID.randomUUID().toString());
            if (d.getCustomLabel() != null) newGate.setCustomLabel(d.getCustomLabel());
            newGates.add(newGate);
            oldIdMap.put(d.getId(), newGate);
        }

        for (WireSaveData d : template.getInternalWires()) {
            GateView sView = oldIdMap.get(d.getStartGateId());
            GateView eView = oldIdMap.get(d.getEndGateId());
            if (sView != null && eView != null) {
                ConnectionPin sPin = sView.getPinOut();
                ConnectionPin ePin = (d.getEndPinType().equals("B")) ? eView.getPinInB() : eView.getPinInA();
                if (sPin != null && ePin != null) {
                    Wire w = new Wire(sPin, ePin);
                    w.setOnMouseClicked(e -> { toggleSelectItem(w, e.isShiftDown()); e.consume(); });
                    rootPane.getChildren().add(w);
                    newWires.add(w);

                    Gate inGate = eView.getGateModel();
                    if (inGate instanceof UnaryGate) ((UnaryGate)inGate).setInput(sView.getGateModel());
                    else if (inGate instanceof BinaryGate) {
                        if(d.getEndPinType().equals("B")) ((BinaryGate)inGate).setInputB(sView.getGateModel());
                        else ((BinaryGate)inGate).setInputA(sView.getGateModel());
                    }
                }
            }
        }
        deselectAll();
        newGates.forEach(this::selectItem);
        newWires.forEach(this::selectItem);
    }

    //Wiring

    public void onPinClicked(ConnectionPin clickedPin) {
        if (!isWiring) {
            if (clickedPin.getPinType() == ConnectionPin.PinType.OUTPUT) {
                isWiring = true;
                startPin = clickedPin;
                tempWire = new Line();
                tempWire.setStrokeWidth(3);
                tempWire.setMouseTransparent(true);
                tempWire.startXProperty().bind(startPin.layoutXProperty().add(startPin.getParentGateView().layoutXProperty()));
                tempWire.startYProperty().bind(startPin.layoutYProperty().add(startPin.getParentGateView().layoutYProperty()));
                tempWire.setEndX(tempWire.getStartX());
                tempWire.setEndY(tempWire.getStartY());
                rootPane.getChildren().add(tempWire);
            }
        } else {
            if (clickedPin.getPinType() == ConnectionPin.PinType.INPUT && clickedPin.getParentGateView() != startPin.getParentGateView()) {
                Wire wire = new Wire(startPin, clickedPin);
                wire.setOnMouseClicked(event -> { toggleSelectItem(wire, event.isShiftDown()); event.consume(); });
                rootPane.getChildren().add(wire);

                Gate outputGate = startPin.getParentGateView().getGateModel();
                GateView inputView = clickedPin.getParentGateView();
                Gate inputGate = inputView.getGateModel();

                if (inputGate instanceof UnaryGate) ((UnaryGate) inputGate).setInput(outputGate);
                else if (inputGate instanceof BinaryGate) {
                    if (clickedPin == inputView.getPinInA()) ((BinaryGate) inputGate).setInputA(outputGate);
                    else if (clickedPin == inputView.getPinInB()) ((BinaryGate) inputGate).setInputB(outputGate);
                }
                app.triggerCircuitUpdate();
            }
            isWiring = false;
            rootPane.getChildren().remove(tempWire);
            tempWire = null;
            startPin = null;
        }
    }

    //Selection

    public void toggleSelectItem(Node item, boolean isShiftDown) {
        if (isShiftDown) {
            if (selectedItems.contains(item)) deselectItem(item);
            else selectItem(item);
        } else {
            deselectAll();
            selectItem(item);
        }
    }

    private void selectItem(Node item) {
        if (selectedItems.contains(item)) return;
        selectedItems.add(item);
        if (item instanceof GateView) ((GateView) item).select();
        else if (item instanceof Wire) ((Wire) item).select();
    }

    private void deselectItem(Node item) {
        if (!selectedItems.contains(item)) return;
        selectedItems.remove(item);
        if (item instanceof GateView) ((GateView) item).deselect();
        else if (item instanceof Wire) ((Wire) item).deselect();
    }

    public void deselectAll() {
        new ArrayList<>(selectedItems).forEach(this::deselectItem);
        selectedItems.clear();
    }

    public List<Node> getSelectedItems() {
        return selectedItems;
    }

    public void removeSelectedItems() {
        new ArrayList<>(selectedItems).forEach(item -> {
            if (item instanceof GateView) removeGateView((GateView) item);
            else if (item instanceof Wire) removeWire((Wire) item);
        });
        selectedItems.clear();
    }

    private void removeWire(Wire wire) {
        ConnectionPin inputPin = wire.getEndPin();
        Gate inputGate = inputPin.getParentGateView().getGateModel();
        GateView inputView = inputPin.getParentGateView();

        if (inputGate instanceof UnaryGate) ((UnaryGate) inputGate).setInput(null);
        else if (inputGate instanceof BinaryGate) {
            if (inputPin == inputView.getPinInA()) ((BinaryGate) inputGate).setInputA(null);
            else if (inputPin == inputView.getPinInB()) ((BinaryGate) inputGate).setInputB(null);
        }

        rootPane.getChildren().remove(wire);
        app.triggerCircuitUpdate();
    }

    private void removeGateView(GateView gateView) {
        List<Wire> wiresToRemove = new ArrayList<>();
        for (Node child : rootPane.getChildren()) {
            if (child instanceof Wire) {
                Wire wire = (Wire) child;
                if (wire.getStartPin().getParentGateView() == gateView || wire.getEndPin().getParentGateView() == gateView) {
                    wiresToRemove.add(wire);
                }
            }
        }
        wiresToRemove.forEach(this::removeWire);
        rootPane.getChildren().remove(gateView);
        allGateViews.remove(gateView);
    }

    public void clear() {
        rootPane.getChildren().clear();
        allGateViews.clear();
        selectedItems.clear();
        isWiring = false;
        tempWire = null;
        startPin = null;
    }
}