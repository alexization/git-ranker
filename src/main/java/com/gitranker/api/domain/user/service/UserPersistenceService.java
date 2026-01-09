package com.gitranker.api.domain.user.service;

import com.gitranker.api.domain.log.ActivityLogService;
import com.gitranker.api.domain.user.User;
import com.gitranker.api.domain.user.UserRepository;
import com.gitranker.api.domain.user.vo.ActivityStatistics;
import com.gitranker.api.global.error.ErrorType;
import com.gitranker.api.global.error.exception.BusinessException;
import com.gitranker.api.infrastructure.github.dto.GitHubUserInfoResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserPersistenceService {

    private final UserRepository userRepository;
    private final ActivityLogService activityLogService;

    @Transactional
    public User saveNewUser(GitHubUserInfoResponse githubUserInfo, ActivityStatistics totalStats, ActivityStatistics baselineStats) {
        User newUser = User.builder()
                .nodeId(githubUserInfo.getNodeId())
                .username(githubUserInfo.getLogin())
                .profileImage(githubUserInfo.getAvatarUrl())
                .githubCreatedAt(githubUserInfo.getGitHubCreatedAt())
                .build();

        int newScore = totalStats.calculateScore().getValue();
        long higherScoreCount = userRepository.countByScoreValueGreaterThan(newScore);
        long totalUserCount = userRepository.count() + 1;

        newUser.updateActivityStatistics(totalStats, higherScoreCount, totalUserCount);
        userRepository.save(newUser);

        saveActivityLogs(newUser, totalStats, baselineStats);

        return newUser;
    }

    @Transactional
    public User updateUserStatistics(Long userId, ActivityStatistics statistics) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorType.USER_NOT_FOUND));

        int newScore = statistics.calculateScore().getValue();
        long higherScoreCount = userRepository.countByScoreValueGreaterThan(newScore);
        long totalUserCount = userRepository.count();

        user.updateActivityStatistics(statistics, higherScoreCount, totalUserCount);
        user.recordFullScan();

        activityLogService.saveActivityLog(user, statistics, LocalDate.now());

        return user;
    }

    @Transactional
    public User updateProfile(User user, String newUsername, String newProfileImage) {
        user.changeProfile(newUsername, newProfileImage);

        return userRepository.save(user);
    }

    private void saveActivityLogs(User user,
                                  ActivityStatistics totalStats,
                                  ActivityStatistics baselineStats) {
        if (baselineStats != null) {
            int lastYear = LocalDate.now().getYear() - 1;
            activityLogService.saveBaselineLog(user, baselineStats, LocalDate.of(lastYear, 12, 31));
        }

        activityLogService.saveActivityLog(user, totalStats, LocalDate.now());
    }
}
