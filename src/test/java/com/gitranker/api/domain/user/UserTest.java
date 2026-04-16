package com.gitranker.api.domain.user;

import com.gitranker.api.domain.user.vo.ActivityStatistics;
import com.gitranker.api.domain.user.vo.RankInfo;
import com.gitranker.api.domain.user.vo.Score;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static com.gitranker.api.support.TestFixtures.user;
import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    @Test
    @DisplayName("사용자는 기본적으로 USER 역할, 0점, IRON 티어로 시작한다")
    void initializesWithDefaultRoleScoreAndTier() {
        User user = User.builder()
                .githubId(1L)
                .nodeId("node-alice")
                .username("alice")
                .build();

        assertThat(user.getRole()).isEqualTo(Role.USER);
        assertThat(user.getTotalScore()).isZero();
        assertThat(user.getTier()).isEqualTo(Tier.IRON);
        assertThat(user.isNewUser()).isTrue();
    }

    @Test
    @DisplayName("프로필 업데이트는 null과 동일값을 무시하고 변경이 있을 때만 updatedAt을 갱신한다")
    void updatesOnlyChangedProfileFields() {
        User user = user("alice");
        LocalDateTime previousUpdatedAt = user.getUpdatedAt();

        boolean unchanged = user.updateProfile("alice", null, "alice@example.com");
        boolean changed = user.updateProfile("alice-renamed", null, "alice-renamed@example.com");

        assertThat(unchanged).isFalse();
        assertThat(changed).isTrue();
        assertThat(user.getUsername()).isEqualTo("alice-renamed");
        assertThat(user.getEmail()).isEqualTo("alice-renamed@example.com");
        assertThat(user.getUpdatedAt()).isAfterOrEqualTo(previousUpdatedAt);
    }

    @Test
    @DisplayName("full scan 가능 여부는 null, 쿨다운 전, 쿨다운 이후를 구분한다")
    void determinesFullScanAvailabilityFromCooldown() {
        User user = user("alice");

        ReflectionTestUtils.setField(user, "lastFullScanAt", null);
        assertThat(user.canTriggerFullScan()).isTrue();

        ReflectionTestUtils.setField(user, "lastFullScanAt", LocalDateTime.now().minusMinutes(4));
        assertThat(user.canTriggerFullScan()).isFalse();

        ReflectionTestUtils.setField(user, "lastFullScanAt", LocalDateTime.now().minusMinutes(6));
        assertThat(user.canTriggerFullScan()).isTrue();
    }

    @Test
    @DisplayName("recordFullScan과 통계 업데이트는 점수와 랭크를 새 값으로 바꾼다")
    void recordsFullScanAndUpdatesRanking() {
        User user = user("alice");
        LocalDateTime previousUpdatedAt = user.getUpdatedAt();

        user.updateActivityStatistics(ActivityStatistics.of(10, 2, 1, 3, 4), 1L, 10L);
        user.recordFullScan();

        assertThat(user.getTotalScore()).isEqualTo(63);
        assertThat(user.getRanking()).isEqualTo(2);
        assertThat(user.getPercentile()).isEqualTo(20.0);
        assertThat(user.getUpdatedAt()).isAfterOrEqualTo(previousUpdatedAt);
        assertThat(user.getLastFullScanAt()).isNotNull();
    }

    @Test
    @DisplayName("embedded 값이 null이어도 getter는 안전한 기본값을 반환한다")
    void returnsFallbackValuesWhenEmbeddedFieldsAreNull() {
        User user = user("alice");
        ReflectionTestUtils.setField(user, "score", null);
        ReflectionTestUtils.setField(user, "rankInfo", null);

        assertThat(user.getTotalScore()).isZero();
        assertThat(user.getRanking()).isZero();
        assertThat(user.getTier()).isEqualTo(Tier.IRON);
        assertThat(user.getPercentile()).isEqualTo(100.0);
    }

    @Test
    @DisplayName("isAtLeast는 티어 ordinal 기준으로 비교한다")
    void checksTierThreshold() {
        User user = user("alice");
        user.updateScore(Score.of(2200));
        user.updateRankInfo(RankInfo.of(5, 10.0, 2200));

        assertThat(user.isAtLeast(Tier.GOLD)).isTrue();
        assertThat(user.isAtLeast(Tier.MASTER)).isFalse();
    }
}
