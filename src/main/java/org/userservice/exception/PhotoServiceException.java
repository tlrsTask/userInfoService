package org.userservice.exception;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PhotoServiceException extends RuntimeException {
    public PhotoServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    public PhotoServiceException(String message) {
        super(message);
    }
}