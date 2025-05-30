package org.userservice.dto.security;

public record RegisterRequest(
        String username,
        String password
) {}
