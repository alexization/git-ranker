package com.gitranker.api.domain.user;

import com.gitranker.api.domain.log.ActivityLog;
import com.gitranker.api.domain.log.ActivityLogRepository;
import com.gitranker.api.domain.ranking.RankingInfo;
import com.gitranker.api.domain.ranking.RankingService;
import com.gitranker.api.domain.user.dto.RegisterUserResponse;
import com.gitranker.api.global.exception.BusinessException;
import com.gitranker.api.global.exception.ErrorType;
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
        GitHubUserInfoResponse userInfo = graphQLClient.getUserInfo(username);
        if (userInfo.data().user() == null) {
            throw new BusinessException(ErrorType.USER_NOT_FOUND);
        }

        String nodeId = userInfo.getNodeId();

        return userRepository.findByNodeId(nodeId)
                .map(existingUser -> handleExistingUser(existingUser, userInfo))
                .orElseGet(() -> handleNewUser(userInfo, nodeId));
    }

    private RegisterUserResponse handleExistingUser(User existingUser, GitHubUserInfoResponse userInfo) {
        log.info("기존 사용자 정보 업데이트: {}", userInfo.getLogin());

        existingUser.updateUsername(userInfo.getLogin());
        existingUser.updateProfileImage(userInfo.getAvatarUrl());

        ActivityLog activityLog = activityLogRepository.getTopByUserOrderByActivityDateDesc(existingUser);

        return RegisterUserResponse.of(existingUser, activityLog, false);
    }

    private RegisterUserResponse handleNewUser(GitHubUserInfoResponse userInfo, String nodeId) {
        log.info("신규 사용자 등록 시작: {}", userInfo.getLogin());

        LocalDateTime githubCreatedAt = userInfo.getGitHubCreatedAt();

        User newUser = User.builder()
                .nodeId(nodeId)
                .username(userInfo.getLogin())
                .profileImage(userInfo.getAvatarUrl())
                .githubCreatedAt(githubCreatedAt)
                .build();

        userRepository.save(newUser);

        GitHubActivitySummary summary =
                gitHubActivityService.collectAllActivities(userInfo.getLogin(), githubCreatedAt);

        int totalScore = summary.calculateTotalScore();
        newUser.updateScore(totalScore);

        RankingInfo rankingInfo = rankingService.calculateRankingForNewUser(totalScore);
        newUser.updateRankInfo(
                rankingInfo.ranking(),
                rankingInfo.percentile(),
                rankingInfo.tier()
        );

        ActivityLog activityLog = saveInitialActivityLog(newUser, summary);

        log.info("신규 사용자 등록 완료: {}, 총점: {}, {}", userInfo.getLogin(), totalScore, rankingInfo);

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
