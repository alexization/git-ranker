package com.gitranker.api.global.auth.jwt;

import com.gitranker.api.domain.user.Role;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.time.LocalDateTime;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class JwtProviderTest {

    private JwtProvider jwtProvider;

    private static final String TEST_SECRET = Base64.getEncoder().encodeToString(
            "test-secret-key-for-jwt-must-be-at-least-64-bytes-long-for-hs512-algorithm-padding".getBytes()
    );
    private static final long ACCESS_TOKEN_EXPIRATION = 3600000L;  // 1시간
    private static final long REFRESH_TOKEN_EXPIRATION = 604800000L;  // 7일

    @BeforeEach
    void setUp() {
        jwtProvider = new JwtProvider();
        ReflectionTestUtils.setField(jwtProvider, "secret", TEST_SECRET);
        ReflectionTestUtils.setField(jwtProvider, "accessTokenExpirationMs", ACCESS_TOKEN_EXPIRATION);
        ReflectionTestUtils.setField(jwtProvider, "refreshTokenExpirationMs", REFRESH_TOKEN_EXPIRATION);
        jwtProvider.init();
    }

    @Nested
    @DisplayName("createAccessToken")
    class CreateAccessToken {

        @Test
        @DisplayName("유효한 JWT를 생성한다")
        void should_createValidJwt() {
            String token = jwtProvider.createAccessToken("testuser", Role.USER);

            assertThat(token).isNotNull();
            assertThat(jwtProvider.validateToken(token)).isTrue();
        }

        @Test
        @DisplayName("토큰에 올바른 username이 포함된다")
        void should_containCorrectUsername() {
            String token = jwtProvider.createAccessToken("testuser", Role.USER);

            assertThat(jwtProvider.getUsername(token)).isEqualTo("testuser");
        }

        @Test
        @DisplayName("생성된 토큰은 access 타입이다")
        void should_beAccessTokenType() {
            String token = jwtProvider.createAccessToken("testuser", Role.USER);

            assertThat(jwtProvider.isAccessToken(token)).isTrue();
        }
    }

    @Nested
    @DisplayName("validateToken")
    class ValidateToken {

        @Test
        @DisplayName("유효한 토큰이면 true를 반환한다")
        void should_returnTrue_when_validToken() {
            String token = jwtProvider.createAccessToken("testuser", Role.USER);

            assertThat(jwtProvider.validateToken(token)).isTrue();
        }

        @Test
        @DisplayName("만료된 토큰이면 false를 반환한다")
        void should_returnFalse_when_expiredToken() {
            ReflectionTestUtils.setField(jwtProvider, "accessTokenExpirationMs", -1000L);
            String expiredToken = jwtProvider.createAccessToken("testuser", Role.USER);

            assertThat(jwtProvider.validateToken(expiredToken)).isFalse();

            // 원복
            ReflectionTestUtils.setField(jwtProvider, "accessTokenExpirationMs", ACCESS_TOKEN_EXPIRATION);
        }

        @Test
        @DisplayName("변조된 토큰이면 false를 반환한다")
        void should_returnFalse_when_tamperedToken() {
            String token = jwtProvider.createAccessToken("testuser", Role.USER);
            String tampered = token.substring(0, token.length() - 5) + "xxxxx";

            assertThat(jwtProvider.validateToken(tampered)).isFalse();
        }

        @Test
        @DisplayName("다른 키로 서명된 토큰이면 false를 반환한다")
        void should_returnFalse_when_differentSecretKey() {
            // 별도 키로 서명한 토큰 생성
            String otherSecret = Base64.getEncoder().encodeToString(
                    "another-secret-key-for-test-must-be-at-least-64-bytes-long-for-hs512-algorithm-pad".getBytes()
            );
            JwtProvider otherProvider = new JwtProvider();
            ReflectionTestUtils.setField(otherProvider, "secret", otherSecret);
            ReflectionTestUtils.setField(otherProvider, "accessTokenExpirationMs", ACCESS_TOKEN_EXPIRATION);
            ReflectionTestUtils.setField(otherProvider, "refreshTokenExpirationMs", REFRESH_TOKEN_EXPIRATION);
            otherProvider.init();

            String tokenFromOtherKey = otherProvider.createAccessToken("testuser", Role.USER);

            assertThat(jwtProvider.validateToken(tokenFromOtherKey)).isFalse();
        }

        @Test
        @DisplayName("빈 문자열이면 false를 반환한다")
        void should_returnFalse_when_emptyToken() {
            assertThat(jwtProvider.validateToken("")).isFalse();
        }

        @Test
        @DisplayName("형식이 잘못된 문자열이면 false를 반환한다")
        void should_returnFalse_when_malformedToken() {
            assertThat(jwtProvider.validateToken("not.a.jwt")).isFalse();
        }
    }

    @Nested
    @DisplayName("isAccessToken")
    class IsAccessToken {

        @Test
        @DisplayName("access 토큰이면 true를 반환한다")
        void should_returnTrue_when_accessToken() {
            String token = jwtProvider.createAccessToken("testuser", Role.USER);

            assertThat(jwtProvider.isAccessToken(token)).isTrue();
        }

        @Test
        @DisplayName("유효하지 않은 토큰이면 false를 반환한다")
        void should_returnFalse_when_invalidToken() {
            assertThat(jwtProvider.isAccessToken("invalid")).isFalse();
        }
    }

    @Nested
    @DisplayName("createRefreshToken")
    class CreateRefreshToken {

        @Test
        @DisplayName("null이 아닌 토큰을 생성한다")
        void should_createNonNullToken() {
            String token = jwtProvider.createRefreshToken();

            assertThat(token).isNotBlank();
        }

        @Test
        @DisplayName("매 호출마다 다른 토큰을 생성한다")
        void should_createUniqueTokens() {
            String token1 = jwtProvider.createRefreshToken();
            String token2 = jwtProvider.createRefreshToken();

            assertThat(token1).isNotEqualTo(token2);
        }
    }

    @Nested
    @DisplayName("calculateRefreshTokenExpiry")
    class CalculateRefreshTokenExpiry {

        @Test
        @DisplayName("현재 시간 이후의 만료 시간을 반환한다")
        void should_returnFutureDateTime() {
            LocalDateTime before = LocalDateTime.now();
            LocalDateTime expiry = jwtProvider.calculateRefreshTokenExpiry();

            assertThat(expiry).isAfter(before);
        }
    }
}
