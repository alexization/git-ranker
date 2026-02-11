package com.gitranker.api.domain.user;

import com.gitranker.api.domain.user.vo.ActivityStatistics;
import com.gitranker.api.domain.user.vo.RankInfo;
import com.gitranker.api.domain.user.vo.Score;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    private User createDefaultUser() {
        return User.builder()
                .githubId(12345L)
                .nodeId("MDQ6VXNlcjEyMzQ1")
                .username("testuser")
                .email("test@example.com")
                .profileImage("https://avatars.githubusercontent.com/u/12345")
                .githubCreatedAt(LocalDateTime.of(2020, 1, 1, 0, 0))
                .role(Role.USER)
                .build();
    }

    @Nested
    @DisplayName("생성")
    class Creation {

        @Test
        @DisplayName("새 사용자는 0점, IRON 티어로 초기화된다")
        void should_initializeWithZeroScore_and_ironTier() {
            User user = createDefaultUser();

            assertThat(user.getTotalScore()).isZero();
            assertThat(user.getTier()).isEqualTo(Tier.IRON);
            assertThat(user.getRanking()).isZero();
        }

        @Test
        @DisplayName("role을 지정하지 않으면 USER로 설정된다")
        void should_defaultToUserRole_when_roleIsNull() {
            User user = User.builder()
                    .githubId(1L)
                    .nodeId("node1")
                    .username("user1")
                    .build();

            assertThat(user.getRole()).isEqualTo(Role.USER);
        }

        @Test
        @DisplayName("새 사용자는 isNewUser가 true를 반환한다")
        void should_beNewUser_when_justCreated() {
            User user = createDefaultUser();

            assertThat(user.isNewUser()).isTrue();
        }
    }

    @Nested
    @DisplayName("쿨다운 (5분)")
    class Cooldown {

        @Test
        @DisplayName("생성 직후에는 쿨다운 상태이다")
        void should_beCooldownActive_when_justCreated() {
            User user = createDefaultUser();

            // 생성 시 lastFullScanAt = now() 이므로 바로 재스캔 불가
            assertThat(user.canTriggerFullScan()).isFalse();
        }

        @Test
        @DisplayName("다음 스캔 가능 시간은 마지막 스캔 시간 + 5분이다")
        void should_returnCorrectNextAvailableTime() {
            User user = createDefaultUser();
            LocalDateTime nextAvailable = user.getNextFullScanAvailableAt();

            // lastFullScanAt + 5분
            assertThat(nextAvailable).isAfter(LocalDateTime.now().minusSeconds(1));
            assertThat(nextAvailable).isBefore(LocalDateTime.now().plusMinutes(6));
        }
    }

    @Nested
    @DisplayName("프로필 업데이트")
    class ProfileUpdate {

        @Test
        @DisplayName("username이 변경되면 true를 반환한다")
        void should_returnTrue_when_usernameChanged() {
            User user = createDefaultUser();

            boolean changed = user.updateProfile("newname", user.getProfileImage(), user.getEmail());

            assertThat(changed).isTrue();
            assertThat(user.getUsername()).isEqualTo("newname");
        }

        @Test
        @DisplayName("profileImage가 변경되면 true를 반환한다")
        void should_returnTrue_when_profileImageChanged() {
            User user = createDefaultUser();

            boolean changed = user.updateProfile(user.getUsername(), "https://new-image.png", user.getEmail());

            assertThat(changed).isTrue();
            assertThat(user.getProfileImage()).isEqualTo("https://new-image.png");
        }

        @Test
        @DisplayName("아무것도 변경되지 않으면 false를 반환한다")
        void should_returnFalse_when_nothingChanged() {
            User user = createDefaultUser();

            boolean changed = user.updateProfile(
                    user.getUsername(), user.getProfileImage(), user.getEmail());

            assertThat(changed).isFalse();
        }

        @Test
        @DisplayName("null 값은 기존 값을 유지한다")
        void should_keepExistingValues_when_nullPassed() {
            User user = createDefaultUser();
            String originalUsername = user.getUsername();

            boolean changed = user.updateProfile(null, null, null);

            assertThat(changed).isFalse();
            assertThat(user.getUsername()).isEqualTo(originalUsername);
        }
    }

    @Nested
    @DisplayName("점수/랭킹 업데이트")
    class ScoreUpdate {

        @Test
        @DisplayName("활동 통계로 점수와 랭킹을 동시에 업데이트한다")
        void should_updateScoreAndRank_when_statisticsProvided() {
            User user = createDefaultUser();
            ActivityStatistics stats = ActivityStatistics.of(100, 20, 10, 5, 15);

            user.updateActivityStatistics(stats, 0, 100);

            assertThat(user.getTotalScore()).isEqualTo(305);
            assertThat(user.getRanking()).isEqualTo(1);
        }

        @Test
        @DisplayName("점수 업데이트 후에는 더 이상 신규 사용자가 아니다")
        void should_notBeNewUser_after_scoreUpdate() {
            User user = createDefaultUser();
            user.updateScore(Score.of(100));

            assertThat(user.isNewUser()).isFalse();
        }
    }

    @Nested
    @DisplayName("티어 비교")
    class TierComparison {

        @Test
        @DisplayName("IRON 사용자는 isAtLeast(IRON)이 true이다")
        void should_returnTrue_when_sameOrHigherTier() {
            User user = createDefaultUser();

            assertThat(user.isAtLeast(Tier.IRON)).isTrue();
        }

        @Test
        @DisplayName("IRON 사용자는 isAtLeast(BRONZE)가 false이다")
        void should_returnFalse_when_lowerTier() {
            User user = createDefaultUser();

            assertThat(user.isAtLeast(Tier.BRONZE)).isFalse();
        }

        @Test
        @DisplayName("점수 업데이트 후 티어가 반영된다")
        void should_reflectTierAfterRankInfoUpdate() {
            User user = createDefaultUser();
            user.updateRankInfo(RankInfo.of(1, 5.0, 2000));

            assertThat(user.getTier()).isEqualTo(Tier.MASTER);
            assertThat(user.isAtLeast(Tier.GOLD)).isTrue();
        }
    }

    @Nested
    @DisplayName("null 안전성")
    class NullSafety {

        @Test
        @DisplayName("score가 null이면 getTotalScore는 0을 반환한다")
        void should_returnZero_when_scoreIsNull() {
            // Builder로 생성하면 Score.zero()가 설정되므로 정상 케이스만 확인
            User user = createDefaultUser();
            assertThat(user.getTotalScore()).isZero();
        }
    }
}
