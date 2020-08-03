package org.example.elevatorsimulation;

import org.example.elevatorsimulation.scheduler.NaiveScheduler;
import org.example.elevatorsimulation.simulation.DefaultSimulator;
import org.example.elevatorsimulation.simulation.Simulator;

/**
 * @author Abhilash Sulibela
 * <p>
 * The main class / starting point for the Elevator Simulation
 */
public class App {
    public static void main(String[] args) {
        Simulator simulator = new DefaultSimulator(20, 3, new NaiveScheduler(), 3000, 3);
        simulator.start();
    }
}
