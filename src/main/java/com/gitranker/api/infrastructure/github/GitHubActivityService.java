package com.gitranker.api.infrastructure.github;

import com.gitranker.api.global.logging.EventType;
import com.gitranker.api.global.logging.LogCategory;
import com.gitranker.api.global.logging.MdcUtils;
import com.gitranker.api.infrastructure.github.dto.GitHubActivitySummary;
import com.gitranker.api.infrastructure.github.dto.GitHubAllActivitiesResponse;
import com.gitranker.api.infrastructure.github.token.GitHubTokenPool;
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
        MdcUtils.setLogContext(LogCategory.EXTERNAL_API, EventType.REQUEST);
        MdcUtils.setUsername(username);

        String token = tokenPool.getToken();
        GitHubAllActivitiesResponse response = graphQLClient.getActivitiesForYear(token, username, year);

        MdcUtils.setEventType(EventType.RESPONSE);
        log.info("증분 데이터 조회 완료 - 사용자: {}, 연도: {}", username, year);

        return toSummary(response);
    }

    public GitHubAllActivitiesResponse fetchRawAllActivities(String username, LocalDateTime githubJoinDate) {
        MdcUtils.setLogContext(LogCategory.EXTERNAL_API, EventType.REQUEST);
        MdcUtils.setUsername(username);

        String token = tokenPool.getToken();
        GitHubAllActivitiesResponse response = graphQLClient.getAllActivities(token, username, githubJoinDate);

        MdcUtils.setEventType(EventType.RESPONSE);
        log.info("전체 데이터 조회 완료 - 사용자: {}", username);

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
