package com.gitranker.api.domain.auth.service;

import com.gitranker.api.domain.auth.RefreshToken;
import com.gitranker.api.domain.auth.RefreshTokenRepository;
import com.gitranker.api.domain.user.User;
import com.gitranker.api.global.auth.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProvider jwtProvider;

    @Transactional
    public String issueRefreshToken(User user) {
        refreshTokenRepository.deleteAllByUser(user);

        String tokenValue = jwtProvider.createRefreshToken();

        RefreshToken refreshToken = RefreshToken.builder()
                .token(tokenValue)
                .user(user)
                .expiresAt(jwtProvider.calculateRefreshTokenExpiry())
                .build();

        refreshTokenRepository.save(refreshToken);

        return tokenValue;
    }
}
