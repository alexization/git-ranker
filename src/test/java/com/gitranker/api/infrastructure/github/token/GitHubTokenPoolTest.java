package com.gitranker.api.infrastructure.github.token;

import com.gitranker.api.global.error.exception.GitHubRateLimitExhaustedException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GitHubTokenPoolTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
    private static final int THRESHOLD = 10;

    private GitHubTokenPool createPool(String tokensConfig) {
        return new GitHubTokenPool(tokensConfig, THRESHOLD, ZONE);
    }

    @Test
    @DisplayName("단일 토큰 설정 시 해당 토큰을 반환한다")
    void should_returnToken_when_singleTokenConfigured() {
        GitHubTokenPool pool = createPool("ghp_token1");

        assertThat(pool.getToken()).isEqualTo("ghp_token1");
    }

    @Test
    @DisplayName("쉼표로 구분된 여러 토큰을 파싱한다")
    void should_returnFirstToken_when_multipleTokensConfigured() {
        GitHubTokenPool pool = createPool("ghp_token1, ghp_token2, ghp_token3");

        assertThat(pool.getToken()).isEqualTo("ghp_token1");
    }

    @Test
    @DisplayName("빈 토큰 설정이면 예외가 발생한다")
    void should_throwException_when_tokensConfigIsBlank() {
        assertThatThrownBy(() -> createPool(""))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("null 토큰 설정이면 예외가 발생한다")
    void should_throwException_when_tokensConfigIsNull() {
        assertThatThrownBy(() -> new GitHubTokenPool(null, THRESHOLD, ZONE))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("현재 토큰이 Rate Limit에 걸리면 다음 토큰으로 로테이션한다")
    void should_rotateToNextToken_when_currentTokenRateLimited() {
        GitHubTokenPool pool = createPool("ghp_token1, ghp_token2");

        // token1의 remaining을 threshold 이하로 설정 → 사용 불가
        pool.updateTokenState("ghp_token1", 5, LocalDateTime.now(ZONE).plusHours(1));

        assertThat(pool.getToken()).isEqualTo("ghp_token2");
    }

    @Test
    @DisplayName("모든 토큰이 소진되면 GitHubRateLimitExhaustedException이 발생한다")
    void should_throwExhausted_when_allTokensRateLimited() {
        GitHubTokenPool pool = createPool("ghp_token1, ghp_token2");

        pool.updateTokenState("ghp_token1", 5, LocalDateTime.now(ZONE).plusHours(1));
        pool.updateTokenState("ghp_token2", 3, LocalDateTime.now(ZONE).plusHours(1));

        assertThatThrownBy(pool::getToken)
                .isInstanceOf(GitHubRateLimitExhaustedException.class);
    }

    @Test
    @DisplayName("토큰 상태 갱신 후에도 remaining이 threshold보다 크면 사용 가능하다")
    void should_remainAvailable_when_remainingAboveThreshold() {
        GitHubTokenPool pool = createPool("ghp_token1");

        pool.updateTokenState("ghp_token1", 100, LocalDateTime.now(ZONE).plusHours(1));

        assertThat(pool.getToken()).isEqualTo("ghp_token1");
    }

    @Test
    @DisplayName("로테이션 후 다시 요청하면 마지막으로 사용한 토큰부터 탐색한다")
    void should_startFromLastUsedIndex_when_gettingTokenAfterRotation() {
        GitHubTokenPool pool = createPool("ghp_token1, ghp_token2, ghp_token3");

        // token1 소진 → token2로 로테이션
        pool.updateTokenState("ghp_token1", 5, LocalDateTime.now(ZONE).plusHours(1));
        assertThat(pool.getToken()).isEqualTo("ghp_token2");

        // 다음 호출도 token2부터 시작 (token2가 아직 사용 가능하므로 token2 반환)
        assertThat(pool.getToken()).isEqualTo("ghp_token2");
    }
}
