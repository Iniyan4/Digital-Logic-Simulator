package com.simulator.model;

/**
 * Implements a NOT gate (Inverter).
 * This is a concrete implementation of the UnaryGate interface.
 */
public class NotGate extends AbstractGate implements UnaryGate {

    private Gate input = null;

    @Override
    public void setInput(Gate input) {
        this.input = input;
    }

    @Override
    public boolean getOutput() {
        // Handle the case of an unconnected input (default to LOW/false)
        if (input == null) {
            return true; // NOT(false) is true
        }
        // The core logic: return the inverse of the input
        return !input.getSafeOutput();
    }
}