package org.userservice.security.service;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.userservice.dto.security.TokenResponse;
import org.userservice.security.interfaces.RefreshTokenStrategy;

@Component
@ConditionalOnProperty(name = "jwt.use.refresh", havingValue = "false")
@RequiredArgsConstructor
public class NoRefreshTokenStrategy implements RefreshTokenStrategy {
    @Override
    public TokenResponse createLoginResponse(String accessToken, Authentication authentication) {
        return new TokenResponse(accessToken, null);
    }
    
    @Override
    public boolean supports(boolean useRefreshToken) {
        return !useRefreshToken;
    }

    @Override
    public TokenResponse refresh(String refreshToken) {
        throw new UnsupportedOperationException("Refresh token is disabled");
    }
}