package com.gitranker.api.global.util;

import com.gitranker.api.global.error.ErrorType;
import com.gitranker.api.global.error.exception.BusinessException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseCookie;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;

public class CookieUtils {

    public static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";
    public static final String ACCESS_TOKEN_COOKIE_NAME = "accessToken";

    private CookieUtils() {
    }

    /**
     * Access Token 쿠키 생성.
     * SameSite=Lax: OAuth2 리다이렉트(GitHub → 우리 사이트) 시 쿠키 전송을 허용합니다.
     * SameSite=Strict는 cross-site navigation에서 쿠키를 전송하지 않아 로그인이 실패합니다.
     */
    public static ResponseCookie createAccessTokenCookie(
            String tokenValue,
            String domain,
            boolean secure,
            Duration maxAge
    ) {
        return ResponseCookie.from(ACCESS_TOKEN_COOKIE_NAME, tokenValue)
                .httpOnly(true)
                .secure(secure)
                .path("/")
                .maxAge(maxAge)
                .domain(domain)
                .sameSite("Lax")
                .build();
    }

    public static ResponseCookie createDeleteAccessTokenCookie(String domain, boolean secure) {
        return createAccessTokenCookie("", domain, secure, Duration.ZERO);
    }

    public static ResponseCookie createRefreshTokenCookie(
            String tokenValue,
            String domain,
            boolean secure,
            Duration maxAge
    ) {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, tokenValue)
                .httpOnly(true)
                .secure(secure)
                .path("/")
                .maxAge(maxAge)
                .domain(domain)
                .sameSite("Lax")
                .build();
    }

    public static ResponseCookie createDeleteRefreshTokenCookie(String domain, boolean secure) {
        return createRefreshTokenCookie("", domain, secure, Duration.ZERO);
    }

    public static Optional<String> getCookieValue(HttpServletRequest request, String cookieName) {
        if (request.getCookies() == null) {
            return Optional.empty();
        }

        return Arrays.stream(request.getCookies())
                .filter(cookie -> cookieName.equals(cookie.getName()))
                .findFirst()
                .map(Cookie::getValue);
    }

    public static String extractRefreshToken(HttpServletRequest request) {
        return getCookieValue(request, REFRESH_TOKEN_COOKIE_NAME)
                .orElseThrow(() -> new BusinessException(ErrorType.INVALID_REFRESH_TOKEN));
    }
}
