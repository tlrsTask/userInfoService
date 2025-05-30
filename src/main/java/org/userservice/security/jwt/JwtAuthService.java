package org.userservice.security.jwt;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.userservice.dto.security.TokenResponse;
import org.userservice.dto.security.LoginRequest;
import org.userservice.security.interfaces.IAuthService;
import org.userservice.security.interfaces.RefreshTokenStrategy;

@Service
@RequiredArgsConstructor
public class JwtAuthService implements IAuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenStrategy refreshTokenStrategy;

    @Transactional
    public TokenResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );
        String accessToken = jwtService.generateAccessToken(authentication);
        return refreshTokenStrategy.createLoginResponse(accessToken, authentication);
    }

    @Transactional
    public TokenResponse refreshToken(String refreshToken) {
        return refreshTokenStrategy.refresh(refreshToken);
    }
}