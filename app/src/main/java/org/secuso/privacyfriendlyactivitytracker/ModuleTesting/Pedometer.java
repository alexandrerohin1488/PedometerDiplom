package org.secuso.privacyfriendlyactivitytracker.ModuleTesting;

public class Pedometer {
    private int steps;

    public Pedometer() {
        steps = 0;
    }

    public void addStep() {
        steps++;
    }

    public int getSteps() {
        return steps;
    }

    public void reset() {
        steps = 0;
    }
}