package org.userservice.security.service;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.userservice.dto.security.TokenResponse;
import org.userservice.security.interfaces.RefreshTokenStrategy;
import org.userservice.security.jwt.JwtService;
import org.userservice.security.jwt.TokenBlacklistService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import java.util.List;

@Component
@ConditionalOnProperty(name = "jwt.use.refresh", havingValue = "true")
@RequiredArgsConstructor
public class DefaultRefreshTokenStrategy implements RefreshTokenStrategy {

    private final JwtService jwtService;
    private final TokenBlacklistService blacklistService;

    @Override
    public TokenResponse createLoginResponse(String accessToken, Authentication authentication) {
        String refreshToken = jwtService.generateRefreshToken(authentication);
        return new TokenResponse(accessToken, refreshToken);
    }
    
    @Override
    public boolean supports(boolean useRefreshToken) {
        return useRefreshToken;
    }

    @Override
    public TokenResponse refresh(String refreshToken) {
        if (!jwtService.validateRefreshToken(refreshToken)) {
            throw new JwtException("Invalid refresh token");
        }
        if (blacklistService.isBlacklisted(refreshToken)) {
            throw new JwtException("Refresh token revoked");
        }
        Claims claims = jwtService.extractRefreshClaims(refreshToken);
        String username = claims.getSubject();
        Authentication auth = new UsernamePasswordAuthenticationToken(username, null, List.of());
        String access = jwtService.generateAccessToken(auth);
        String fresh = jwtService.generateRefreshToken(auth);
        blacklistService.blacklist(refreshToken);
        return new TokenResponse(access, fresh);
    }
}

