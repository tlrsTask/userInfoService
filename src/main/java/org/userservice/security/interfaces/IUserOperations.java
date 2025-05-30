package org.userservice.security.interfaces;

import org.userservice.dto.security.RegisterRequest;
import org.userservice.dto.UserDto;

public interface IUserOperations {
    UserDto register(RegisterRequest req);
    void changePassword(String username, String newPassword);
}
