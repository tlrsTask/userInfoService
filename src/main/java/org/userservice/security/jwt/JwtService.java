package org.userservice.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Сервис для работы с JWT-токенами: генерация, валидация и извлечение данных из токенов.
 * <p>
 * Использует два типа токенов:
 * <ul>
 *     <li>Access Token — для аутентификации и авторизации пользователей.</li>
 *     <li>Refresh Token — для обновления Access Token (опционально, включается в настройках).</li>
 * </ul>
 * <p>
 * Секретные ключи и время жизни токенов берутся из настроек {@link JwtProperties}.
 * <p>
 * Для корректной работы секретный ключ должен быть не менее 256 бит (32 символа).
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class JwtService {

    private final JwtProperties jwtProperties;

    private SecretKey accessSecretKey;
    private SecretKey refreshSecretKey;

    /**
     * Инициализация секретных ключей для access и refresh токенов на основе настроек.
     * Проверяет минимальную длину секретов.
     */
    @PostConstruct
    public void init() {
    	 validateSecret(jwtProperties.getAccess().getSecret());
    	    this.accessSecretKey = Keys.hmacShaKeyFor(
    	        jwtProperties.getAccess().getSecret().getBytes(StandardCharsets.UTF_8)
    	    );
        
        if (jwtProperties.isUseRefresh()) {
            this.refreshSecretKey = Keys.hmacShaKeyFor(
                jwtProperties.getRefresh().getSecret().getBytes(StandardCharsets.UTF_8)
            );
        }
    }

    /**
     * Генерирует JWT access токен для указанного аутентифицированного пользователя.
     *
     * @param authentication объект аутентификации пользователя
     * @return сгенерированный JWT access токен
     */
    public String generateAccessToken(Authentication authentication) {
        log.info("Generating token for user: " + authentication.getName());
        String token = buildToken(authentication, accessSecretKey, jwtProperties.getAccess().getExpiration());
        log.info("Generated token: " + token);
        return token;
    }

    /**
     * Генерирует JWT refresh токен для указанного аутентифицированного пользователя.
     *
     * @param authentication объект аутентификации пользователя
     * @return сгенерированный JWT refresh токен
     * @throws UnsupportedOperationException если refresh токены отключены в настройках
     */
    public String generateRefreshToken(Authentication authentication) {
    	if (!jwtProperties.isUseRefresh()) {
            throw new UnsupportedOperationException("Refresh tokens are disabled");
        }
        return buildToken(authentication, refreshSecretKey, jwtProperties.getRefresh().getExpiration());
    }

    /**
     * Строит JWT токен с заданным временем жизни и секретным ключом.
     *
     * @param authentication объект аутентификации пользователя
     * @param secretKey      секретный ключ для подписи токена
     * @param expiration     время жизни токена
     * @return сгенерированный JWT токен в виде строки
     * @throws IllegalArgumentException если authentication или secretKey равны null
     */
    private String buildToken(Authentication authentication, SecretKey secretKey, Duration expiration) {
    	if (authentication == null || authentication.getName() == null) {
    	    throw new IllegalArgumentException("Authentication cannot be null");
    	}
    	if (secretKey == null) {
    	    throw new IllegalArgumentException("SecretKey cannot be null");
    	}
    	Instant now = Instant.now();
        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
        
        return Jwts.builder()
                .subject(authentication.getName())
                .claim("roles", roles)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expiration)))
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Проверяет валидность JWT токена (access или refresh).
     *
     * @param token         JWT токен
     * @param isAccessToken true для проверки access токена, false — refresh токена
     * @return true, если токен валиден, false — если нет или истёк срок действия
     */
    public boolean validate(String token, boolean isAccessToken) {
        try {
            SecretKey key = isAccessToken ? accessSecretKey : refreshSecretKey;
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Проверяет минимальную длину секретного ключа.
     *
     * @param secret секретный ключ в виде строки
     * @throws IllegalArgumentException если длина ключа меньше 32 символов
     */
    private void validateSecret(String secret) {
        if (secret == null || secret.length() < 32) {
            throw new IllegalArgumentException("Secret key must be at least 256 bits (32 chars)");
        }
    }

    /**
     * Проверяет валидность refresh токена.
     *
     * @param token JWT refresh токен
     * @return true, если refresh токен валиден, иначе false
     */
    public boolean validateRefreshToken(String token) {
        return validate(token, false);
    }

    /**
     * Извлекает Claims (полезные данные) из refresh токена.
     *
     * @param token JWT refresh токен
     * @return объект Claims с полезной нагрузкой токена
     */
    public Claims extractRefreshClaims(String token) {
        return Jwts.parser()
                .verifyWith(refreshSecretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Извлекает имя пользователя (subject) из JWT токена.
     *
     * @param token         JWT токен
     * @param isAccessToken true, если токен — access, false — refresh
     * @return имя пользователя из токена
     */
    public String extractUsername(String token, boolean isAccessToken) {
        SecretKey key = isAccessToken ? accessSecretKey : refreshSecretKey;
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload().getSubject();
    }

    /**
     * Извлекает список ролей из JWT токена в виде списка GrantedAuthority.
     *
     * @param token         JWT токен
     * @param isAccessToken true, если токен — access, false — refresh
     * @return список ролей пользователя из токена
     */
    public List<SimpleGrantedAuthority> extractAuthorities(String token, boolean isAccessToken) {
        SecretKey key = isAccessToken ? accessSecretKey : refreshSecretKey;
        Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        List<String> roles = claims.get("roles", List.class);
        return roles.stream().map(SimpleGrantedAuthority::new).toList();
    }
}
