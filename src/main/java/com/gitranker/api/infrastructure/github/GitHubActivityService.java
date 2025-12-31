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
    public GitHubActivitySummary collectActivityForYear(String username, int year) {
        GitHubAllActivitiesResponse response = graphQLClient.getActivitiesForYear(username, year);

        log.info("[GitHub API] 증분 데이터 조회 완료 - 사용자: {}", username);

        return convertToSummary(response);
    }

    public GitHubAllActivitiesResponse fetchRawAllActivities(String username, LocalDateTime githubJoinDate) {
        return graphQLClient.getAllActivities(username, githubJoinDate);
    }

    public GitHubActivitySummary convertToSummary(GitHubAllActivitiesResponse response) {
        return new GitHubActivitySummary(
                response.getCommitCount(),
                response.getPRCount(),
                response.getMergedPRCount(),
                response.getIssueCount(),
                response.getReviewCount()
        );
    }
}
