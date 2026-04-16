package com.gitranker.api.infrastructure.github.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class GitHubAllActivitiesResponseTest {

    @Test
    @DisplayName("empty response starts with zero counts and no errors")
    void emptyResponseHasZeroCounts() {
        GitHubAllActivitiesResponse response = GitHubAllActivitiesResponse.empty();

        assertThat(response.hasErrors()).isFalse();
        assertThat(response.getCommitCount()).isZero();
        assertThat(response.getIssueCount()).isZero();
        assertThat(response.getPRCount()).isZero();
        assertThat(response.getMergedPRCount()).isZero();
        assertThat(response.getReviewCount()).isZero();
    }

    @Test
    @DisplayName("count getters return zero when response data is missing")
    void countGettersHandleMissingData() {
        GitHubAllActivitiesResponse response = new GitHubAllActivitiesResponse(null, null);

        assertThat(response.getCommitCount()).isZero();
        assertThat(response.getIssueCount()).isZero();
        assertThat(response.getPRCount()).isZero();
        assertThat(response.getMergedPRCount()).isZero();
        assertThat(response.getReviewCount()).isZero();
    }

    @Test
    @DisplayName("merge combines year data merged PRs and accumulated rate-limit cost")
    void mergesResponses() {
        GitHubAllActivitiesResponse base = response("year2025", 1, 2, 3, 4, 5, 1, 200, LocalDateTime.of(2026, 4, 16, 18, 0));
        GitHubAllActivitiesResponse other = response("year2026", 10, 20, 30, 40, 7, 2, 150, LocalDateTime.of(2026, 4, 16, 19, 0));

        base.merge(other);

        assertThat(base.getCommitCount()).isEqualTo(11);
        assertThat(base.getIssueCount()).isEqualTo(22);
        assertThat(base.getPRCount()).isEqualTo(33);
        assertThat(base.getReviewCount()).isEqualTo(44);
        assertThat(base.getMergedPRCount()).isEqualTo(7);
        assertThat(base.data().rateLimit().cost()).isEqualTo(3);
        assertThat(base.data().rateLimit().remaining()).isEqualTo(150);
    }

    @Test
    @DisplayName("data stores only year-prefixed entries")
    void storesOnlyYearPrefixedEntries() {
        GitHubAllActivitiesResponse.Data data = new GitHubAllActivitiesResponse.Data();

        data.setYearData("year2025", yearData(1, 1, 1, 1));
        data.setYearData("notYear", yearData(9, 9, 9, 9));

        assertThat(data.getYearDataMap()).containsOnlyKeys("year2025");
    }

    private GitHubAllActivitiesResponse response(
            String yearKey,
            int commits,
            int issues,
            int prs,
            int reviews,
            int mergedPrs,
            int cost,
            int remaining,
            LocalDateTime resetAt
    ) {
        GitHubAllActivitiesResponse.Data data = new GitHubAllActivitiesResponse.Data();
        data.setYearData(yearKey, yearData(commits, issues, prs, reviews));
        ReflectionTestUtils.setField(data, "mergedPRs", new GitHubAllActivitiesResponse.Search(mergedPrs));
        ReflectionTestUtils.setField(data, "rateLimit", new GitHubAllActivitiesResponse.RateLimit(5000, cost, remaining, resetAt));
        return new GitHubAllActivitiesResponse(data, null);
    }

    private GitHubAllActivitiesResponse.YearData yearData(int commits, int issues, int prs, int reviews) {
        return new GitHubAllActivitiesResponse.YearData(
                new GitHubAllActivitiesResponse.ContributionsCollection(commits, issues, prs, reviews)
        );
    }
}
