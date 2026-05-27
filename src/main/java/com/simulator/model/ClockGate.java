package com.simulator.model;

/**
 * A logical clock gate that supports synchronous two-phase updates
 * to ensure reliable edge-triggering across the simulation network.
 */
public class ClockGate extends AbstractGate {

    private boolean state = false;
    private boolean nextState = false;

    /**
     * Phase 1: Prepares the inverted clock state.
     * Does not change the active output yet.
     */
    public void prepareToggle() {
        this.nextState = !this.state;
    }

    /**
     * Phase 2: Commits the toggled state to the live simulation system.
     */
    public void commitToggle() {
        this.state = this.nextState;
        this.lastStableState = this.state;
    }

    @Override
    public boolean getOutput() {
        return this.state;
    }

    // Note: getSafeOutput() and resetEvaluation() are removed here
    // because they are already cleanly handled by AbstractGate!
}