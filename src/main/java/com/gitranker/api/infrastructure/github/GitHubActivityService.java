package com.gitranker.api.infrastructure.github;

import com.gitranker.api.infrastructure.github.dto.GitHubActivitySummary;
import com.gitranker.api.infrastructure.github.dto.GitHubAllActivitiesResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class GitHubActivityService {
    private final GitHubGraphQLClient graphQLClient;

    public GitHubActivitySummary collectAllActivities(String username, LocalDateTime githubJoinDate) {
        log.info("GitHub 활동 정보 수집 시작");

        GitHubAllActivitiesResponse response = graphQLClient.getAllActivities(username, githubJoinDate);

        int commitCount = response.getCommitCount();
        int prOpenCount = response.getPRCount();
        int prMergedCount = response.getMergedPRCount();
        int issueCount = response.getIssueCount();
        int reviewCount = response.getReviewCount();

        GitHubActivitySummary summary = new GitHubActivitySummary(
                commitCount,
                prOpenCount,
                prMergedCount,
                issueCount,
                reviewCount
        );

        log.info("GitHub 활동 정보 수집 완료 - Score: {}, Commits: {}, PrOpen: {}, PrMerged: {}, Issues: {}, Reviews: {}",
                summary.calculateTotalScore(), commitCount, prOpenCount, prMergedCount, issueCount, reviewCount);

        return summary;
    }
}
