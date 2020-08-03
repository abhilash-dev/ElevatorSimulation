package org.example.elevatorsimulation.model;

import org.example.elevatorsimulation.service.BuildingService;

/**
 * @author Abhilash Sulibela
 * <p>
 * This class encapsulates the information required by a Call Request made a building floor
 */
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

    /**
     * Submit this request to the Building service's processing queue to schedule an elevator
     */
    public void submitRequest() {
        BuildingService.getInstance().submit(this);
    }
}
