package org.example.elevatorsimulation.scheduler;

import org.example.elevatorsimulation.model.ElevatorCallRequest;

public interface Scheduler {

    boolean schedule(ElevatorCallRequest elevatorRequest);

    void start();

    void stop();
}
