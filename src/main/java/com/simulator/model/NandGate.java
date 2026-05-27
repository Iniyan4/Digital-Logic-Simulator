package com.simulator.model;

/**
 * Implements a 2-input NAND gate.
 * It "is a" BinaryGate, but "has a" AndGate and a NotGate.
 */
public class NandGate extends AbstractGate implements BinaryGate {

    // Internal gates (Encapsulation)
    private final AndGate andGate;
    private final NotGate notGate;

    public NandGate() {
        // Create the internal gates
        this.andGate = new AndGate();
        this.notGate = new NotGate();

        // Wire them together: The output of AND goes into the input of NOT
        this.notGate.setInput(this.andGate);
    }

    @Override
    public void setInputA(Gate inputA) {
        // Pass the input down to the internal AND gate
        this.andGate.setInputA(inputA);
    }

    @Override
    public void setInputB(Gate inputB) {
        // Pass the input down to the internal AND gate
        this.andGate.setInputB(inputB);
    }

    @Override
    public boolean getOutput() {
        // The final output is the output of the internal NOT gate
        return this.notGate.getSafeOutput();
    }
}