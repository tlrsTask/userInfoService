package org.userservice.exception;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DeleteUserPhotoFromUserProfile extends RuntimeException {
    public DeleteUserPhotoFromUserProfile(String message) {
        super(message);
        log.error("UseDetailsStorageException: {}", message);
    }

}
