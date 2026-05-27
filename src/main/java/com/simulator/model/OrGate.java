package com.simulator.model;

/**
 * Implements a 2-input OR gate.
 * This is a concrete implementation of the BinaryGate interface.
 */
public class OrGate extends AbstractGate implements BinaryGate {

    private Gate inputA = null;
    private Gate inputB = null;

    @Override
    public void setInputA(Gate inputA) {
        this.inputA = inputA;
    }

    @Override
    public void setInputB(Gate inputB) {
        this.inputB = inputB;
    }

    @Override
    public boolean getOutput() {
        // Get the state of both inputs, defaulting to false if unconnected
        boolean stateA = (inputA != null) && inputA.getSafeOutput();
        boolean stateB = (inputB != null) && inputB.getSafeOutput();

        // The core logic: A OR B
        return stateA || stateB;
    }
}