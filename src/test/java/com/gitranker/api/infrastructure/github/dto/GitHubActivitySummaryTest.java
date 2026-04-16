package com.gitranker.api.infrastructure.github.dto;

import com.gitranker.api.domain.user.vo.ActivityStatistics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GitHubActivitySummaryTest {

    @Test
    @DisplayName("summary converts to activity statistics with GitHub field ordering")
    void convertsToActivityStatistics() {
        GitHubActivitySummary summary = new GitHubActivitySummary(7, 3, 4, 2, 5);

        ActivityStatistics statistics = summary.toActivityStatistics();

        assertThat(statistics).isEqualTo(ActivityStatistics.of(7, 2, 3, 4, 5));
    }
}
