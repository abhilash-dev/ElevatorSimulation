package org.example.elevatorsimulation.exception;

/**
 * @author Abhilash Sulibela
 * <p>
 * Custom exception thrown when an unkown error has occured
 */
public class ElevatorSimulationException extends RuntimeException {
    public ElevatorSimulationException(String message) {
        super(message);
    }

    public ElevatorSimulationException(String message, Throwable cause) {
        super(message, cause);
    }
}
