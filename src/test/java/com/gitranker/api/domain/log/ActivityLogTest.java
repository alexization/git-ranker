package com.gitranker.api.domain.log;

import com.gitranker.api.domain.user.User;
import com.gitranker.api.domain.user.vo.ActivityStatistics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static com.gitranker.api.support.TestFixtures.stats;
import static com.gitranker.api.support.TestFixtures.user;
import static org.assertj.core.api.Assertions.assertThat;

class ActivityLogTest {

    @Test
    @DisplayName("일반 활동 로그 생성은 총계와 diff를 각 필드에 매핑한다")
    void createsRegularActivityLogFromStatistics() {
        User user = user("alice");
        ActivityLog log = ActivityLog.of(
                user,
                stats(10, 2, 3, 4, 5),
                stats(1, -1, 0, 2, 3),
                LocalDate.of(2025, 1, 2)
        );

        assertThat(log.getCommitCount()).isEqualTo(10);
        assertThat(log.getIssueCount()).isEqualTo(2);
        assertThat(log.getPrCount()).isEqualTo(3);
        assertThat(log.getMergedPrCount()).isEqualTo(4);
        assertThat(log.getReviewCount()).isEqualTo(5);
        assertThat(log.getDiffIssueCount()).isEqualTo(-1);
        assertThat(log.getDiffMergedPrCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("baseline과 empty 로그는 diff가 모두 0이다")
    void baselineAndEmptyLogsResetDiffs() {
        User user = user("alice");
        ActivityLog baseline = ActivityLog.baseline(user, stats(5, 4, 3, 2, 1), LocalDate.of(2024, 12, 31));
        ActivityLog empty = ActivityLog.empty(user, LocalDate.of(2025, 1, 1));

        assertThat(baseline.getDiffCommitCount()).isZero();
        assertThat(baseline.getDiffReviewCount()).isZero();
        assertThat(empty.toStatistics()).isEqualTo(ActivityStatistics.empty());
    }

    @Test
    @DisplayName("toStatistics와 updateStatistics는 총계만 반영한다")
    void convertsAndUpdatesStatisticsWithoutChangingDiff() {
        User user = user("alice");
        ActivityLog log = ActivityLog.empty(user, LocalDate.of(2025, 1, 1));

        log.updateStatistics(stats(8, 7, 6, 5, 4));

        assertThat(log.toStatistics()).isEqualTo(stats(8, 7, 6, 5, 4));
        assertThat(log.getDiffCommitCount()).isZero();
    }

    @Test
    @DisplayName("updateStatisticsWithDiff는 총계와 diff를 함께 교체한다")
    void updatesStatisticsWithDiff() {
        User user = user("alice");
        ActivityLog log = ActivityLog.empty(user, LocalDate.of(2025, 1, 1));

        log.updateStatisticsWithDiff(stats(9, 8, 7, 6, 5), stats(1, 2, 3, 4, 5));

        assertThat(log.toStatistics()).isEqualTo(stats(9, 8, 7, 6, 5));
        assertThat(log.getDiffCommitCount()).isEqualTo(1);
        assertThat(log.getDiffReviewCount()).isEqualTo(5);
    }
}
