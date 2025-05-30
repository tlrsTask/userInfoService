package org.userservice.security.jwt;

import java.time.Duration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.userservice.entity.User;
import lombok.RequiredArgsConstructor;

@Service
@ConditionalOnProperty(name = "jwt.use.refresh", havingValue = "true")
@RequiredArgsConstructor
public class TokenBlacklistService {

    private final RedisTemplate<String, String> redisTemplate;
    private final Duration refreshTokenTtl = Duration.ofDays(7);

    public void blacklist(String token) {
        redisTemplate.opsForValue().set(token, "revoked", refreshTokenTtl);
    }

    public boolean isBlacklisted(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(token));
    }
    
    public void revokeAll(User user) {
        String userKey = "user:" + user.getUserName();
        redisTemplate.opsForValue().set(userKey, "revoked", refreshTokenTtl);
    }
}
