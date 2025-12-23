package com.gitranker.api.domain.user;

import com.gitranker.api.domain.log.ActivityLog;
import com.gitranker.api.domain.log.ActivityLogRepository;
import com.gitranker.api.domain.ranking.RankingService;
import com.gitranker.api.domain.ranking.dto.RankingInfo;
import com.gitranker.api.domain.user.dto.RegisterUserResponse;
import com.gitranker.api.global.aop.LogExecutionTime;
import com.gitranker.api.global.exception.BusinessException;
import com.gitranker.api.global.exception.ErrorType;
import com.gitranker.api.global.logging.MdcKey;
import com.gitranker.api.global.logging.MdcUtils;
import com.gitranker.api.infrastructure.github.GitHubActivityService;
import com.gitranker.api.infrastructure.github.GitHubGraphQLClient;
import com.gitranker.api.infrastructure.github.dto.GitHubActivitySummary;
import com.gitranker.api.infrastructure.github.dto.GitHubUserInfoResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final ActivityLogRepository activityLogRepository;
    private final GitHubGraphQLClient graphQLClient;
    private final GitHubActivityService gitHubActivityService;
    private final RankingService rankingService;

    @Transactional
    @LogExecutionTime
    public RegisterUserResponse registerUser(String username) {
        MdcUtils.setUsername(username);

        User existingUserByUsername = userRepository.findByUsername(username).orElse(null);
        if (existingUserByUsername != null) {
            log.info("[UserService] 기존 사용자 발견 - Username: {}", username);
            return createResponseForExistingUser(existingUserByUsername);
        }

        GitHubUserInfoResponse githubUserInfo = graphQLClient.getUserInfo(username);
        String nodeId = githubUserInfo.getNodeId();

        MdcUtils.setNodeId(nodeId);

        return userRepository.findByNodeId(nodeId)
                .map(existingUser -> {
                    log.info("[UserService] 기존 사용자 발견 (닉네임 변경 감지)");
                    return updateExistingUserProfile(existingUser, githubUserInfo);
                })
                .orElseGet(() -> registerNewUser(githubUserInfo, nodeId));
    }

    @Transactional(readOnly = true)
    @LogExecutionTime
    public RegisterUserResponse searchUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorType.USER_NOT_FOUND));

        ActivityLog activityLog = activityLogRepository.getTopByUserOrderByActivityDateDesc(user);

        MdcUtils.setUserContext(user.getUsername(), user.getNodeId());

        return RegisterUserResponse.of(user, activityLog, false);
    }

    private RegisterUserResponse createResponseForExistingUser(User user) {
        ActivityLog activityLog = activityLogRepository.getTopByUserOrderByActivityDateDesc(user);

        MdcUtils.setNodeId(user.getNodeId());

        return RegisterUserResponse.of(user, activityLog, false);
    }

    private RegisterUserResponse updateExistingUserProfile(User user, GitHubUserInfoResponse userInfo) {
        user.updateUsername(userInfo.getLogin());
        user.updateProfileImage(userInfo.getAvatarUrl());

        userRepository.save(user);

        ActivityLog activityLog = activityLogRepository.getTopByUserOrderByActivityDateDesc(user);

        return RegisterUserResponse.of(user, activityLog, false);
    }

    private RegisterUserResponse registerNewUser(GitHubUserInfoResponse githubUserInfo, String nodeId) {
        log.debug("[UserService] 신규 사용자 등록 시작 - Username: {}", githubUserInfo.getLogin());

        LocalDateTime githubCreatedAt = githubUserInfo.getGitHubCreatedAt();

        User newUser = User.builder()
                .nodeId(nodeId)
                .username(githubUserInfo.getLogin())
                .profileImage(githubUserInfo.getAvatarUrl())
                .githubCreatedAt(githubCreatedAt)
                .build();

        userRepository.save(newUser);

        GitHubActivitySummary summary =
                gitHubActivityService.collectAllActivities(githubUserInfo.getLogin(), githubCreatedAt);

        int totalScore = summary.calculateTotalScore();
        newUser.updateScore(totalScore);

        RankingInfo rankingInfo = rankingService.calculateRankingForNewUser(totalScore);
        newUser.updateRankInfo(
                rankingInfo.ranking(),
                rankingInfo.percentile(),
                rankingInfo.tier()
        );

        ActivityLog activityLog = saveInitialActivityLog(newUser, summary);

        return RegisterUserResponse.register(newUser, activityLog, true);
    }

    private ActivityLog saveInitialActivityLog(User user, GitHubActivitySummary summary) {
        ActivityLog activityLog = ActivityLog.builder()
                .user(user)
                .activityDate(LocalDate.now())
                .commitCount(summary.totalCommitCount())
                .issueCount(summary.totalIssueCount())
                .prCount(summary.totalPrOpenedCount())
                .mergedPrCount(summary.totalPrMergedCount())
                .reviewCount(summary.totalReviewCount())
                .diffCommitCount(0)
                .diffPrCount(0)
                .diffMergedPrCount(0)
                .diffReviewCount(0)
                .diffIssueCount(0)
                .build();

        activityLogRepository.save(activityLog);
        return activityLog;
    }
}
