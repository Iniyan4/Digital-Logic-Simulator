package com.simulator.model;

/**
 * Implements a 2-input XOR gate using composition.
 * Logic: (A AND (NOT B)) OR ((NOT A) AND B)
 */
public class XorGate extends AbstractGate implements BinaryGate {

    // Encapsulated internal gates
    private final NotGate notA;
    private final NotGate notB;
    private final AndGate and1; // (NOT A) AND B
    private final AndGate and2; // A AND (NOT B)
    private final OrGate finalOr;

    public XorGate() {
        // Create all the necessary internal gates
        this.notA = new NotGate();
        this.notB = new NotGate();
        this.and1 = new AndGate();
        this.and2 = new AndGate();
        this.finalOr = new OrGate();

        // Wire them up according to the logic
        // (NOT A) -> and1
        this.and1.setInputA(notA);

        // A -> and2
        // B -> and1
        // (NOT B) -> and2
        this.and2.setInputB(notB);

        // Connect the outputs of the two AND gates to the final OR gate
        this.finalOr.setInputA(and1);
        this.finalOr.setInputB(and2);
    }

    @Override
    public void setInputA(Gate inputA) {
        // Input A feeds two internal gates
        this.notA.setInput(inputA);
        this.and2.setInputA(inputA);
    }

    @Override
    public void setInputB(Gate inputB) {
        // Input B feeds two internal gates
        this.notB.setInput(inputB);
        this.and1.setInputB(inputB);
    }

    @Override
    public boolean getOutput() {
        // The final output is from the OR gate
        return this.finalOr.getSafeOutput();
    }
}