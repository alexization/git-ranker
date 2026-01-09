package com.gitranker.api.domain.user.service;

import com.gitranker.api.domain.log.ActivityLog;
import com.gitranker.api.domain.log.ActivityLogService;
import com.gitranker.api.domain.user.User;
import com.gitranker.api.domain.user.UserRepository;
import com.gitranker.api.domain.user.dto.RegisterUserResponse;
import com.gitranker.api.domain.user.vo.ActivityStatistics;
import com.gitranker.api.global.logging.EventType;
import com.gitranker.api.global.logging.LogCategory;
import com.gitranker.api.global.logging.MdcUtils;
import com.gitranker.api.infrastructure.github.GitHubActivityService;
import com.gitranker.api.infrastructure.github.GitHubDataMapper;
import com.gitranker.api.infrastructure.github.GitHubGraphQLClient;
import com.gitranker.api.infrastructure.github.dto.GitHubAllActivitiesResponse;
import com.gitranker.api.infrastructure.github.dto.GitHubUserInfoResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserRegistrationService {

    private final UserRepository userRepository;
    private final UserPersistenceService userPersistenceService;
    private final ActivityLogService activityLogService;
    private final GitHubGraphQLClient gitHubGraphQLClient;
    private final GitHubActivityService gitHubActivityService;
    private final GitHubDataMapper gitHubDataMapper;

    public RegisterUserResponse register(String username) {
        MdcUtils.setUsername(username);
        MdcUtils.setLogContext(LogCategory.DOMAIN, EventType.REQUEST);

        Optional<User> existingByUsername = userRepository.findByUsername(username);
        if (existingByUsername.isPresent()) {
            User user = existingByUsername.get();

            MdcUtils.setNodeId(user.getNodeId());
            MdcUtils.setEventType(EventType.SUCCESS);
            log.info("기존 사용자 조회 - 사용자: {}", username);

            return createResponse(user, false);
        }

        GitHubUserInfoResponse githubUserInfo = gitHubGraphQLClient.getUserInfo(username);
        String nodeId = githubUserInfo.getNodeId();
        MdcUtils.setNodeId(nodeId);

        Optional<User> existingByNodeId = userRepository.findByNodeId(nodeId);
        if (existingByNodeId.isPresent()) {
            return handleExistingUserWithChangedProfile(existingByNodeId.get(), githubUserInfo);
        }

        return registerNewUser(githubUserInfo);
    }

    private RegisterUserResponse registerNewUser(GitHubUserInfoResponse githubUserInfo) {
        String username = githubUserInfo.getLogin();
        log.debug("신규 사용자 등록 시작 - 사용자: {}", username);

        GitHubAllActivitiesResponse rawResponse = gitHubActivityService
                .fetchRawAllActivities(username, githubUserInfo.getGitHubCreatedAt());

        ActivityStatistics totalStats = gitHubDataMapper.toActivityStatistics(rawResponse);
        ActivityStatistics baselineStats = calculateBaselineStats(githubUserInfo, rawResponse);

        User savedUser = userPersistenceService.saveNewUser(githubUserInfo, totalStats, baselineStats);

        MdcUtils.setEventType(EventType.SUCCESS);
        log.info("신규 사용자 등록 완료 - 사용자: {}, 점수: {}, 티어: {}",
                savedUser.getUsername(), savedUser.getTotalScore(), savedUser.getTier());

        return createResponse(savedUser, true);
    }

    private RegisterUserResponse handleExistingUserWithChangedProfile(User existingUser, GitHubUserInfoResponse githubUserInfo) {
        String oldUsername = existingUser.getUsername();
        String newUsername = githubUserInfo.getLogin();

        log.info("사용자 닉네임 변경 감지 - 기존: {}, 신규: {}", oldUsername, newUsername);

        User updatedUser = userPersistenceService.updateProfile(existingUser, newUsername, githubUserInfo.getAvatarUrl());

        MdcUtils.setEventType(EventType.SUCCESS);
        log.info("사용자 프로필 업데이트 완료 - 사용자: {}", newUsername);

        return createResponse(updatedUser, false);
    }

    private ActivityStatistics calculateBaselineStats(GitHubUserInfoResponse githubUserInfo, GitHubAllActivitiesResponse rawResponse) {
        int currentYear = LocalDate.now().getYear();

        if (githubUserInfo.getGitHubCreatedAt().getYear() < currentYear) {
            int lastYear = currentYear - 1;
            return gitHubDataMapper.calculateStatisticsUntilYear(rawResponse, lastYear);
        }

        return null;
    }

    private RegisterUserResponse createResponse(User user, boolean isNewUser) {
        ActivityLog activityLog = activityLogService.findLatestLog(user)
                .orElse(createEmptyActivityLog(user));

        return RegisterUserResponse.of(user, activityLog, isNewUser);
    }

    private ActivityLog createEmptyActivityLog(User user) {
        return ActivityLog.builder()
                .user(user)
                .activityDate(LocalDate.now())
                .commitCount(0).issueCount(0).prCount(0).mergedPrCount(0).reviewCount(0)
                .diffCommitCount(0).diffIssueCount(0).diffPrCount(0).diffMergedPrCount(0).diffReviewCount(0)
                .build();
    }
}
