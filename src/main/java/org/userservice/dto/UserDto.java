package org.userservice.dto;

import java.util.UUID;

public record UserDto(
        UUID id,
        String username
) {}
