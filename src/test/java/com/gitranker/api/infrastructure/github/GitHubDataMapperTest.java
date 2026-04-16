package com.gitranker.api.infrastructure.github;

import com.gitranker.api.domain.user.vo.ActivityStatistics;
import com.gitranker.api.infrastructure.github.dto.GitHubAllActivitiesResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class GitHubDataMapperTest {

    private final GitHubDataMapper mapper = new GitHubDataMapper();

    @Test
    @DisplayName("toActivityStatistics returns empty stats for null or missing data")
    void returnsEmptyStatisticsForNullResponse() {
        assertThat(mapper.toActivityStatistics(null)).isEqualTo(ActivityStatistics.empty());
        assertThat(mapper.toActivityStatistics(new GitHubAllActivitiesResponse(null, null))).isEqualTo(ActivityStatistics.empty());
    }

    @Test
    @DisplayName("toActivityStatistics maps total counts from aggregated response")
    void mapsResponseToStatistics() {
        GitHubAllActivitiesResponse response = responseWithYears(
                new Object[][] { { "year2025", 7, 3, 4, 5 } },
                6,
                100,
                LocalDateTime.of(2026, 4, 16, 18, 0)
        );

        ActivityStatistics statistics = mapper.toActivityStatistics(response);

        assertThat(statistics).isEqualTo(ActivityStatistics.of(7, 3, 4, 6, 5));
    }

    @Test
    @DisplayName("calculateStatisticsUntilYear aggregates eligible years and ignores invalid keys")
    void aggregatesStatisticsUntilTargetYear() {
        GitHubAllActivitiesResponse.Data data = new GitHubAllActivitiesResponse.Data();
        data.setYearData("year2023", yearData(1, 2, 3, 4));
        data.setYearData("year2024", yearData(10, 20, 30, 40));
        data.setYearData("year2027", yearData(100, 200, 300, 400));
        data.setYearData("invalid", yearData(999, 999, 999, 999));

        GitHubAllActivitiesResponse response = new GitHubAllActivitiesResponse(data, null);

        ActivityStatistics statistics = mapper.calculateStatisticsUntilYear(response, 2024);

        assertThat(statistics).isEqualTo(ActivityStatistics.of(11, 22, 33, 0, 44));
    }

    private GitHubAllActivitiesResponse responseWithYears(
            Object[][] years,
            int mergedPrCount,
            int remaining,
            LocalDateTime resetAt
    ) {
        GitHubAllActivitiesResponse.Data data = new GitHubAllActivitiesResponse.Data();
        for (Object[] year : years) {
            data.setYearData(
                    (String) year[0],
                    yearData((int) year[1], (int) year[2], (int) year[3], (int) year[4])
            );
        }
        ReflectionTestUtils.setField(data, "mergedPRs", new GitHubAllActivitiesResponse.Search(mergedPrCount));
        ReflectionTestUtils.setField(data, "rateLimit", new GitHubAllActivitiesResponse.RateLimit(5000, 1, remaining, resetAt));
        return new GitHubAllActivitiesResponse(data, null);
    }

    private GitHubAllActivitiesResponse.YearData yearData(int commits, int issues, int prs, int reviews) {
        return new GitHubAllActivitiesResponse.YearData(
                new GitHubAllActivitiesResponse.ContributionsCollection(commits, issues, prs, reviews)
        );
    }
}
