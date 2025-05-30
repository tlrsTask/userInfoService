package org.userservice.dto.security;

public record TokenResponse(String accessToken, String refresh_token) {}