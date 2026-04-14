package com.gitranker.api.domain.auth.service;

import com.gitranker.api.domain.auth.RefreshToken;
import com.gitranker.api.domain.auth.RefreshTokenRepository;
import com.gitranker.api.domain.user.User;
import com.gitranker.api.global.auth.jwt.JwtProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static com.gitranker.api.support.TestFixtures.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private JwtProvider jwtProvider;

    @Test
    @DisplayName("refresh token 발급 시 기존 token을 지우고 새 token을 저장한다")
    void issuesNewRefreshTokenAfterDeletingOldOnes() {
        User user = user("alice");
        when(jwtProvider.createRefreshToken()).thenReturn("new-token");
        when(jwtProvider.calculateRefreshTokenExpiry()).thenReturn(LocalDateTime.of(2025, 1, 10, 12, 0));

        String token = refreshTokenService.issueRefreshToken(user);

        assertThat(token).isEqualTo("new-token");
        verify(refreshTokenRepository).deleteAllByUser(user);
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("저장되는 refresh token에는 사용자와 만료 시각이 함께 들어간다")
    void savesRefreshTokenWithExpectedFields() {
        User user = user("alice");
        LocalDateTime expiry = LocalDateTime.of(2025, 1, 10, 12, 0);
        when(jwtProvider.createRefreshToken()).thenReturn("new-token");
        when(jwtProvider.calculateRefreshTokenExpiry()).thenReturn(expiry);

        refreshTokenService.issueRefreshToken(user);

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());

        RefreshToken savedToken = captor.getValue();
        assertThat(savedToken.getToken()).isEqualTo("new-token");
        assertThat(savedToken.getUser()).isEqualTo(user);
        assertThat(savedToken.getExpiresAt()).isEqualTo(expiry);
    }
}
