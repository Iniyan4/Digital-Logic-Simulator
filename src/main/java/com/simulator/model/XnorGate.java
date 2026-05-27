package com.simulator.model;

/**
 * Implements a 2-input XNOR gate using composition (XOR + NOT).
 */
public class XnorGate extends AbstractGate implements BinaryGate {

    // Encapsulated internal gates
    private final XorGate xorGate;
    private final NotGate notGate;

    public XnorGate() {
        this.xorGate = new XorGate();
        this.notGate = new NotGate();

        // Wire them together: XOR -> NOT
        this.notGate.setInput(this.xorGate);
    }

    @Override
    public void setInputA(Gate inputA) {
        this.xorGate.setInputA(inputA);
    }

    @Override
    public void setInputB(Gate inputB) {
        this.xorGate.setInputB(inputB);
    }

    @Override
    public boolean getOutput() {
        return this.notGate.getSafeOutput();
    }
}