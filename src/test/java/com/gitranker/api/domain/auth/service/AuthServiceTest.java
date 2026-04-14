package com.gitranker.api.domain.auth.service;

import com.gitranker.api.domain.auth.RefreshToken;
import com.gitranker.api.domain.auth.RefreshTokenRepository;
import com.gitranker.api.domain.user.Role;
import com.gitranker.api.domain.user.User;
import com.gitranker.api.global.auth.AuthCookieManager;
import com.gitranker.api.global.auth.jwt.JwtProvider;
import com.gitranker.api.global.error.ErrorType;
import com.gitranker.api.global.error.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static com.gitranker.api.support.TestFixtures.refreshToken;
import static com.gitranker.api.support.TestFixtures.savedUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @InjectMocks
    private AuthService authService;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private RefreshTokenService refreshTokenService;
    @Mock
    private JwtProvider jwtProvider;
    @Mock
    private AuthCookieManager authCookieManager;

    @Test
    @DisplayName("유효한 refresh token이면 새 access/refresh token 쿠키를 발급한다")
    void refreshesAccessTokenForValidRefreshToken() {
        User user = savedUser(1L, "alice");
        RefreshToken refreshToken = refreshToken(user, "valid-token", validExpiry());
        HttpServletResponse response = mock(HttpServletResponse.class);

        when(refreshTokenRepository.findByToken("valid-token")).thenReturn(Optional.of(refreshToken));
        when(jwtProvider.createAccessToken("alice", Role.USER)).thenReturn("new-access-token");
        when(refreshTokenService.issueRefreshToken(user)).thenReturn("new-refresh-token");

        authService.refreshAccessToken("valid-token", response);

        verify(authCookieManager).addAccessTokenCookie(response, "new-access-token");
        verify(authCookieManager).addRefreshTokenCookie(response, "new-refresh-token");
    }

    @Test
    @DisplayName("존재하지 않는 refresh token이면 INVALID_REFRESH_TOKEN 예외가 발생한다")
    void throwsWhenRefreshTokenDoesNotExist() {
        when(refreshTokenRepository.findByToken("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refreshAccessToken("missing", mock(HttpServletResponse.class)))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorType())
                .isEqualTo(ErrorType.INVALID_REFRESH_TOKEN);
    }

    @Test
    @DisplayName("만료된 refresh token이면 삭제 후 EXPIRED_REFRESH_TOKEN 예외가 발생한다")
    void deletesExpiredRefreshTokenBeforeThrowing() {
        User user = savedUser(1L, "alice");
        RefreshToken expiredToken = refreshToken(user, "expired-token", expiredAt());

        when(refreshTokenRepository.findByToken("expired-token")).thenReturn(Optional.of(expiredToken));

        assertThatThrownBy(() -> authService.refreshAccessToken("expired-token", mock(HttpServletResponse.class)))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorType())
                .isEqualTo(ErrorType.EXPIRED_REFRESH_TOKEN);

        verify(refreshTokenRepository).delete(expiredToken);
        verify(authCookieManager, never()).addAccessTokenCookie(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    @DisplayName("본인 token으로 logout하면 token을 삭제하고 쿠키와 세션을 정리한다")
    void logsOutAndClearsSessionForMatchingUser() {
        User user = savedUser(1L, "alice");
        RefreshToken refreshToken = refreshToken(user, "valid-token", validExpiry());
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        HttpSession session = mock(HttpSession.class);

        when(refreshTokenRepository.findByToken("valid-token")).thenReturn(Optional.of(refreshToken));
        when(request.getSession(false)).thenReturn(session);

        authService.logout(user, "valid-token", request, response);

        verify(refreshTokenRepository).deleteByToken("valid-token");
        verify(authCookieManager).clearAccessTokenCookie(response);
        verify(authCookieManager).clearRefreshTokenCookie(response);
        verify(session).invalidate();
    }

    @Test
    @DisplayName("다른 사용자의 token으로 logout하면 FORBIDDEN 예외가 발생한다")
    void throwsForbiddenForOtherUsersToken() {
        User currentUser = savedUser(1L, "alice");
        User otherUser = savedUser(2L, "bob");
        RefreshToken refreshToken = refreshToken(otherUser, "foreign-token", validExpiry());

        when(refreshTokenRepository.findByToken("foreign-token")).thenReturn(Optional.of(refreshToken));

        assertThatThrownBy(() -> authService.logout(currentUser, "foreign-token", mock(HttpServletRequest.class), mock(HttpServletResponse.class)))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorType())
                .isEqualTo(ErrorType.FORBIDDEN);
    }

    @Test
    @DisplayName("logoutAll은 사용자의 모든 token과 쿠키를 정리한다")
    void logoutAllClearsAllTokensAndCookies() {
        User user = savedUser(1L, "alice");
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        HttpSession session = mock(HttpSession.class);

        when(request.getSession(false)).thenReturn(session);

        authService.logoutAll(user, request, response);

        verify(refreshTokenRepository).deleteAllByUser(user);
        verify(authCookieManager).clearAccessTokenCookie(response);
        verify(authCookieManager).clearRefreshTokenCookie(response);
        verify(session).invalidate();
    }

    private LocalDateTime validExpiry() {
        return LocalDateTime.now().plusDays(1);
    }

    private LocalDateTime expiredAt() {
        return LocalDateTime.now().minusDays(1);
    }
}
