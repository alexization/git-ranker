package com.gitranker.api.domain.user;

import com.gitranker.api.domain.log.ActivityLog;
import com.gitranker.api.domain.log.ActivityLogRepository;
import com.gitranker.api.domain.ranking.RankingService;
import com.gitranker.api.domain.ranking.dto.RankingInfo;
import com.gitranker.api.domain.user.dto.RegisterUserResponse;
import com.gitranker.api.global.aop.LogExecutionTime;
import com.gitranker.api.global.exception.BusinessException;
import com.gitranker.api.global.exception.ErrorType;
import com.gitranker.api.global.logging.MdcUtils;
import com.gitranker.api.infrastructure.github.GitHubActivityService;
import com.gitranker.api.infrastructure.github.GitHubGraphQLClient;
import com.gitranker.api.infrastructure.github.dto.GitHubActivitySummary;
import com.gitranker.api.infrastructure.github.dto.GitHubAllActivitiesResponse;
import com.gitranker.api.infrastructure.github.dto.GitHubAllActivitiesResponse.ContributionsCollection;
import com.gitranker.api.infrastructure.github.dto.GitHubAllActivitiesResponse.YearData;
import com.gitranker.api.infrastructure.github.dto.GitHubUserInfoResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Map;

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
            return createResponseForExistingUser(existingUserByUsername);
        }

        GitHubUserInfoResponse githubUserInfo = graphQLClient.getUserInfo(username);
        String nodeId = githubUserInfo.getNodeId();

        MdcUtils.setNodeId(nodeId);

        return userRepository.findByNodeId(nodeId)
                .map(existingUser -> {
                    log.info("[Domain Event] 사용자 닉네임 업데이트 - 기존 닉네임: {}, 신규 닉네임: {}", existingUser.getUsername(), githubUserInfo.getLogin());
                    return updateExistingUserProfile(existingUser, githubUserInfo);
                })
                .orElseGet(() -> registerNewUser(githubUserInfo, nodeId));
    }

    private RegisterUserResponse registerNewUser(GitHubUserInfoResponse githubUserInfo, String nodeId) {
        User newUser = User.builder()
                .nodeId(nodeId)
                .username(githubUserInfo.getLogin())
                .profileImage(githubUserInfo.getAvatarUrl())
                .githubCreatedAt(githubUserInfo.getGitHubCreatedAt())
                .build();

        userRepository.save(newUser);

        ActivityLog currentLog = createPastLogAndCurrentLog(newUser);

        updateUserScoreAndRanking(newUser, currentLog);

        log.info("[Domain Event] 신규 사용자 등록 완료");

        return RegisterUserResponse.register(newUser, currentLog, true);
    }

    @Transactional(readOnly = true)
    @LogExecutionTime
    public RegisterUserResponse searchUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorType.USER_NOT_FOUND));

        log.info("[Domain Event] 사용자 검색 - 타겟 사용자: {}", username);

        ActivityLog activityLog = activityLogRepository.getTopByUserOrderByActivityDateDesc(user);

        MdcUtils.setUserContext(user.getUsername(), user.getNodeId());

        return RegisterUserResponse.of(user, activityLog, false);
    }

    @Transactional
    @LogExecutionTime
    public RegisterUserResponse refreshUser(String username) {
        MdcUtils.setUsername(username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorType.USER_NOT_FOUND));

        MdcUtils.setNodeId(user.getNodeId());

        if (!user.canTriggerFullScan()) {
            throw new BusinessException(ErrorType.REFRESH_COOL_DOWN_EXCEEDED);
        }

        log.info("[Domain Event] 사용자 수동 전체 갱신 요청 - 사용자: {}", username);

        ActivityLog currentLog = createPastLogAndCurrentLog(user);

        updateUserScoreAndRanking(user, currentLog);
        user.updateLastFullScanAt();

        return RegisterUserResponse.of(user, currentLog, false);
    }

    private ActivityLog createPastLogAndCurrentLog(User user) {
        GitHubAllActivitiesResponse rawResponse
                = gitHubActivityService.fetchRawAllActivities(user.getUsername(), user.getGithubCreatedAt());

        int currentYear = LocalDate.now().getYear();
        int lastYear = currentYear - 1;

        if (user.getGithubCreatedAt().getYear() < currentYear) {
            GitHubActivitySummary baselineSummary = calculateSummaryUntilYear(rawResponse, lastYear);

            saveActivityLog(user, baselineSummary, LocalDate.of(lastYear, 12, 31));
        }

        GitHubActivitySummary totalSummary = calculateSummaryTotal(rawResponse);
        return saveActivityLog(user, totalSummary, LocalDate.now());
    }

    private GitHubActivitySummary calculateSummaryUntilYear(GitHubAllActivitiesResponse response, int targetYear) {
        int commits = 0;
        int issues = 0;
        int prs = 0;
        int reviews = 0;

        if (response.data() != null && response.data().getYearDataMap() != null) {
            for (Map.Entry<String, YearData> entry : response.data().getYearDataMap().entrySet()) {
                try {
                    int year = Integer.parseInt(entry.getKey().replace("year", ""));

                    if (year <= targetYear) {
                        ContributionsCollection collection = entry.getValue().contributionsCollection();
                        commits += collection.totalCommitContributions();
                        issues += collection.totalIssueContributions();
                        prs += collection.totalPullRequestContributions();
                        reviews += collection.totalPullRequestReviewContributions();
                    }
                } catch (NumberFormatException e) {
                    log.warn("연도 파싱 실패: {}", entry.getKey());
                }
            }
        }

        return new GitHubActivitySummary(commits, prs, 0, issues, reviews);
    }

    private GitHubActivitySummary calculateSummaryTotal(GitHubAllActivitiesResponse response) {
        return new GitHubActivitySummary(
                response.getCommitCount(),
                response.getPRCount(),
                response.getMergedPRCount(),
                response.getIssueCount(),
                response.getReviewCount()
        );
    }

    private ActivityLog saveActivityLog(User user, GitHubActivitySummary summary, LocalDate date) {
        ActivityLog activityLog = ActivityLog.builder()
                .user(user)
                .activityDate(date)
                .commitCount(summary.totalCommitCount())
                .issueCount(summary.totalIssueCount())
                .prCount(summary.totalPrOpenedCount())
                .mergedPrCount(summary.totalPrMergedCount())
                .reviewCount(summary.totalReviewCount())
                .diffCommitCount(0).diffIssueCount(0).diffPrCount(0)
                .diffMergedPrCount(0).diffReviewCount(0)
                .build();

        activityLogRepository.save(activityLog);
        return activityLog;
    }

    private void updateUserScoreAndRanking(User user, ActivityLog current) {
        GitHubActivitySummary summary = new GitHubActivitySummary(
                current.getCommitCount(),
                current.getPrCount(),
                current.getMergedPrCount(),
                current.getIssueCount(),
                current.getReviewCount()
        );

        int totalScore = summary.calculateTotalScore();
        user.updateScore(totalScore);

        RankingInfo rankingInfo = rankingService.calculateRankingForNewUser(totalScore);
        user.updateRankInfo(rankingInfo.ranking(), rankingInfo.percentile(), rankingInfo.tier());
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
}
