package com.gitranker.api.domain.auth;

import com.gitranker.api.domain.user.User;
import com.gitranker.api.global.auth.jwt.JwtProvider;
import com.gitranker.api.global.error.ErrorType;
import com.gitranker.api.global.error.exception.BusinessException;
import com.gitranker.api.global.util.CookieUtils;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProvider jwtProvider;

    @Value("${app.cookie.domain}")
    private String cookieDomain;

    @Value("${app.cookie.secure}")
    private boolean cookieSecure;

    @Transactional(readOnly = true)
    public TokenResponse refreshAccessToken(String refreshTokenValue) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenValue)
                .orElseThrow(() -> new BusinessException(ErrorType.INVALID_REFRESH_TOKEN));

        if (refreshToken.isExpired()) {
            throw new BusinessException(ErrorType.EXPIRED_REFRESH_TOKEN);
        }

        User user = refreshToken.getUser();

        String newAccessToken = jwtProvider.createAccessToken(user.getUsername(), user.getRole());

        log.info("Access Token 재발급 성공 - 사용자: {}", user.getUsername());

        return new TokenResponse(newAccessToken);
    }

    @Transactional
    public void logout(User user, String refreshTokenValue, HttpServletResponse response) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenValue)
                .orElseThrow(() -> new BusinessException(ErrorType.INVALID_REFRESH_TOKEN));

        if (!refreshToken.getUser().getId().equals(user.getId())) {
            throw new BusinessException(ErrorType.FORBIDDEN);
        }

        refreshTokenRepository.deleteByToken(refreshTokenValue);
        clearRefreshTokenCookie(response);

        log.info("로그아웃 성공 - 사용자: {}", user.getUsername());
    }

    @Transactional
    public void logoutAll(User user, HttpServletResponse response) {
        refreshTokenRepository.deleteAllByUser(user);
        clearRefreshTokenCookie(response);

        log.info("전체 로그아웃 성공 - 사용자: {}", user.getUsername());
    }

    private void clearRefreshTokenCookie(HttpServletResponse response) {
        ResponseCookie cookie = CookieUtils.createDeleteRefreshTokenCookie(cookieDomain, cookieSecure);

        response.addHeader("Set-Cookie", cookie.toString());
    }
}
