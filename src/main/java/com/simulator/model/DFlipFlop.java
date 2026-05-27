package com.simulator.model;

public class DFlipFlop extends AbstractGate implements BinaryGate {
    private Gate clockInput = null;
    private Gate dInput = null;

    private boolean lastClockState = false;
    private boolean internalState = false;
    private boolean nextState = false;

    @Override
    public void setInputA(Gate clockSource) {
        this.clockInput = clockSource;
    }

    @Override
    public void setInputB(Gate dSource) {
        this.dInput = dSource;
    }

    /**
     * Phase 1: Clock Edge Capture Pass.
     * Evaluates inputs and updates the nextState buffer on a rising clock edge.
     */
    public void prepareNextState() {
        boolean currentClock = (clockInput != null) && clockInput.getSafeOutput();
        boolean currentD = (dInput != null) && dInput.getSafeOutput();

        // Rising Edge Detection (Transition from false to true)
        if (!lastClockState && currentClock) {
            nextState = currentD;
        }
        lastClockState = currentClock;
    }

    /**
     * Phase 2: State Commit Pass.
     * Synchronously commits the prepared state to the active runtime state.
     */
    public void commitState() {
        this.internalState = this.nextState;
        this.lastStableState = this.internalState;
    }

    @Override
    public boolean getOutput() {
        return internalState;
    }
}