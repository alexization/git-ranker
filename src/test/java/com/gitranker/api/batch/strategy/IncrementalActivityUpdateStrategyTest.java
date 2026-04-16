package com.gitranker.api.batch.strategy;

import com.gitranker.api.domain.log.ActivityLog;
import com.gitranker.api.domain.user.User;
import com.gitranker.api.domain.user.vo.ActivityStatistics;
import com.gitranker.api.infrastructure.github.GitHubActivityService;
import com.gitranker.api.infrastructure.github.dto.GitHubActivitySummary;
import com.gitranker.api.support.TestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IncrementalActivityUpdateStrategyTest {

    @InjectMocks
    private IncrementalActivityUpdateStrategy strategy;

    @Mock
    private GitHubActivityService activityService;

    @Test
    @DisplayName("incremental update merges baseline counts with current-year summary")
    void mergesBaselineWithCurrentYearSummary() {
        User user = TestFixtures.user("alice");
        ActivityLog baselineLog = ActivityLog.baseline(
                user,
                TestFixtures.stats(10, 5, 4, 9, 3),
                LocalDate.of(2025, 12, 31)
        );
        GitHubActivitySummary summary = new GitHubActivitySummary(7, 2, 8, 1, 4);

        when(activityService.fetchActivityForYear(user.getUsername(), 2026)).thenReturn(summary);

        ActivityStatistics statistics = strategy.update(user, ActivityUpdateContext.forIncremental(baselineLog, 2026));

        assertThat(statistics).isEqualTo(ActivityStatistics.of(17, 6, 6, 8, 7));
        verify(activityService).fetchActivityForYear(user.getUsername(), 2026);
    }

    @Test
    @DisplayName("incremental update propagates GitHub fetch failures")
    void propagatesFetchFailure() {
        User user = TestFixtures.user("alice");
        ActivityLog baselineLog = ActivityLog.baseline(
                user,
                TestFixtures.stats(1, 1, 1, 1, 1),
                LocalDate.of(2025, 12, 31)
        );

        when(activityService.fetchActivityForYear(user.getUsername(), 2026))
                .thenThrow(new IllegalArgumentException("bad response"));

        assertThatThrownBy(() -> strategy.update(user, ActivityUpdateContext.forIncremental(baselineLog, 2026)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("bad response");
    }
}
