package com.simulator.model;

public interface Gate {
    /**
     * Computes and returns the output state of this gate.
     * This method will recursively pull outputs from its inputs.
     */
    boolean getOutput();

    /**
     * Safely retrieves the output while tracking evaluation state
     * to prevent infinite loops.
     */
    boolean getSafeOutput();

    /**
     * Resets the loop detection flags before a new evaluation cycle starts.
     */
    void resetEvaluation();
}