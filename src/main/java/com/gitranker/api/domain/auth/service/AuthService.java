package com.gitranker.api.domain.auth.service;

import com.gitranker.api.domain.auth.RefreshToken;
import com.gitranker.api.domain.auth.RefreshTokenRepository;
import com.gitranker.api.domain.user.User;
import com.gitranker.api.global.auth.jwt.JwtProvider;
import com.gitranker.api.global.error.ErrorType;
import com.gitranker.api.global.error.exception.BusinessException;
import com.gitranker.api.global.util.CookieUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenService refreshTokenService;
    private final JwtProvider jwtProvider;

    @Value("${app.cookie.domain}")
    private String cookieDomain;

    @Value("${app.cookie.secure}")
    private boolean isCookieSecure;

    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpirationMs;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpirationMs;

    @Transactional
    public void refreshAccessToken(String refreshTokenValue, HttpServletResponse response) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenValue)
                .orElseThrow(() -> new BusinessException(ErrorType.INVALID_REFRESH_TOKEN));

        if (refreshToken.isExpired()) {
            throw new BusinessException(ErrorType.EXPIRED_REFRESH_TOKEN);
        }

        User user = refreshToken.getUser();

        String newAccessToken = jwtProvider.createAccessToken(user.getUsername(), user.getRole());
        addAccessTokenCookie(response, newAccessToken);

        String newRefreshTokenValue = refreshTokenService.issueRefreshToken(user);
        addRefreshTokenCookie(response, newRefreshTokenValue);

        log.info("토큰 갱신 성공 - 사용자: {}", user.getUsername());
    }

    @Transactional
    public void logout(User user, String refreshTokenValue, HttpServletRequest request, HttpServletResponse response) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenValue)
                .orElseThrow(() -> new BusinessException(ErrorType.INVALID_REFRESH_TOKEN));

        if (!refreshToken.getUser().getId().equals(user.getId())) {
            throw new BusinessException(ErrorType.FORBIDDEN);
        }

        refreshTokenRepository.deleteByToken(refreshTokenValue);
        clearAccessTokenCookie(response);
        clearRefreshTokenCookie(response);
        invalidateSession(request);

        log.info("로그아웃 성공 - 사용자: {}", user.getUsername());
    }

    @Transactional
    public void logoutAll(User user, HttpServletResponse response) {
        refreshTokenRepository.deleteAllByUser(user);
        clearAccessTokenCookie(response);
        clearRefreshTokenCookie(response);

        log.info("전체 로그아웃 성공 - 사용자: {}", user.getUsername());
    }

    private void addAccessTokenCookie(HttpServletResponse response, String accessToken) {
        ResponseCookie cookie = CookieUtils.createAccessTokenCookie(
                accessToken, cookieDomain, isCookieSecure, Duration.ofMillis(accessTokenExpirationMs));

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void addRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        ResponseCookie cookie = CookieUtils.createRefreshTokenCookie(
                refreshToken, cookieDomain, isCookieSecure, Duration.ofMillis(refreshTokenExpirationMs));

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearAccessTokenCookie(HttpServletResponse response) {
        ResponseCookie cookie = CookieUtils.createDeleteAccessTokenCookie(cookieDomain, isCookieSecure);

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearRefreshTokenCookie(HttpServletResponse response) {
        ResponseCookie cookie = CookieUtils.createDeleteRefreshTokenCookie(cookieDomain, isCookieSecure);

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void invalidateSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }
}
