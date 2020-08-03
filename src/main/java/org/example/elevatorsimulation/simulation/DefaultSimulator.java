package org.example.elevatorsimulation.simulation;

import org.example.elevatorsimulation.exception.BadRequestException;
import org.example.elevatorsimulation.exception.ElevatorSimulationException;
import org.example.elevatorsimulation.model.ElevatorCallRequest;
import org.example.elevatorsimulation.scheduler.Scheduler;
import org.example.elevatorsimulation.service.BuildingService;

import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.example.elevatorsimulation.util.Constants.MAX_ELEVATORS;
import static org.example.elevatorsimulation.util.Constants.MAX_FLOORS;

public class DefaultSimulator implements Simulator, Runnable {
    private Thread thread;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private long interval;
    private int floorCount;
    private int elevatorCount;
    private Scheduler scheduler;
    private int simulationCount;

    public DefaultSimulator(int floorCount, int elevatorCount, Scheduler scheduler) {
        this(floorCount, elevatorCount, scheduler, 5000, 20);
    }

    public DefaultSimulator(int floorCount, int elevatorCount, Scheduler scheduler, long interval, int simulationCount) {
        if (!isValid(floorCount, elevatorCount)) {
            throw new BadRequestException("Bad Request to Elevator Simulation. Please enter a value between 1-10 for elevator " +
                    "count & 1-1000 for no. of floors in the building");
        }
        this.floorCount = floorCount;
        this.elevatorCount = elevatorCount;
        this.scheduler = scheduler;
        this.interval = interval;
        this.simulationCount = simulationCount;
    }

    private boolean isValid(int floorCount, int elevatorCount) {
        return floorCount > 0 && floorCount <= MAX_FLOORS && elevatorCount > 0 && elevatorCount <= MAX_ELEVATORS;
    }

    @Override
    public void run() {
        running.set(true);
        for (int i = 0; i < simulationCount; i++) {
            int[] vals = generateRandom();
            ElevatorCallRequest elevatorCallRequest = new ElevatorCallRequest(vals[0], vals[1]);
            elevatorCallRequest.submitRequest();
            try {
                Thread.sleep(this.interval);
            } catch (InterruptedException ie) {
                throw new ElevatorSimulationException("There was a problem during generation of elevator call requests", ie);
            }
        }
    }

    private int[] generateRandom() {
        int a = 0;
        int b = 0;

        while (a == b) {
            a = getRandomNumberUsingInts(0, floorCount);
            b = getRandomNumberUsingInts(0, floorCount);
        }

        return new int[]{a, b};
    }

    public int getRandomNumberUsingInts(int min, int max) {
        Random random = new Random();
        return random.ints(min, max)
                .findFirst()
                .getAsInt();
    }

    @Override
    public void start() {
        thread = new Thread(this);
        thread.start();
        this.scheduler.start();
        BuildingService.getInstance().initializeElevators(this.elevatorCount);
    }

    @Override
    public void stop() {
        running.set(false);
    }
}