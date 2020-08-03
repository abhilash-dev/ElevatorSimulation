package org.example.elevatorsimulation.scheduler;

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

public class NaiveScheduler implements Scheduler, Runnable {
    private Thread thread;
    private final AtomicBoolean running = new AtomicBoolean(false);

    @Override
    public void start() {
        this.thread = new Thread(this);
        this.thread.start();
    }

    @Override
    public void stop() {
        this.running.set(false);
    }

    @Override
    public void run() {
        this.running.set(true);
        while (this.running.get()) {
            try {
                if (BuildingService.getInstance().fetch().isPresent()) {
                    ElevatorCallRequest request = BuildingService.getInstance().fetch().get();
                    if (schedule(request)) {
                        BuildingService.getInstance().removeRequest();
                    }
                }
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean schedule(ElevatorCallRequest elevatorRequest) {
        Elevator elevator = null;

        ElevatorState elevatorState = getRequestedElevatorDirection(elevatorRequest);

        synchronized (BuildingService.getInstance()) {
            List<Elevator> elevatorList = BuildingService.getInstance().getElevatorList();
            Optional<Elevator> stationaryElevatorOpt = elevatorList.stream()
                    .filter(e -> e.getElevatorState().equals(ElevatorState.STATIONARY))
                    .min(Comparator.comparingInt(a -> elevatorRequest.getRequestFloor() - a.getCurrentFloor()));

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

            if (elevator != null) {
                setUpElevatorPath(elevatorRequest, elevator);
                System.out.println(elevatorRequest.getRequestFloor() + " to " + elevatorRequest.getTargetFloor() + " scheduled to elevator" + elevator.getId());
            }
        }
        return elevator != null;
    }

    private void setUpElevatorPath(ElevatorCallRequest elevatorCallRequest, Elevator elevator) {
        // if the elevator is not at the requested floor, set up a path to get to request floor
        if (elevator.getCurrentFloor() != elevatorCallRequest.getRequestFloor()) {
            ElevatorCallRequest toRequestFloor = new ElevatorCallRequest(elevator.getCurrentFloor(), elevatorCallRequest.getRequestFloor());
            ElevatorState requestFloorDirection = getRequestedElevatorDirection(toRequestFloor);


            NavigableSet<Integer> pathCheckpoints = elevator.getPathMap().get(requestFloorDirection);
            if (pathCheckpoints == null) {
                pathCheckpoints = new ConcurrentSkipListSet<>();
            }

            pathCheckpoints.add(elevator.getCurrentFloor());
            pathCheckpoints.add(elevatorCallRequest.getRequestFloor());
            elevator.getPathMap().put(requestFloorDirection, pathCheckpoints);
        }
        // the elevator is now at the requested floor, set up a path from requested floor to target floor
        ElevatorCallRequest toTargetFloor = new ElevatorCallRequest(elevatorCallRequest.getRequestFloor(), elevatorCallRequest.getTargetFloor());
        ElevatorState targetFloorDirection = getRequestedElevatorDirection(toTargetFloor);

        NavigableSet<Integer> targetPathCheckpoints = elevator.getPathMap().get(targetFloorDirection);
        if (targetPathCheckpoints == null) {
            targetPathCheckpoints = new ConcurrentSkipListSet<>();
        }

        targetPathCheckpoints.add(elevatorCallRequest.getRequestFloor());
        targetPathCheckpoints.add(elevatorCallRequest.getTargetFloor());
        elevator.getPathMap().put(targetFloorDirection, targetPathCheckpoints);
    }

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