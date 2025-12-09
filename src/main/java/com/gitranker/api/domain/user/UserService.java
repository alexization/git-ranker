package com.gitranker.api.domain.user;

import com.gitranker.api.domain.log.ActivityLog;
import com.gitranker.api.domain.log.ActivityLogRepository;
import com.gitranker.api.domain.ranking.RankingInfo;
import com.gitranker.api.domain.ranking.RankingService;
import com.gitranker.api.domain.user.dto.RegisterUserResponse;
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
        User existingUserByUsername = userRepository.findByUsername(username).orElse(null);
        if (existingUserByUsername != null) {
            return createResponseForExistingUser(existingUserByUsername);
        }

        GitHubUserInfoResponse githubUserInfo = graphQLClient.getUserInfo(username);
        String nodeId = githubUserInfo.getNodeId();

        return userRepository.findByNodeId(nodeId)
                .map(existingUser -> updateExistingUserProfile(existingUser, githubUserInfo))
                .orElseGet(() -> registerNewUser(githubUserInfo, nodeId));
    }

    private RegisterUserResponse createResponseForExistingUser(User user) {
        ActivityLog activityLog = activityLogRepository.getTopByUserOrderByActivityDateDesc(user);

        return RegisterUserResponse.of(user, activityLog, false);
    }

    private RegisterUserResponse updateExistingUserProfile(User user, GitHubUserInfoResponse userInfo) {
        log.info("기존 사용자 정보 업데이트: {}", userInfo.getLogin());

        user.updateUsername(userInfo.getLogin());
        user.updateProfileImage(userInfo.getAvatarUrl());

        ActivityLog activityLog = activityLogRepository.getTopByUserOrderByActivityDateDesc(user);

        return RegisterUserResponse.of(user, activityLog, false);
    }

    private RegisterUserResponse registerNewUser(GitHubUserInfoResponse githubUserInfo, String nodeId) {
        log.info("신규 사용자 등록 시작: {}", githubUserInfo.getLogin());

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

        log.info("신규 사용자 등록 완료: {}, 총점: {}, {}", githubUserInfo.getLogin(), totalScore, rankingInfo);

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
