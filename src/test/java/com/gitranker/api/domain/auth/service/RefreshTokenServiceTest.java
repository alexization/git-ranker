package com.gitranker.api.domain.auth.service;

import com.gitranker.api.domain.auth.RefreshToken;
import com.gitranker.api.domain.auth.RefreshTokenRepository;
import com.gitranker.api.domain.user.Role;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private JwtProvider jwtProvider;

    private User createUser() {
        return User.builder()
                .githubId(1L)
                .nodeId("node1")
                .username("testuser")
                .role(Role.USER)
                .build();
    }

    @Test
    @DisplayName("토큰 발급 시 기존 토큰을 삭제하고 새 토큰을 저장한다")
    void should_deleteOldAndSaveNew_when_issuingToken() {
        User user = createUser();
        when(jwtProvider.createRefreshToken()).thenReturn("new-token-value");
        when(jwtProvider.calculateRefreshTokenExpiry()).thenReturn(LocalDateTime.now().plusDays(7));

        String tokenValue = refreshTokenService.issueRefreshToken(user);

        assertThat(tokenValue).isEqualTo("new-token-value");
        verify(refreshTokenRepository).deleteAllByUser(user);
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("저장되는 토큰에 올바른 사용자와 만료 시간이 설정된다")
    void should_setCorrectUserAndExpiry_when_saving() {
        User user = createUser();
        LocalDateTime expiry = LocalDateTime.now().plusDays(7);
        when(jwtProvider.createRefreshToken()).thenReturn("token-123");
        when(jwtProvider.calculateRefreshTokenExpiry()).thenReturn(expiry);

        refreshTokenService.issueRefreshToken(user);

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());

        RefreshToken savedToken = captor.getValue();
        assertThat(savedToken.getToken()).isEqualTo("token-123");
        assertThat(savedToken.getUser()).isEqualTo(user);
        assertThat(savedToken.getExpiresAt()).isEqualTo(expiry);
    }
}
