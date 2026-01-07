package com.gitranker.api.infrastructure.github;

import com.gitranker.api.global.logging.EventType;
import com.gitranker.api.global.logging.LogCategory;
import com.gitranker.api.global.logging.MdcUtils;
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

    public GitHubActivitySummary collectActivityForYear(String username, int year) {
        MdcUtils.setLogContext(LogCategory.EXTERNAL_API, EventType.REQUEST);
        MdcUtils.setUsername(username);

        GitHubAllActivitiesResponse response = graphQLClient.getActivitiesForYear(username, year);

        MdcUtils.setEventType(EventType.RESPONSE);
        log.info("증분 데이터 조회 완료 - 사용자: {}, 연도: {}", username, year);

        return convertToSummary(response);
    }

    public GitHubAllActivitiesResponse fetchRawAllActivities(String username, LocalDateTime githubJoinDate) {
        MdcUtils.setLogContext(LogCategory.EXTERNAL_API, EventType.REQUEST);
        MdcUtils.setUsername(username);

        GitHubAllActivitiesResponse response = graphQLClient.getAllActivities(username, githubJoinDate);

        MdcUtils.setEventType(EventType.RESPONSE);
        log.info("전체 데이터 조회 완료 - 사용자: {}", username);

        return response;
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
