package com.gitranker.api.infrastructure.github;

import com.gitranker.api.infrastructure.github.dto.GitHubActivitySummary;
import com.gitranker.api.infrastructure.github.dto.GitHubAllActivitiesResponse;
import com.gitranker.api.infrastructure.github.dto.GitHubNodeUserResponse;
import com.gitranker.api.infrastructure.github.token.GitHubTokenPool;
import com.gitranker.api.global.logging.LogSanitizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class GitHubActivityService {

    private final GitHubGraphQLClient graphQLClient;
    private final GitHubTokenPool tokenPool;

    public GitHubActivitySummary fetchActivityForYear(String username, int year) {
        String token = tokenPool.getToken();
        GitHubAllActivitiesResponse response = graphQLClient.getActivitiesForYear(token, username, year);

        log.debug("증분 데이터 조회 완료 - 사용자: {}, 연도: {}", LogSanitizer.maskUsername(username), year);

        return toSummary(response);
    }

    public GitHubAllActivitiesResponse fetchRawAllActivities(String username, LocalDateTime githubJoinDate) {
        String token = tokenPool.getToken();
        GitHubAllActivitiesResponse response = graphQLClient.getAllActivities(token, username, githubJoinDate);

        log.debug("전체 데이터 조회 완료 - 사용자: {}", LogSanitizer.maskUsername(username));

        return response;
    }

    public GitHubNodeUserResponse fetchUserByNodeId(String nodeId) {
        String token = tokenPool.getToken();
        GitHubNodeUserResponse response = graphQLClient.getUserInfoByNodeId(token, nodeId);

        log.debug("nodeId 기반 사용자 조회 완료 - nodeId: {}, login: {}",
                nodeId, LogSanitizer.maskUsername(response.getLogin()));

        return response;
    }

    public GitHubActivitySummary toSummary(GitHubAllActivitiesResponse response) {
        return new GitHubActivitySummary(
                response.getCommitCount(),
                response.getPRCount(),
                response.getMergedPRCount(),
                response.getIssueCount(),
                response.getReviewCount()
        );
    }
}
