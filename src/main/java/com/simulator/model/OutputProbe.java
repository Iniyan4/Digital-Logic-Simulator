package com.simulator.model;

/**
 * A probe to read the output of a circuit.
 * We implement UnaryGate so it can be "plugged into"
 * our system just like any other gate.
 */
public class OutputProbe extends AbstractGate implements UnaryGate {

    private Gate input = null;

    @Override
    public void setInput(Gate input) {
        this.input = input;
    }

    /**
     * This is the method our ProbeView will call to get the state.
     */
    public boolean getResult() {
        if (input == null) {
            return false;
        }
        // Recursively pull the value from the entire circuit
        return input.getSafeOutput();
    }

    /**
     * Required by the Gate interface.
     * For a probe, its "output" is just its "input".
     * This allows a probe to be chained (though not typical).
     */
    @Override
    public boolean getOutput() {
        return getResult();
    }
}