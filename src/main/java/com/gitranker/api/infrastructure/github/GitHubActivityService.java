package com.gitranker.api.infrastructure.github;

import com.gitranker.api.domain.user.User;
import com.gitranker.api.domain.user.UserRepository;
import com.gitranker.api.domain.user.vo.ActivityStatistics;
import com.gitranker.api.global.error.ErrorType;
import com.gitranker.api.global.error.exception.BusinessException;
import com.gitranker.api.global.logging.EventType;
import com.gitranker.api.global.logging.LogCategory;
import com.gitranker.api.global.logging.MdcUtils;
import com.gitranker.api.infrastructure.github.dto.GitHubActivitySummary;
import com.gitranker.api.infrastructure.github.dto.GitHubAllActivitiesResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
public class GitHubActivityService {

    private final GitHubGraphQLClient graphQLClient;
    private final GitHubDataMapper dataMapper;
    private final UserRepository userRepository;
    private final String systemToken;

    public GitHubActivityService(
            GitHubGraphQLClient graphQLClient,
            GitHubDataMapper dataMapper,
            UserRepository userRepository,
            @Value("${github.api.token}") String systemToken
    ) {
        this.graphQLClient = graphQLClient;
        this.dataMapper = dataMapper;
        this.userRepository = userRepository;
        this.systemToken = systemToken;
    }

    public GitHubActivitySummary collectActivityForYear(String username, int year) {
        MdcUtils.setLogContext(LogCategory.EXTERNAL_API, EventType.REQUEST);
        MdcUtils.setUsername(username);

        GitHubAllActivitiesResponse response = graphQLClient.getActivitiesForYear(systemToken, username, year);

        MdcUtils.setEventType(EventType.RESPONSE);
        log.info("증분 데이터 조회 완료 - 사용자: {}, 연도: {}", username, year);

        return convertToSummary(response);
    }

    public GitHubAllActivitiesResponse fetchRawAllActivities(String accessToken, String username, LocalDateTime githubJoinDate) {
        MdcUtils.setLogContext(LogCategory.EXTERNAL_API, EventType.REQUEST);
        MdcUtils.setUsername(username);

        GitHubAllActivitiesResponse response = graphQLClient.getAllActivities(accessToken, username, githubJoinDate);

        MdcUtils.setEventType(EventType.RESPONSE);
        log.info("전체 데이터 조회 완료 - 사용자: {}", username);

        return response;
    }

    public GitHubAllActivitiesResponse fetchRawAllActivities(String username, LocalDateTime githubJoinDate) {
        return fetchRawAllActivities(systemToken, username, githubJoinDate);
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

    @Transactional
    public void fetchAndSaveUserActivities(String accessToken, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorType.USER_NOT_FOUND));

        GitHubAllActivitiesResponse response = graphQLClient.getAllActivities(accessToken, username, user.getGithubCreatedAt());

        ActivityStatistics statistics = dataMapper.toActivityStatistics(response);

        long totalUserCount = userRepository.count();
        long higherScoreCount = userRepository.countByScoreValueGreaterThan(statistics.calculateScore().getValue());

        user.updateActivityStatistics(statistics, higherScoreCount, totalUserCount);

        log.info("사용자 활동 데이터 수집 및 점수 갱신 완료 - 사용자: {}", username);
    }
}
