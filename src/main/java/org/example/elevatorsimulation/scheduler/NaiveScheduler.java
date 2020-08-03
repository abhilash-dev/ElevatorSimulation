package org.example.elevatorsimulation.scheduler;

import lombok.extern.slf4j.Slf4j;
import org.example.elevatorsimulation.exception.ElevatorSimulationException;
import org.example.elevatorsimulation.model.ElevatorCallRequest;
import org.example.elevatorsimulation.model.ElevatorState;
import org.example.elevatorsimulation.service.BuildingService;
import org.example.elevatorsimulation.service.Elevator;

import java.util.Comparator;
import java.util.List;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Abhilash Sulibela
 * <p>
 * A naive implementation for the Scheduler interface to schedule elevator call requests in a given building using the BuildingService
 */
@Slf4j
public class NaiveScheduler implements Scheduler, Runnable {
    private Thread thread;
    private final AtomicBoolean running = new AtomicBoolean(false);

    @Override
    public void start() {
        log.info("Started the scheduler");
        this.thread = new Thread(this);
        this.thread.start();
    }

    @Override
    public void stop() {
        log.info("Stopping the scheduler");
        this.running.set(false);
    }

    @Override
    public void run() {
        this.running.set(true);
        while (this.running.get()) {
            try {
                // fetch a call request from the queue, if it exists
                if (BuildingService.getInstance().fetch().isPresent()) {
                    ElevatorCallRequest request = BuildingService.getInstance().fetch().get();
                    log.debug("scheduling the call request from floor - {} to floor - {}", request.getRequestFloor(), request.getTargetFloor());
                    if (schedule(request)) {
                        // remove the request after successfully scheduling the call request
                        log.debug("successfully scheduled the call request from floor - {} to floor - {}", request.getRequestFloor(), request.getTargetFloor());
                        BuildingService.getInstance().removeRequest();
                    }
                }
                Thread.sleep(100);
            } catch (InterruptedException e) {
                log.error("There was a problem when scheduling elevator call requests");
                throw new ElevatorSimulationException("There was a problem when scheduling elevator call requests", e);
            }
        }
    }

    /**
     * Schedule the given instance of ElevatorRequest to an elevator using the building service
     *
     * @param elevatorRequest an instance of ElevatorRequest capturing a call request
     * @return True, if the request was successfully scheduled. False, otherwise
     */
    @Override
    public boolean schedule(ElevatorCallRequest elevatorRequest) {
        Elevator elevator = null;

        ElevatorState elevatorState = getRequestedElevatorDirection(elevatorRequest);

        synchronized (BuildingService.getInstance()) {
            List<Elevator> elevatorList = BuildingService.getInstance().getElevatorList();

            // fetch all elevators in stationary state
            Optional<Elevator> stationaryElevatorOpt = elevatorList.stream()
                    .filter(e -> e.getElevatorState().equals(ElevatorState.STATIONARY))
                    .min(Comparator.comparingInt(a -> elevatorRequest.getRequestFloor() - a.getCurrentFloor()));

            // fetch all elevators approaching the requested floor
            if (elevatorState.equals(ElevatorState.UP)) {
                Optional<Elevator> approachingElevatorOpt = elevatorList.stream()
                        .filter(e -> e.getElevatorState().equals(ElevatorState.UP) && e.getCurrentFloor() <= elevatorRequest.getRequestFloor())
                        .min(Comparator.comparingInt(a -> elevatorRequest.getRequestFloor() - a.getCurrentFloor()));

                // prefer a moving elevator over a stationary one
                if (approachingElevatorOpt.isPresent() && stationaryElevatorOpt.isPresent()) {
                    Elevator approachingElevator = approachingElevatorOpt.get();
                    Elevator stationaryElevator = stationaryElevatorOpt.get();

                    if (approachingElevator.getCurrentFloor() == stationaryElevator.getCurrentFloor()) {
                        elevator = approachingElevator;
                    } else {
                        elevator = approachingElevator.getCurrentFloor() > stationaryElevator.getCurrentFloor() ? approachingElevator : stationaryElevator;
                    }
                } else if (stationaryElevatorOpt.isPresent()) {
                    elevator = stationaryElevatorOpt.get();
                } else if (approachingElevatorOpt.isPresent()) {
                    elevator = approachingElevatorOpt.get();
                }

            } else if (elevatorState.equals(ElevatorState.DOWN)) {
                // fetch all approaching elevators
                Optional<Elevator> approachingElevatorOpt = elevatorList.stream()
                        .filter(e -> e.getElevatorState().equals(ElevatorState.DOWN) && e.getCurrentFloor() >= elevatorRequest.getRequestFloor())
                        .min(Comparator.comparingInt(a -> elevatorRequest.getRequestFloor() - a.getCurrentFloor()));

                // prefer a moving elevator over a stationary one
                if (approachingElevatorOpt.isPresent() && stationaryElevatorOpt.isPresent()) {
                    Elevator approachingElevator = approachingElevatorOpt.get();
                    Elevator stationaryElevator = stationaryElevatorOpt.get();

                    if (approachingElevator.getCurrentFloor() == stationaryElevator.getCurrentFloor()) {
                        elevator = approachingElevator;
                    } else {
                        elevator = approachingElevator.getCurrentFloor() < stationaryElevator.getCurrentFloor() ? approachingElevator : stationaryElevator;
                    }
                } else if (stationaryElevatorOpt.isPresent()) {
                    elevator = stationaryElevatorOpt.get();
                } else if (approachingElevatorOpt.isPresent()) {
                    elevator = approachingElevatorOpt.get();
                }
            }

            // if an elevator was scheduled
            if (elevator != null) {
                log.debug("Call request from - {} to - {} scheduled to Elevator - {}", elevatorRequest.getRequestFloor(), elevatorRequest.getTargetFloor(), elevator.getId());
                setUpElevatorPath(elevatorRequest, elevator);
                System.out.println(elevatorRequest.getRequestFloor() + " to " + elevatorRequest.getTargetFloor() + " scheduled to elevator" + elevator.getId());
            }
        }
        return elevator != null;
    }

