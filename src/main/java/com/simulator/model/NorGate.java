package com.simulator.model;

/**
 * Implements a 2-input NOR gate using composition (OR + NOT).
 */
public class NorGate extends AbstractGate implements BinaryGate {

    // Encapsulated internal gates
    private final OrGate orGate;
    private final NotGate notGate;

    public NorGate() {
        this.orGate = new OrGate();
        this.notGate = new NotGate();

        // Wire them together: OR -> NOT
        this.notGate.setInput(this.orGate);
    }

    @Override
    public void setInputA(Gate inputA) {
        this.orGate.setInputA(inputA);
    }

    @Override
    public void setInputB(Gate inputB) {
        this.orGate.setInputB(inputB);
    }

    @Override
    public boolean getOutput() {
        // Get the output from the final gate in the chain
        return this.notGate.getSafeOutput();
    }
}