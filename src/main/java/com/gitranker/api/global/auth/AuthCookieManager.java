package com.gitranker.api.global.auth;

import com.gitranker.api.global.util.CookieUtils;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class AuthCookieManager {

    @Value("${app.cookie.domain}")
    private String cookieDomain;

    @Value("${app.cookie.secure}")
    private boolean isCookieSecure;

    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpirationMs;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpirationMs;

    public void addAccessTokenCookie(HttpServletResponse response, String accessToken) {
        ResponseCookie cookie = CookieUtils.createAccessTokenCookie(
                accessToken, cookieDomain, isCookieSecure, Duration.ofMillis(accessTokenExpirationMs));

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public void addRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        ResponseCookie cookie = CookieUtils.createRefreshTokenCookie(
                refreshToken, cookieDomain, isCookieSecure, Duration.ofMillis(refreshTokenExpirationMs));

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public void clearAccessTokenCookie(HttpServletResponse response) {
        ResponseCookie cookie = CookieUtils.createDeleteAccessTokenCookie(cookieDomain, isCookieSecure);

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public void clearRefreshTokenCookie(HttpServletResponse response) {
        ResponseCookie cookie = CookieUtils.createDeleteRefreshTokenCookie(cookieDomain, isCookieSecure);

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
