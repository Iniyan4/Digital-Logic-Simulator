package com.simulator.ui;

import com.simulator.model.*;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Effect;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.scene.text.Font;

import java.util.Optional;
import java.util.UUID;

/**
 * A polymorphic, draggable, interactive view for any gate or probe.
 * It supports custom labels and uses the built-in Node ID.
 */
public class GateView extends Pane {

    private final Gate gateModel;
    private final SimulatorApp app;
    private final Shape body;
    private final Label label;
    private double offsetX;
    private double offsetY;

    public static final double GATE_WIDTH = 80;
    public static final double GATE_HEIGHT = 50;
    public static final double PROBE_RADIUS = 25;

    // Fields for Labels and Connections
    private final String gateType;
    private final String baseLabel; // e.g., "AND", "SWITCH"
    private String customLabel = ""; // e.g., "A", "Sum"

    private ConnectionPin pinOut = null;
    private ConnectionPin pinInA = null;
    private ConnectionPin pinInB = null;

    private static final Effect selectedEffect = new DropShadow(20, Color.DODGERBLUE);

    /**
     * Constructor for creating NEW gates from the palette.
     */
    public GateView(Gate gateModel, String baseLabel, SimulatorApp app, String gateType) {
        this(gateModel, baseLabel, app, gateType, UUID.randomUUID().toString());
    }

    /**
     * Master constructor, used for both new and loaded gates.
     */
    public GateView(Gate gateModel, String baseLabel, SimulatorApp app, String gateType, String id) {
        this.gateModel = gateModel;
        this.app = app;
        this.gateType = gateType;
        this.baseLabel = baseLabel;
        this.setId(id);

        this.label = new Label();
        this.label.setFont(new Font("Arial", 14));
        this.label.setTextFill(Color.BLACK);

        if (gateModel instanceof OutputProbe) {
            this.body = new Circle(PROBE_RADIUS);
            this.body.setFill(Color.DARKRED);
            this.body.setStroke(Color.BLACK);
            this.body.setStrokeWidth(2);
            this.body.setLayoutX(PROBE_RADIUS);
            this.body.setLayoutY(PROBE_RADIUS);
            this.label.setMinWidth(PROBE_RADIUS * 2);
            this.label.setMinHeight(PROBE_RADIUS * 2);
            this.label.setAlignment(javafx.geometry.Pos.CENTER);
            this.label.setLayoutX(0);
            this.label.setLayoutY(0);

        } else {
            this.body = new Rectangle(GATE_WIDTH, GATE_HEIGHT);
            this.body.setFill(Color.WHITE);
            this.body.setStroke(Color.BLACK);
            this.body.setStrokeWidth(2);
            this.body.setLayoutX(0);
            this.body.setLayoutY(0);
            this.label.setMinWidth(GATE_WIDTH);
            this.label.setMinHeight(GATE_HEIGHT);
            this.label.setAlignment(javafx.geometry.Pos.CENTER);
            this.label.setLayoutX(0);
            this.label.setLayoutY(0);
        }

        this.getChildren().addAll(body, label);
        createPins();
        setupEventHandlers();
        update();
    }

    /**
     * Updates the UI state (color and label) based on the model.
     */
    public void update() {

        if (gateModel != null) {
            this.currentEvaluatedValue = gateModel.getOutput();
        }

        if (gateModel instanceof OutputProbe) {
            OutputProbe probeModel = (OutputProbe) gateModel;
            boolean result = probeModel.getResult();
            if (result) {
                body.setFill(Color.LIME);
            } else {
                body.setFill(Color.DARKRED);
            }
        } else if (gateModel instanceof InputSwitch) {
            InputSwitch switchModel = (InputSwitch) gateModel;
            boolean state = switchModel.getOutput();
            if (state) {
                ((Rectangle) body).setFill(Color.LIGHTYELLOW);
            } else {
                ((Rectangle) body).setFill(Color.WHITE);
            }
        } else if (gateModel instanceof ClockGate) {
            ClockGate clockModel = (ClockGate) gateModel;
            boolean state = clockModel.getOutput();
            if (state) {
                ((Rectangle) body).setFill(Color.LIGHTCYAN); // Use a different color for clocks
            } else {
                ((Rectangle) body).setFill(Color.WHITE);
            }
        }

        updateLabelText();
    }

    /**
     * Combines the custom label, base label, and state into the UI label.
     */
    private void updateLabelText() {
        String stateText = "";
        if (gateModel instanceof InputSwitch) {
            stateText = ((InputSwitch) gateModel).getOutput() ? ": 1" : ": 0";
        }
        else if (gateModel instanceof ClockGate) {
            stateText = ((ClockGate) gateModel).getOutput() ? ": 1" : ": 0";
        }

        if (customLabel == null || customLabel.isEmpty()) {
            label.setText(baseLabel + stateText);
        } else {
            String fullBaseLabel = baseLabel;
            if (gateModel instanceof InputSwitch || gateModel instanceof ClockGate) {
                fullBaseLabel += stateText;
            }
            label.setText(customLabel + " (" + fullBaseLabel + ")");
        }
    }

