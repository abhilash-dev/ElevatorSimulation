package org.example.elevatorsimulation.scheduler;

import org.example.elevatorsimulation.model.ElevatorCallRequest;

/**
 * Interface that captures the core functions of a Scheduler
 */
public interface Scheduler {

    boolean schedule(ElevatorCallRequest elevatorRequest);

    void start();

    void stop();
}
