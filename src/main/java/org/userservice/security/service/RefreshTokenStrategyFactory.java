package org.userservice.security.service;

import lombok.RequiredArgsConstructor;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.userservice.security.interfaces.RefreshTokenStrategy;

@Component
@RequiredArgsConstructor
public class RefreshTokenStrategyFactory {

	 private final List<RefreshTokenStrategy> strategies;
	    
	    @Value("${jwt.use.refresh}")
	    private boolean useRefreshToken;

	    public RefreshTokenStrategy getStrategy() {
	        return strategies.stream()
	            .filter(s -> s.supports(useRefreshToken))
	            .findFirst()
	            .orElseThrow(() -> new IllegalStateException("No strategy found for refresh token"));
	    }
}