    /**
     * Setup path checkpoints to reach the target floor from the elevators current floor
     *
     * @param elevatorCallRequest an instance of ElevatorRequest encapsulating a user elevator call request
     * @param elevator            the elevator that is scheduled to carry on the given call request
     */
    private void setUpElevatorPath(ElevatorCallRequest elevatorCallRequest, Elevator elevator) {
        // if the elevator is not at the requested floor, set up a path to get to request floor
        if (elevator.getCurrentFloor() != elevatorCallRequest.getRequestFloor()) {
            ElevatorCallRequest toRequestFloor = new ElevatorCallRequest(elevator.getCurrentFloor(), elevatorCallRequest.getRequestFloor());
            ElevatorState requestFloorDirection = getRequestedElevatorDirection(toRequestFloor);

            NavigableSet<Integer> pathCheckpoints = elevator.getPathMap().getOrDefault(requestFloorDirection, new ConcurrentSkipListSet<>());
            pathCheckpoints.add(elevator.getCurrentFloor());
            pathCheckpoints.add(elevatorCallRequest.getRequestFloor());

            elevator.getPathMap().put(requestFloorDirection, pathCheckpoints);
        }
        // the elevator is now at the requested floor, set up a path from requested floor to target floor
        ElevatorCallRequest toTargetFloor = new ElevatorCallRequest(elevatorCallRequest.getRequestFloor(), elevatorCallRequest.getTargetFloor());
        ElevatorState targetFloorDirection = getRequestedElevatorDirection(toTargetFloor);

        NavigableSet<Integer> targetPathCheckpoints = elevator.getPathMap().getOrDefault(targetFloorDirection, new ConcurrentSkipListSet<>());
        targetPathCheckpoints.add(elevatorCallRequest.getRequestFloor());
        targetPathCheckpoints.add(elevatorCallRequest.getTargetFloor());

        elevator.getPathMap().put(targetFloorDirection, targetPathCheckpoints);
    }

    /**
     * Return the direction the elevator has to travel to satisfy the current call request
     *
     * @param elevatorCallRequest an instance of ElevatorCallRequest encapsulating a user elevator call request
     * @return a value depicting the direction for the elevator to travel
     */
    private ElevatorState getRequestedElevatorDirection(ElevatorCallRequest elevatorCallRequest) {
        int requestedFloor = elevatorCallRequest.getRequestFloor();
        int targetFloor = elevatorCallRequest.getTargetFloor();

        if (targetFloor - requestedFloor > 0) {
            return ElevatorState.UP;
        } else {
            return ElevatorState.DOWN;
        }
    }
}