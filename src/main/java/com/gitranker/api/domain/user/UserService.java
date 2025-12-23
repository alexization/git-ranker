package com.gitranker.api.domain.user;

import com.gitranker.api.domain.log.ActivityLog;
import com.gitranker.api.domain.log.ActivityLogRepository;
import com.gitranker.api.domain.ranking.RankingService;
import com.gitranker.api.domain.ranking.dto.RankingInfo;
import com.gitranker.api.domain.user.dto.RegisterUserResponse;
import com.gitranker.api.global.exception.BusinessException;
import com.gitranker.api.global.exception.ErrorType;
import com.gitranker.api.global.logging.MdcUtils;
import com.gitranker.api.infrastructure.github.GitHubActivityService;
import com.gitranker.api.infrastructure.github.GitHubGraphQLClient;
import com.gitranker.api.infrastructure.github.dto.GitHubActivitySummary;
import com.gitranker.api.infrastructure.github.dto.GitHubUserInfoResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    public RegisterUserResponse registerUser(String username) {
        MdcUtils.setUsername(username);
        log.info("사용자 분석하기 요청");

        User existingUserByUsername = userRepository.findByUsername(username).orElse(null);
        if (existingUserByUsername != null) {
            log.info("기존 사용자 발견 - username 으로 조회");
            return createResponseForExistingUser(existingUserByUsername);
        }

        GitHubUserInfoResponse githubUserInfo = graphQLClient.getUserInfo(username);
        String nodeId = githubUserInfo.getNodeId();

        MdcUtils.setNodeId(nodeId);

        return userRepository.findByNodeId(nodeId)
                .map(existingUser -> {
                    log.info("기존 사용자 발견 - nodeId 로 조회");
                    return updateExistingUserProfile(existingUser, githubUserInfo);
                })
                .orElseGet(() -> registerNewUser(githubUserInfo, nodeId));
    }

    @Transactional(readOnly = true)
    public RegisterUserResponse searchUser(String username) {
        log.debug("사용자 조회");

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorType.USER_NOT_FOUND));

        ActivityLog activityLog = activityLogRepository.getTopByUserOrderByActivityDateDesc(user);

        MdcUtils.setUserContext(user.getUsername(), user.getNodeId());
        log.info("사용자 조회 완료 - Ranking: {}, Tier: {}", user.getRanking(), user.getTier());

        return RegisterUserResponse.of(user, activityLog, false);
    }

    private RegisterUserResponse createResponseForExistingUser(User user) {
        log.debug("기존 사용자 응답 수행");

        ActivityLog activityLog = activityLogRepository.getTopByUserOrderByActivityDateDesc(user);

        MdcUtils.setNodeId(user.getNodeId());
        log.info("기존 사용자 응답 반환");

        return RegisterUserResponse.of(user, activityLog, false);
    }

    private RegisterUserResponse updateExistingUserProfile(User user, GitHubUserInfoResponse userInfo) {
        log.debug("기존 사용자 프로필 업데이트 수행");

        user.updateUsername(userInfo.getLogin());
        user.updateProfileImage(userInfo.getAvatarUrl());

        userRepository.save(user);

        ActivityLog activityLog = activityLogRepository.getTopByUserOrderByActivityDateDesc(user);

        log.info("기존 사용자 프로필 업데이트 완료");

        return RegisterUserResponse.of(user, activityLog, false);
    }

    private RegisterUserResponse registerNewUser(GitHubUserInfoResponse githubUserInfo, String nodeId) {
        log.debug("신규 사용자 등록 시작");

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

        log.info("신규 사용자 등록 완료");

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
