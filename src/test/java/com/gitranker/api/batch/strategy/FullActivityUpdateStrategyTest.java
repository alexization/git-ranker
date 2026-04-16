package com.gitranker.api.batch.strategy;

import com.gitranker.api.domain.user.User;
import com.gitranker.api.domain.user.vo.ActivityStatistics;
import com.gitranker.api.infrastructure.github.GitHubActivityService;
import com.gitranker.api.infrastructure.github.dto.GitHubActivitySummary;
import com.gitranker.api.infrastructure.github.dto.GitHubAllActivitiesResponse;
import com.gitranker.api.support.TestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FullActivityUpdateStrategyTest {

    @InjectMocks
    private FullActivityUpdateStrategy strategy;

    @Mock
    private GitHubActivityService activityService;

    @Test
    @DisplayName("full update fetches all activities and converts them to statistics")
    void updatesWithFullGitHubScan() {
        User user = TestFixtures.user("alice");
        GitHubAllActivitiesResponse response = GitHubAllActivitiesResponse.empty();
        GitHubActivitySummary summary = new GitHubActivitySummary(7, 3, 4, 2, 5);

        when(activityService.fetchRawAllActivities(user.getUsername(), user.getGithubCreatedAt())).thenReturn(response);
        when(activityService.toSummary(response)).thenReturn(summary);

        ActivityStatistics statistics = strategy.update(user, ActivityUpdateContext.forFull(2026));

        assertThat(statistics).isEqualTo(ActivityStatistics.of(7, 2, 3, 4, 5));
        verify(activityService).fetchRawAllActivities(user.getUsername(), user.getGithubCreatedAt());
        verify(activityService).toSummary(response);
    }

    @Test
    @DisplayName("full update propagates GitHub activity service failures")
    void propagatesActivityServiceFailure() {
        User user = TestFixtures.user("alice");

        when(activityService.fetchRawAllActivities(user.getUsername(), user.getGithubCreatedAt()))
                .thenThrow(new IllegalStateException("github unavailable"));

        assertThatThrownBy(() -> strategy.update(user, ActivityUpdateContext.forFull(2026)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("github unavailable");
    }
}
