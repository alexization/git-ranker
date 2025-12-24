package com.gitranker.api.infrastructure.github;

import com.gitranker.api.global.aop.LogExecutionTime;
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

    @LogExecutionTime
    public GitHubActivitySummary collectAllActivities(String username, LocalDateTime githubJoinDate) {
        GitHubAllActivitiesResponse response = graphQLClient.getAllActivities(username, githubJoinDate);

        int commitCount = response.getCommitCount();
        int prOpenCount = response.getPRCount();
        int prMergedCount = response.getMergedPRCount();
        int issueCount = response.getIssueCount();
        int reviewCount = response.getReviewCount();

        log.info("[Domain Event] GitHub 활동 내역 수집 - 사용자: {}, Commits: {}, Issues: {}, PrOpen: {}, PrMerged: {}, Reviews: {}",
                username, commitCount, prOpenCount, prMergedCount, issueCount, reviewCount);

        return new GitHubActivitySummary(
                commitCount,
                prOpenCount,
                prMergedCount,
                issueCount,
                reviewCount
        );
    }
}
