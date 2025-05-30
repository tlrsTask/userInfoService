package org.userservice.utils;

import org.springframework.stereotype.Component;
import org.userservice.dto.UserDto;
import org.userservice.entity.User;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class UserMapper {
    public UserDto toDto(User user) {
        return new UserDto(
                user.getId(),
                user.getUserName());
    }
}