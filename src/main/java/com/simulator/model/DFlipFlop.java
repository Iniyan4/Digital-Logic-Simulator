package com.simulator.model;

public class DFlipFlop extends AbstractGate implements BinaryGate {
    private Gate clockInput = null;
    private Gate dInput = null;

    private boolean lastClockState = false;
    private boolean nextClockState = false; // <-- Track the pending clock edge state
    private boolean internalState = false;
    private boolean nextState = false;

    // Add these methods to capture/restore the sequential state fully
    public Object captureStateSnapshot() {
        return new boolean[]{lastClockState, nextClockState, internalState, nextState, lastStableState};
    }

    public void restoreStateSnapshot(Object snapshot) {
        boolean[] states = (boolean[]) snapshot;
        this.lastClockState = states[0];
        this.nextClockState = states[1];
        this.internalState = states[2];
        this.nextState = states[3];
        this.lastStableState = states[4];
    }

    @Override
    public void setInputB(Gate clockSource) {
        this.clockInput = clockSource;
    }

    @Override
    public void setInputA(Gate dSource) {
        this.dInput = dSource;
    }

    /**
     * Phase 1: Clock Edge Capture Pass.
     * Evaluates inputs and updates the nextState buffer on a rising clock edge.
     */
    public void prepareNextState() {
        boolean currentClock = (clockInput != null) && clockInput.getSafeOutput();
        boolean currentD = (dInput != null) && dInput.getSafeOutput();

        // Rising Edge Detection: Compare against the last fully COMMITTED clock state
        if (!lastClockState && currentClock) {
            nextState = currentD;
        }

        // Stage the clock state change to be committed simultaneously in Phase 2
        this.nextClockState = currentClock;
    }

    /**
     * Phase 2: State Commit Pass.
     * Synchronously commits the prepared state to the active runtime state.
     */
    public void commitState() {
        this.internalState = this.nextState;
        this.lastClockState = this.nextClockState; // <-- Safely advance clock state here
        this.lastStableState = this.internalState;
    }

    @Override
    public boolean getOutput() {
        return internalState;
    }
}