package org.userservice.exception;

public class CustomServiceUnavailableException extends RuntimeException {
    public CustomServiceUnavailableException(String message) {
        super(message);
    }
}