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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @InjectMocks
    private AuthService authService;

    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private JwtProvider jwtProvider;
    @Mock private AuthCookieManager authCookieManager;

    private RefreshToken createValidRefreshToken(User user) {
        return RefreshToken.builder()
                .token("valid-token")
                .user(user)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();
    }

    private RefreshToken createExpiredRefreshToken(User user) {
        return RefreshToken.builder()
                .token("expired-token")
                .user(user)
                .expiresAt(LocalDateTime.now().minusDays(1))
                .build();
    }

    @Nested
    @DisplayName("refreshAccessToken")
    class RefreshAccessToken {

        @Test
        @DisplayName("유효한 토큰이면 새 Access/Refresh 토큰을 발급한다")
        void should_issueNewTokens_when_validRefreshToken() {
            User user = mock(User.class);
            when(user.getUsername()).thenReturn("testuser");
            when(user.getRole()).thenReturn(Role.USER);
            RefreshToken refreshToken = createValidRefreshToken(user);
            HttpServletResponse response = mock(HttpServletResponse.class);

            when(refreshTokenRepository.findByToken("valid-token")).thenReturn(Optional.of(refreshToken));
            when(jwtProvider.createAccessToken("testuser", Role.USER)).thenReturn("new-access-token");
            when(refreshTokenService.issueRefreshToken(user)).thenReturn("new-refresh-token");

            authService.refreshAccessToken("valid-token", response);

            verify(authCookieManager).addAccessTokenCookie(response, "new-access-token");
            verify(authCookieManager).addRefreshTokenCookie(response, "new-refresh-token");
        }

        @Test
        @DisplayName("존재하지 않는 토큰이면 INVALID_REFRESH_TOKEN 예외가 발생한다")
        void should_throwInvalidToken_when_tokenNotFound() {
            when(refreshTokenRepository.findByToken("unknown")).thenReturn(Optional.empty());
            HttpServletResponse response = mock(HttpServletResponse.class);

            assertThatThrownBy(() -> authService.refreshAccessToken("unknown", response))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorType())
                            .isEqualTo(ErrorType.INVALID_REFRESH_TOKEN));
        }

        @Test
        @DisplayName("만료된 토큰이면 삭제 후 EXPIRED_REFRESH_TOKEN 예외가 발생한다")
        void should_deleteAndThrow_when_tokenExpired() {
            User user = mock(User.class);
            RefreshToken expiredToken = createExpiredRefreshToken(user);
            HttpServletResponse response = mock(HttpServletResponse.class);

            when(refreshTokenRepository.findByToken("expired-token")).thenReturn(Optional.of(expiredToken));

            assertThatThrownBy(() -> authService.refreshAccessToken("expired-token", response))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorType())
                            .isEqualTo(ErrorType.EXPIRED_REFRESH_TOKEN));

            verify(refreshTokenRepository).delete(expiredToken);
        }
    }

    @Nested
    @DisplayName("logout")
    class Logout {

        @Test
        @DisplayName("본인의 토큰으로 로그아웃하면 토큰을 삭제하고 쿠키를 정리한다")
        void should_deleteTokenAndClearCookies_when_validLogout() {
            User user = mock(User.class);
            when(user.getId()).thenReturn(1L);
            when(user.getUsername()).thenReturn("testuser");
            RefreshToken refreshToken = createValidRefreshToken(user);
            HttpServletRequest request = mock(HttpServletRequest.class);
            HttpServletResponse response = mock(HttpServletResponse.class);

            when(refreshTokenRepository.findByToken("valid-token")).thenReturn(Optional.of(refreshToken));
            when(request.getSession(false)).thenReturn(null);

            authService.logout(user, "valid-token", request, response);

            verify(refreshTokenRepository).deleteByToken("valid-token");
            verify(authCookieManager).clearAccessTokenCookie(response);
            verify(authCookieManager).clearRefreshTokenCookie(response);
        }

        @Test
        @DisplayName("다른 사용자의 토큰으로 로그아웃하면 FORBIDDEN 예외가 발생한다")
        void should_throwForbidden_when_logoutWithOtherUsersToken() {
            User currentUser = mock(User.class);
            when(currentUser.getId()).thenReturn(1L);
            User otherUser = mock(User.class);
            when(otherUser.getId()).thenReturn(2L);
            RefreshToken otherToken = createValidRefreshToken(otherUser);
            HttpServletRequest request = mock(HttpServletRequest.class);
            HttpServletResponse response = mock(HttpServletResponse.class);

            when(refreshTokenRepository.findByToken("valid-token")).thenReturn(Optional.of(otherToken));

            assertThatThrownBy(() -> authService.logout(currentUser, "valid-token", request, response))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorType())
                            .isEqualTo(ErrorType.FORBIDDEN));
        }

        @Test
        @DisplayName("세션이 있으면 무효화한다")
        void should_invalidateSession_when_sessionExists() {
            User user = mock(User.class);
            when(user.getId()).thenReturn(1L);
            when(user.getUsername()).thenReturn("testuser");
            RefreshToken refreshToken = createValidRefreshToken(user);
            HttpServletRequest request = mock(HttpServletRequest.class);
            HttpServletResponse response = mock(HttpServletResponse.class);
            HttpSession session = mock(HttpSession.class);

            when(refreshTokenRepository.findByToken("valid-token")).thenReturn(Optional.of(refreshToken));
            when(request.getSession(false)).thenReturn(session);

            authService.logout(user, "valid-token", request, response);

            verify(session).invalidate();
        }
    }

    @Nested
    @DisplayName("logoutAll")
    class LogoutAll {

        @Test
        @DisplayName("모든 토큰을 삭제하고 쿠키를 정리한다")
        void should_deleteAllTokensAndClearCookies() {
            User user = mock(User.class);
            when(user.getUsername()).thenReturn("testuser");
            HttpServletRequest request = mock(HttpServletRequest.class);
            HttpServletResponse response = mock(HttpServletResponse.class);

            when(request.getSession(false)).thenReturn(null);

            authService.logoutAll(user, request, response);

            verify(refreshTokenRepository).deleteAllByUser(user);
            verify(authCookieManager).clearAccessTokenCookie(response);
            verify(authCookieManager).clearRefreshTokenCookie(response);
        }
    }
}
