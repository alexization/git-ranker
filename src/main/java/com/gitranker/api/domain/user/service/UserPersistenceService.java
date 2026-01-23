package com.gitranker.api.domain.user.service;

import com.gitranker.api.domain.log.ActivityLogService;
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

import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserPersistenceService {

    private final UserRepository userRepository;
    private final ActivityLogService activityLogService;
    private final RankingRecalculationService rankingRecalculationService;

    @Transactional
    public User saveNewUser(User newUser, ActivityStatistics totalStats, ActivityStatistics baselineStats) {
        int newScore = totalStats.calculateScore().getValue();
        long higherScoreCount = userRepository.countByScoreValueGreaterThan(newScore);
        long totalUserCount = userRepository.count() + 1;

        newUser.updateActivityStatistics(totalStats, higherScoreCount, totalUserCount);
        userRepository.save(newUser);

        saveNewUserActivityLogs(newUser, totalStats, baselineStats);

        rankingRecalculationService.recalculateIfNeeded();

        return newUser;
    }

    @Transactional
    public User updateUserStatisticsWithoutLog(Long userId, ActivityStatistics newStats) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorType.USER_NOT_FOUND));

        int newScore = newStats.calculateScore().getValue();
        long higherScoreCount = userRepository.countByScoreValueGreaterThan(newScore);
        long totalUserCount = userRepository.count();

        user.updateActivityStatistics(newStats, higherScoreCount, totalUserCount);
        user.recordFullScan();

        rankingRecalculationService.recalculateIfNeeded();

        return user;
    }

    @Transactional
    public User updateProfile(User user, String newUsername, String newProfileImage) {
        user.updateProfile(newUsername, newProfileImage, null);

        return userRepository.save(user);
    }

    private void saveNewUserActivityLogs(User user,
                                         ActivityStatistics totalStats,
                                         ActivityStatistics baselineStats) {
        LocalDate today = LocalDate.now();
        ActivityStatistics zeroDiff = ActivityStatistics.zeroDiff();

        if (baselineStats != null) {
            int lastYear = today.getYear() - 1;
            activityLogService.saveBaselineLog(user, baselineStats, LocalDate.of(lastYear, 12, 31));
        }

        activityLogService.saveActivityLog(user, totalStats, zeroDiff, today);
    }
}
