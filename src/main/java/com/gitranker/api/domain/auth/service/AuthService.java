package com.gitranker.api.domain.auth.service;

import com.gitranker.api.domain.auth.RefreshToken;
import com.gitranker.api.domain.auth.RefreshTokenRepository;
import com.gitranker.api.domain.user.User;
import com.gitranker.api.global.auth.AuthCookieManager;
import com.gitranker.api.global.auth.jwt.JwtProvider;
import com.gitranker.api.global.error.ErrorType;
import com.gitranker.api.global.error.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenService refreshTokenService;
    private final JwtProvider jwtProvider;
    private final AuthCookieManager authCookieManager;

    @Transactional
    public void refreshAccessToken(String refreshTokenValue, HttpServletResponse response) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenValue)
                .orElseThrow(() -> new BusinessException(ErrorType.INVALID_REFRESH_TOKEN));

        if (refreshToken.isExpired()) {
            refreshTokenRepository.delete(refreshToken);
            throw new BusinessException(ErrorType.EXPIRED_REFRESH_TOKEN);
        }

        User user = refreshToken.getUser();

        String newAccessToken = jwtProvider.createAccessToken(user.getUsername(), user.getRole());
        authCookieManager.addAccessTokenCookie(response, newAccessToken);

        String newRefreshTokenValue = refreshTokenService.issueRefreshToken(user);
        authCookieManager.addRefreshTokenCookie(response, newRefreshTokenValue);

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
        authCookieManager.clearAccessTokenCookie(response);
        authCookieManager.clearRefreshTokenCookie(response);
        invalidateSession(request);

        log.info("로그아웃 성공 - 사용자: {}", user.getUsername());
    }

    @Transactional
    public void logoutAll(User user, HttpServletResponse response) {
        refreshTokenRepository.deleteAllByUser(user);
        authCookieManager.clearAccessTokenCookie(response);
        authCookieManager.clearRefreshTokenCookie(response);

        log.info("전체 로그아웃 성공 - 사용자: {}", user.getUsername());
    }

    private void invalidateSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }
}
