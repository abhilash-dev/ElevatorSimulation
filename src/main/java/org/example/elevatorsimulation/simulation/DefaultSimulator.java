package org.example.elevatorsimulation.simulation;

import org.example.elevatorsimulation.model.ElevatorCallRequest;
import org.example.elevatorsimulation.scheduler.Scheduler;
import org.example.elevatorsimulation.service.BuildingService;

import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

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
        this.floorCount = floorCount;
        this.elevatorCount = elevatorCount;
        this.scheduler = scheduler;
        this.interval = interval;
        this.simulationCount = simulationCount;
    }

    @Override
    public void run() {
        running.set(true);
        for (int i = 0; i < simulationCount; i++) {
            int[] vals = generateRandom();
            new ElevatorCallRequest(vals[0], vals[1]).submitRequest();
            try {
                Thread.sleep(this.interval);
            } catch (InterruptedException ie) {
                System.err.println(ie);
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