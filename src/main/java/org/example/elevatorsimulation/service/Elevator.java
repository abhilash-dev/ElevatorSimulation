package org.example.elevatorsimulation.service;

import lombok.extern.slf4j.Slf4j;
import org.example.elevatorsimulation.exception.ElevatorSimulationException;
import org.example.elevatorsimulation.model.ElevatorState;

import java.util.Iterator;
import java.util.NavigableSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author Abhilash Sulibela
 * <p>
 * Class that encapsulates an Elevator within a building
 */
@Slf4j
public class Elevator implements Runnable {
    private ConcurrentMap<ElevatorState, NavigableSet<Integer>> pathMap;
    private int id;


    private ElevatorState elevatorState;
    private BuildingService buildingService;
    private Integer currentFloor;
    private NavigableSet<Integer> pathCheckpoints;

    public Elevator(int id, BuildingService buildingService) {
        this.id = id;
        this.buildingService = buildingService;
        this.pathMap = new ConcurrentHashMap<>();
        this.setCurrentFloor(0);
        this.setElevatorState(ElevatorState.STATIONARY);
    }

    public int getId() {
        return id;
    }

    public ConcurrentMap<ElevatorState, NavigableSet<Integer>> getPathMap() {
        return pathMap;
    }

    public ElevatorState getElevatorState() {
        return elevatorState;
    }

    public void setElevatorState(ElevatorState elevatorState) {
        this.elevatorState = elevatorState;
    }

    public int getCurrentFloor() {
        return currentFloor;
    }

    public void setCurrentFloor(int currentFloor) {
        this.currentFloor = currentFloor;
    }

    /**
     * move the elevators at a regular pace of 1 floor/sec
     */
    public void move() {
        log.trace("invoking move()");
        this.buildingService.getElevatorList().set(this.getId(), this);
        Iterator<ElevatorState> iter = this.pathMap.keySet().iterator();

        while (iter.hasNext()) {
            this.elevatorState = iter.next();

            this.pathCheckpoints = this.pathMap.get(elevatorState);

            Integer currFlr = null;
            Integer nextFlr = null;

            while (!this.pathCheckpoints.isEmpty()) {

                if (this.elevatorState.equals(ElevatorState.UP)) {
                    currFlr = this.pathCheckpoints.pollFirst();
                    nextFlr = this.pathCheckpoints.higher(currFlr);

                } else if (elevatorState.equals(ElevatorState.DOWN)) {
                    currFlr = this.pathCheckpoints.pollLast();
                    nextFlr = this.pathCheckpoints.lower(currFlr);
                } else {
                    return;
                }

                setCurrentFloor(currFlr);

                if (nextFlr != null) {
                    generateIntermediateFloorCheckpoints(currFlr, nextFlr);
                } else {
                    setElevatorState(ElevatorState.STATIONARY);
                    this.buildingService.getElevatorList().set(this.getId(), this);
                }

                log.info("Elevator ID - {} | Current floor - {} | next - {}", this.getId(), this.getCurrentFloor(), getElevatorState());
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    log.error("There was a problem while on move with elevator - {}", this.getId());
                    throw new ElevatorSimulationException("There was a problem while on move with elevator - " + this.getId(), e);
                }
            }
        }
    }

    /**
     * generate all the checkpoints / floors in between the given start & end floors
     *
     * @param initial the starting floor
     * @param target  the ending floor
     */
    private void generateIntermediateFloorCheckpoints(int initial, int target) {

        if (Math.abs(initial - target) <= 1) {
            return;
        }

        // identify the direction to travel i.e., UP or DOWN for intermediate floors
        int n = 1;
        if (target - initial < 0) {
            // DOWN
            n = -1;
        }

        while (initial != target) {
            initial += n;
            this.pathCheckpoints.add(initial);
        }
    }

    @Override
    public void run() {
        while (true) {
            move();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}