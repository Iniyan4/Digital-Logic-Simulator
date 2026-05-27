package com.simulator.ui;

import com.simulator.model.Gate;
import javafx.scene.paint.Color;
import javafx.scene.shape.CubicCurve;

/**
 * A visual representation of a wire connecting two ConnectionPins.
 * It's a CubicCurve that binds its start and end points to the pins.
 */
public class Wire extends CubicCurve {

    private final ConnectionPin startPin;
    private final ConnectionPin endPin;
    private final Gate inputGateModel;

    public Wire(ConnectionPin startPin, ConnectionPin endPin) {
        this.startPin = startPin;
        this.endPin = endPin;
        this.inputGateModel = endPin.getParentGateView().getGateModel();

        // Style the wire
        setStroke(Color.BLACK);
        setStrokeWidth(3);
        setFill(null); // No fill, just the line

        //Bind the Start Point
        //We bind the wire's startX to the pin's layoutX + its parent's layoutX
        startXProperty().bind(
                startPin.layoutXProperty().add(startPin.getParentGateView().layoutXProperty())
        );
        startYProperty().bind(
                startPin.layoutYProperty().add(startPin.getParentGateView().layoutYProperty())
        );

        //Bind the End Point
        endXProperty().bind(
                endPin.layoutXProperty().add(endPin.getParentGateView().layoutXProperty())
        );
        endYProperty().bind(
                endPin.layoutYProperty().add(endPin.getParentGateView().layoutYProperty())
        );

        //Set Control Points for the Curve
        //These control points make the wire "bend" horizontally
        controlX1Property().bind(
                startXProperty().add(endXProperty()).divide(2)
        );
        controlY1Property().bind(
                startYProperty()
        );
        controlX2Property().bind(
                startXProperty().add(endXProperty()).divide(2)
        );
        controlY2Property().bind(
                endYProperty()
        );
    }

    public void select() {
        setStroke(Color.DODGERBLUE);
        setStrokeWidth(5);
        toFront(); // Bring selected wire to front
    }

    //Method to remove selection
    public void deselect() {
        setStroke(Color.BLACK);
        setStrokeWidth(3);
    }

    // Inside Wire.java, add a dynamic update method:
    public void updateSignalColor() {
        if (startPin != null && startPin.getParentGateView() != null) {
            boolean signalHigh = startPin.getParentGateView().getCurrentEvaluatedValue();

            // If selected, keep the blue selection border highlight
            if (getStroke() == Color.DODGERBLUE) {
                return;
            }

            if (signalHigh) {
                setStroke(Color.web("#2ecc71")); // Vivid Green for active signal
                setStrokeWidth(3.5);             // Slightly thicker to stand out
            } else {
                setStroke(Color.web("#7f8c8d")); // Muted slate gray for inactive/Low lines
                setStrokeWidth(2.5);
            }
        }
    }
    // These methods are needed to manage the logical connection
    public ConnectionPin getStartPin() {
        return startPin;
    }

    public ConnectionPin getEndPin() {
        return endPin;
    }

    public Gate getInputGateModel() {
        return inputGateModel;
    }
}