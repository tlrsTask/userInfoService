package org.userservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.userservice.dto.*;
import org.userservice.dto.security.*;
import org.userservice.security.interfaces.IAuthService;
import org.userservice.security.interfaces.IUserOperations;
import org.userservice.service.details.UserDetailsService;

import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final IAuthService authService;
    private final IUserOperations userOperations;
    private final UserDetailsService userDetailsService;

    @PostMapping("/register")
    public ResponseEntity<UserDto> register(@RequestBody RegisterRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(userOperations.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@RequestBody LoginRequest request) {
        TokenResponse tokens = authService.login(request);
        return ResponseEntity.ok()
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokens.accessToken())
                .body(tokens);
    }

    @PostMapping("/refresh")
    @ConditionalOnProperty(name = "jwt.use.refresh", havingValue = "true")
    public ResponseEntity<TokenResponse> refresh(@RequestBody RefreshRequest request) {
        TokenResponse tokens = authService.refreshToken(request.refreshToken());
        return ResponseEntity.ok()
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokens.accessToken())
                .body(tokens);
    }

    @PostMapping("/{username}/change-password")
    public ResponseEntity<Void> changePassword(
            @PathVariable String username,
            @RequestBody ChangePasswordRequest req
    ) {
        userOperations.changePassword(username, req.newPassword());
        return ResponseEntity.noContent().build();
    }
}
