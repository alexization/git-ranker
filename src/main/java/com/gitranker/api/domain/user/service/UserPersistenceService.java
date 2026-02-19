package com.gitranker.api.domain.user.service;

import com.gitranker.api.domain.log.ActivityLogOrchestrator;
import com.gitranker.api.domain.ranking.RankingRecalculationService;
import com.gitranker.api.domain.user.User;
import com.gitranker.api.domain.user.UserRepository;
import com.gitranker.api.domain.user.vo.ActivityStatistics;
import com.gitranker.api.global.error.ErrorType;
import com.gitranker.api.global.error.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserPersistenceService {

    private final UserRepository userRepository;
    private final ActivityLogOrchestrator activityLogOrchestrator;
    private final RankingRecalculationService rankingRecalculationService;

    @Transactional
    public User saveNewUser(User newUser, ActivityStatistics totalStats, ActivityStatistics baselineStats) {
        int newScore = totalStats.calculateScore().getValue();
        long higherScoreCount = userRepository.countByScoreValueGreaterThan(newScore);
        long totalUserCount = userRepository.count() + 1;

        newUser.updateActivityStatistics(totalStats, higherScoreCount, totalUserCount);
        userRepository.save(newUser);

        activityLogOrchestrator.createLogsForNewUser(newUser, totalStats, baselineStats);

        rankingRecalculationService.recalculateIfNeeded();

        return newUser;
    }

    @Transactional
    public User updateProfile(User user, String newUsername, String newProfileImage, String newEmail) {
        user.updateProfile(newUsername, newProfileImage, newEmail);

        return userRepository.save(user);
    }

    @Transactional
    public User updateUserStatisticsWithLog(Long userId,
                                            ActivityStatistics totalStats,
                                            ActivityStatistics baselineStats) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorType.USER_NOT_FOUND));

        int newScore = totalStats.calculateScore().getValue();
        long higherScoreCount = userRepository.countByScoreValueGreaterThan(newScore);
        long totalUserCount = userRepository.count();

        user.updateActivityStatistics(totalStats, higherScoreCount, totalUserCount);
        user.recordFullScan();

        activityLogOrchestrator.updateLogsForRefresh(user, totalStats, baselineStats);

        rankingRecalculationService.recalculateIfNeeded();

        return user;
    }
}
