package org.userservice.security.interfaces;

import org.userservice.dto.security.LoginRequest;
import org.userservice.dto.security.TokenResponse;

public interface IAuthService {
    TokenResponse login(LoginRequest req);
    TokenResponse refreshToken(String token);
}