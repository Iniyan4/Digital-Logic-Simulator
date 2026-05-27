package com.simulator.model;

/**
 * A controllable input source (like a switch or a power rail).
 * It implements Gate so it can be "plugged into" other gates.
 */
public class InputSwitch implements Gate {

    private boolean state = false;

    /**
     * Manually sets the state of this switch.
     * @param state true for ON (1), false for OFF (0)
     */
    public void setState(boolean state) {
        this.state = state;
    }

    @Override
    public boolean getOutput() {
        return this.state;
    }

    @Override
    public boolean getSafeOutput() {
        return getOutput();
    }

    @Override
    public void resetEvaluation() {
        // No-op for constant/independent source gates
    }
}