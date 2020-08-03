package org.example.elevatorsimulation.exception;

public class ElevatorSimulationException extends RuntimeException {
    public ElevatorSimulationException(String message) {
        super(message);
    }

    public ElevatorSimulationException(String message, Throwable cause) {
        super(message, cause);
    }
}
