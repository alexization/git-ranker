package com.gitranker.api.infrastructure.github;

import com.gitranker.api.infrastructure.github.dto.GitHubActivitySummary;
import com.gitranker.api.infrastructure.github.dto.GitHubAllActivitiesResponse;
import com.gitranker.api.infrastructure.github.dto.GitHubNodeUserResponse;
import com.gitranker.api.infrastructure.github.token.GitHubTokenPool;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GitHubActivityServiceTest {

    @InjectMocks
    private GitHubActivityService service;

    @Mock
    private GitHubGraphQLClient graphQLClient;

    @Mock
    private GitHubTokenPool tokenPool;

    @Test
    @DisplayName("fetchActivityForYear uses pooled token and maps GitHub totals")
    void fetchesActivityForYear() {
        GitHubAllActivitiesResponse response = activityResponse(3, 2, 4, 5, 6);
        when(tokenPool.getToken()).thenReturn("token-a");
        when(graphQLClient.getActivitiesForYear("token-a", "alice", 2026)).thenReturn(response);

        GitHubActivitySummary summary = service.fetchActivityForYear("alice", 2026);

        assertThat(summary.commitCount()).isEqualTo(3);
        assertThat(summary.issueCount()).isEqualTo(5);
        assertThat(summary.prOpenedCount()).isEqualTo(4);
        assertThat(summary.prMergedCount()).isEqualTo(2);
        assertThat(summary.reviewCount()).isEqualTo(6);
        verify(tokenPool).getToken();
        verify(graphQLClient).getActivitiesForYear("token-a", "alice", 2026);
    }

    @Test
    @DisplayName("fetchRawAllActivities uses pooled token and returns raw response")
    void fetchesRawAllActivities() {
        LocalDateTime joinedAt = LocalDateTime.of(2020, 1, 1, 0, 0);
        GitHubAllActivitiesResponse response = GitHubAllActivitiesResponse.empty();
        when(tokenPool.getToken()).thenReturn("token-a");
        when(graphQLClient.getAllActivities("token-a", "alice", joinedAt)).thenReturn(response);

        GitHubAllActivitiesResponse actual = service.fetchRawAllActivities("alice", joinedAt);

        assertThat(actual).isSameAs(response);
        verify(graphQLClient).getAllActivities("token-a", "alice", joinedAt);
    }

    @Test
    @DisplayName("fetchUserByNodeId uses pooled token and returns node lookup response")
    void fetchesUserByNodeId() {
        GitHubNodeUserResponse response = new GitHubNodeUserResponse(
                new GitHubNodeUserResponse.Data(
                        new GitHubNodeUserResponse.Node("node-1", "alice", "alice@example.com", "avatar"),
                        null
                )
        );
        when(tokenPool.getToken()).thenReturn("token-a");
        when(graphQLClient.getUserInfoByNodeId("token-a", "node-1")).thenReturn(response);

        GitHubNodeUserResponse actual = service.fetchUserByNodeId("node-1");

        assertThat(actual).isSameAs(response);
        verify(graphQLClient).getUserInfoByNodeId("token-a", "node-1");
    }

    @Test
    @DisplayName("toSummary maps aggregated GitHub activity counts")
    void mapsAggregatedResponseToSummary() {
        GitHubAllActivitiesResponse response = activityResponse(9, 7, 5, 4, 3);

        GitHubActivitySummary summary = service.toSummary(response);

        assertThat(summary).isEqualTo(new GitHubActivitySummary(9, 5, 7, 4, 3));
    }

    private GitHubAllActivitiesResponse activityResponse(
            int commits,
            int mergedPrs,
            int openedPrs,
            int issues,
            int reviews
    ) {
        GitHubAllActivitiesResponse.Data data = new GitHubAllActivitiesResponse.Data();
        data.setYearData(
                "year2026",
                new GitHubAllActivitiesResponse.YearData(
                        new GitHubAllActivitiesResponse.ContributionsCollection(commits, issues, openedPrs, reviews)
                )
        );
        ReflectionTestUtils.setField(data, "mergedPRs", new GitHubAllActivitiesResponse.Search(mergedPrs));
        return new GitHubAllActivitiesResponse(data, null);
    }
}
