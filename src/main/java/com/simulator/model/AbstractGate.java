package com.simulator.model;

public abstract class AbstractGate implements Gate {
    // Flag to detect infinite recursive execution stacks
    protected boolean isEvaluating = false;

    // Remembers the last valid value to return when breaking a loop
    protected boolean lastStableState = false;

    @Override
    public final boolean getSafeOutput() {
        if (isEvaluating) {
            // Loop detected! Break recursion safely by returning the last stable value.
            return lastStableState;
        }

        isEvaluating = true;
        try {
            // Run the actual subclass logic (e.g. stateA && stateB)
            lastStableState = getOutput();
            return lastStableState;
        } finally {
            isEvaluating = false;
        }

    }

    public void forceStableState(boolean value) { this.lastStableState = value; }
    @Override
    public void resetEvaluation() {
        this.isEvaluating = false;
    }
}
