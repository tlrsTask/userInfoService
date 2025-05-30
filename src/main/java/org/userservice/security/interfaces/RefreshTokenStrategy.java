package org.userservice.security.interfaces;

import org.springframework.security.core.Authentication;
import org.userservice.dto.security.TokenResponse;

public interface RefreshTokenStrategy {
    TokenResponse createLoginResponse(String accessToken, Authentication authentication);
    TokenResponse refresh(String refreshToken);

    boolean supports(boolean useRefreshToken);
}