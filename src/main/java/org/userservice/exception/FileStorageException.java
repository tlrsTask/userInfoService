package org.userservice.exception;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FileStorageException extends RuntimeException {
    public FileStorageException(String message, Throwable cause) {
        super(message, cause);
        log.error("FileStorageException: {}", message, cause);
    }
}
