package org.example.elevatorsimulation.service;

import org.example.elevatorsimulation.model.ElevatorCallRequest;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CopyOnWriteArrayList;

public final class BuildingService {

    private ConcurrentLinkedDeque<ElevatorCallRequest> requestQueue;
    private List<Elevator> elevatorList = new CopyOnWriteArrayList<>();
    private static volatile BuildingService instance = null;

    private BuildingService() {
        if (instance != null) {
            throw new IllegalStateException("Already instantiated");
        }
        this.requestQueue = new ConcurrentLinkedDeque<>();
    }

    public static BuildingService getInstance() {
        if (instance == null) {
            synchronized (BuildingService.class) {
                if (instance == null) {
                    instance = new BuildingService();
                }
            }
        }
        return instance;
    }

    public void initializeElevators(int noOfElevators) {
        for (int i = 0; i < noOfElevators; i++) {
            Elevator elevator = new Elevator(i, this);
            Thread t = new Thread(elevator);
            t.start();

            this.elevatorList.add(elevator);
        }
    }

    public List<Elevator> getElevatorList() {
        return elevatorList;
    }

    public Optional<ElevatorCallRequest> fetch() {
        return Optional.ofNullable(this.requestQueue.peekFirst());
    }

    public void removeRequest() {
        if (!this.requestQueue.isEmpty()) {
            this.requestQueue.pollFirst();
        }
    }

    public void submit(ElevatorCallRequest elevatorCallRequest) {
        this.requestQueue.offerLast(elevatorCallRequest);
    }
}