    /**
     * Creates and positions the connection pins.
     */
    private void createPins() {
        if (!(gateModel instanceof OutputProbe)) {
            this.pinOut = new ConnectionPin(ConnectionPin.PinType.OUTPUT, this, app);
            double xPos = (body instanceof Circle) ? PROBE_RADIUS * 2 : GATE_WIDTH;
            double yPos = (body instanceof Circle) ? PROBE_RADIUS : GATE_HEIGHT / 2;
            pinOut.setLayoutX(xPos);
            pinOut.setLayoutY(yPos);
            this.getChildren().add(pinOut);
        }

        double yCenter = (body instanceof Circle) ? PROBE_RADIUS : GATE_HEIGHT / 2;

        if (gateModel instanceof BinaryGate) {
            this.pinInA = new ConnectionPin(ConnectionPin.PinType.INPUT, this, app);
            this.pinInB = new ConnectionPin(ConnectionPin.PinType.INPUT, this, app);
            pinInA.setLayoutX(0);
            pinInA.setLayoutY(yCenter - (GATE_HEIGHT / 4));
            pinInB.setLayoutX(0);
            pinInB.setLayoutY(yCenter + (GATE_HEIGHT / 4));
            this.getChildren().addAll(pinInA, pinInB);
        } else if (gateModel instanceof UnaryGate) {
            this.pinInA = new ConnectionPin(ConnectionPin.PinType.INPUT, this, app);
            pinInA.setLayoutX(0);
            pinInA.setLayoutY(yCenter);
            this.getChildren().add(pinInA);
        }
        // (InputSwitch and ClockGate fall through and correctly get 0 input pins)
    }

    /**
     * Sets up all mouse handlers for dragging, clicking, and double-clicking.
     */
    private void setupEventHandlers() {
        //Drag Handlers
        this.setOnMousePressed(event -> {
            offsetX = event.getSceneX() - this.getLayoutX();
            offsetY = event.getSceneY() - this.getLayoutY();
            this.toFront();
            event.consume();
        });
        this.setOnMouseDragged(event -> {
            this.setLayoutX(event.getSceneX() - offsetX);
            this.setLayoutY(event.getSceneY() - offsetY);
            event.consume();
        });

        //Cursor Handlers
        this.setOnMouseEntered(event -> {
            if (!event.isPrimaryButtonDown()) {
                if (gateModel instanceof InputSwitch) {
                    this.getScene().setCursor(Cursor.HAND);
                } else {
                    this.getScene().setCursor(Cursor.MOVE);
                }
            }
        });
        this.setOnMouseExited(event -> {
            if (!event.isPrimaryButtonDown()) {
                this.getScene().setCursor(Cursor.DEFAULT);
            }
        });

        //Click Handler
        this.setOnMouseClicked(event -> {
            // Check for double-click FIRST
            if (event.getClickCount() == 2) {
                showLabelEditDialog();
                event.consume();
                return;
            }

            // 1. Handle Selection
            app.toggleSelectItem(this, event.isShiftDown());
            event.consume();

            // 2. Handle Toggling (if it's a switch)
            if (gateModel instanceof InputSwitch) {
                InputSwitch switchModel = (InputSwitch) gateModel;
                boolean newState = !switchModel.getOutput();
                switchModel.setState(newState);
                app.triggerCircuitUpdate();
            }
        });
    }

    /**
     * Shows a dialog to edit the custom label.
     */
    private void showLabelEditDialog() {
        TextInputDialog dialog = new TextInputDialog(this.customLabel);
        dialog.setTitle("Edit Label");
        dialog.setHeaderText("Enter a custom label for this component:");
        dialog.setContentText("Label:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(name -> {
            this.setCustomLabel(name.trim());
        });
    }

    //Selection Methods
    public void select() {
        body.setEffect(selectedEffect);
    }
    public void deselect() {
        body.setEffect(null);
    }

    //Getters and Setters

    /**
     * Gets the display label (custom or base) for the Truth Table.
     */
    public String getLabelText() {
        if (customLabel != null && !customLabel.isEmpty()) {
            return customLabel;
        }
        return baseLabel;
    }

    public Gate getGateModel() {
        return gateModel;
    }

    public String getCustomLabel() {
        return customLabel;
    }

    public void setCustomLabel(String customLabel) {
        this.customLabel = customLabel;
        updateLabelText(); // Refresh the UI immediately
    }

    // Add this field to the top of GateView class
    private boolean currentEvaluatedValue = false;

    // Add this getter method
    public boolean getCurrentEvaluatedValue() {
        return this.currentEvaluatedValue;
    }

    public String getGateType() {
        return gateType;
    }

    public ConnectionPin getPinOut() {
        return pinOut;
    }

    public ConnectionPin getPinInA() {
        return pinInA;
    }

    public ConnectionPin getPinInB() {
        return pinInB;
    }
}