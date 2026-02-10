package com.gitranker.api.global.auth.jwt;

import com.gitranker.api.domain.user.Role;
import com.gitranker.api.global.logging.Event;
import com.gitranker.api.global.logging.LogContext;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Date;

@Component
public class JwtProvider {

    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_TYPE = "type";
    private static final String TOKEN_TYPE_ACCESS = "access";

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpirationMs;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpirationMs;

    private SecretKey key;

    @PostConstruct
    public void init() {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    public String createAccessToken(String username, Role role) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + accessTokenExpirationMs);

        return Jwts.builder()
                .subject(username)
                .claim(CLAIM_ROLE, role.getKey())
                .claim(CLAIM_TYPE, TOKEN_TYPE_ACCESS)
                .issuedAt(now)
                .expiration(validity)
                .signWith(key, Jwts.SIG.HS512)
                .compact();
    }

    public String createRefreshToken() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);

        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    public LocalDateTime calculateRefreshTokenExpiry() {
        return LocalDateTime.now().plusSeconds(refreshTokenExpirationMs / 1000);
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (SecurityException | MalformedJwtException e) {
            LogContext.event(Event.AUTH_FAILED)
                    .with("error_type", "INVALID_SIGNATURE")
                    .with("error_message", e.getMessage())
                    .warn();
        } catch (ExpiredJwtException e) {
            LogContext.event(Event.AUTH_FAILED)
                    .with("error_type", "EXPIRED_TOKEN")
                    .with("error_message", e.getMessage())
                    .info();
        } catch (UnsupportedJwtException e) {
            LogContext.event(Event.AUTH_FAILED)
                    .with("error_type", "UNSUPPORTED_TOKEN")
                    .with("error_message", e.getMessage())
                    .warn();
        } catch (IllegalArgumentException e) {
            LogContext.event(Event.AUTH_FAILED)
                    .with("error_type", "MALFORMED_TOKEN")
                    .with("error_message", e.getMessage())
                    .warn();
        }
        return false;
    }

    public boolean isAccessToken(String token) {
        try {
            Claims claims = parseClaims(token);
            String type = claims.get(CLAIM_TYPE, String.class);

            return TOKEN_TYPE_ACCESS.equals(type);
        } catch (Exception e) {
            return false;
        }
    }

    public String getUsername(String token) {
        return parseClaims(token).getSubject();
    }

    private Claims parseClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            return e.getClaims();
        }
    }
}
