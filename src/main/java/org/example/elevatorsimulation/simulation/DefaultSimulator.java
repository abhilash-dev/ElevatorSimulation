package org.example.elevatorsimulation.simulation;

import lombok.extern.slf4j.Slf4j;
import org.example.elevatorsimulation.exception.BadRequestException;
import org.example.elevatorsimulation.exception.ElevatorSimulationException;
import org.example.elevatorsimulation.model.ElevatorCallRequest;
import org.example.elevatorsimulation.scheduler.Scheduler;
import org.example.elevatorsimulation.service.BuildingService;

import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.example.elevatorsimulation.util.Constants.*;

/**
 * @author Abhilash Sulibela
 * <p>
 * A default implementation for a simulator that generates random simulated elevator call requests at a regular interval
 */
@Slf4j
public class DefaultSimulator implements Simulator, Runnable {
    private Thread thread;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private long interval;
    private int floorCount;
    private int elevatorCount;
    private Scheduler scheduler;
    private int simulationCount;

    public DefaultSimulator(int floorCount, int elevatorCount, Scheduler scheduler) {
        this(floorCount, elevatorCount, scheduler, DEFAULT_SIMULATION_TIME_INTERVAL, DEFAULT_SIMULATION_COUNT);
    }

    public DefaultSimulator(int floorCount, int elevatorCount, Scheduler scheduler, long interval, int simulationCount) {
        if (!isValid(floorCount, elevatorCount)) {
            log.error("Bad Request to Elevator Simulation. Please enter a value between 1-10 for elevator count & 1-1000 " +
                    "for no. of floors in the building");
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
        return floorCount > MIN_FLOORS && floorCount <= MAX_FLOORS && elevatorCount > MIN_ELEVATORS && elevatorCount <= MAX_ELEVATORS;
    }

    @Override
    public void run() {
        running.set(true);
        log.debug("Generating {} call request simulations", simulationCount);
        for (int i = 0; i < simulationCount; i++) {
            int[] vals = generateRandom();
            int requestFloor = vals[0];
            int targetFloor = vals[1];
            log.debug("Randomly generated a call request from floor - {} to floor - {}", requestFloor, targetFloor);
            ElevatorCallRequest elevatorCallRequest = new ElevatorCallRequest(requestFloor, targetFloor);
            elevatorCallRequest.submitRequest();
            try {
                Thread.sleep(this.interval);
            } catch (InterruptedException ie) {
                log.error("There was a problem during generation of elevator call requests");
                throw new ElevatorSimulationException("There was a problem during generation of elevator call requests", ie);
            }
        }
    }

    private int[] generateRandom() {
        int a = 0;
        int b = 0;

        // as long as both the requestFloor & targetFloor are not same
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
        log.debug("Starting simulator...");
        thread = new Thread(this);
        thread.start();
        this.scheduler.start();
        BuildingService.getInstance().initializeElevators(this.elevatorCount);
    }

    @Override
    public void stop() {
        log.debug("Stopping simulator...");
        running.set(false);
    }
}