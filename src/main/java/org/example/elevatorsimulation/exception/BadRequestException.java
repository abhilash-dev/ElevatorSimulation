package org.example.elevatorsimulation.exception;

/**
 * @author Abhilash Sulibela
 * <p>
 * Custom exception thrown when a bad request is found
 */
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }

    public BadRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}
