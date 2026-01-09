package com.gitranker.api.infrastructure.github;

import com.gitranker.api.domain.user.vo.ActivityStatistics;
import com.gitranker.api.infrastructure.github.dto.GitHubAllActivitiesResponse;
import com.gitranker.api.infrastructure.github.dto.GitHubAllActivitiesResponse.YearData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

import static com.gitranker.api.infrastructure.github.dto.GitHubAllActivitiesResponse.ContributionsCollection;

@Slf4j
@Component
public class GitHubDataMapper {

    public ActivityStatistics toActivityStatistics(GitHubAllActivitiesResponse response) {
        if (response == null || response.data() == null) {
            return ActivityStatistics.empty();
        }

        return ActivityStatistics.of(
                response.getCommitCount(),
                response.getIssueCount(),
                response.getPRCount(),
                response.getMergedPRCount(),
                response.getReviewCount()
        );
    }

    public ActivityStatistics calculateStatisticsUntilYear(GitHubAllActivitiesResponse response, int targetYear) {
        if (response == null || response.data() == null || response.data().getYearDataMap() == null) {
            return ActivityStatistics.empty();
        }

        int commits = 0;
        int issues = 0;
        int prs = 0;
        int reviews = 0;

        for (Map.Entry<String, YearData> entry : response.data().getYearDataMap().entrySet()) {
            int year = extractYear(entry.getKey());

            if (year > 0 && year <= targetYear) {
                ContributionsCollection collection = entry.getValue().contributionsCollection();
                commits += collection.totalCommitContributions();
                issues += collection.totalIssueContributions();
                prs += collection.totalPullRequestContributions();
                reviews += collection.totalPullRequestReviewContributions();
            }
        }

        return ActivityStatistics.of(commits, issues, prs, 0, reviews);
    }

    private int extractYear(String yearKey) {
        try {
            return Integer.parseInt(yearKey.replace("year", ""));
        } catch (NumberFormatException e) {
            log.warn("연도 파싱 실패: {}", yearKey);
            return -1;
        }
    }
}
