package org.example.elevatorsimulation.model;

import org.example.elevatorsimulation.service.BuildingService;

public class ElevatorCallRequest {
    private int requestFloor;
    private int targetFloor;

    public ElevatorCallRequest(int requestFloor, int targetFloor) {
        this.requestFloor = requestFloor;
        this.targetFloor = targetFloor;
    }

    public int getRequestFloor() {
        return requestFloor;
    }

    public int getTargetFloor() {
        return targetFloor;
    }

    public void submitRequest() {
        BuildingService.getInstance().submit(this);
    }
}
