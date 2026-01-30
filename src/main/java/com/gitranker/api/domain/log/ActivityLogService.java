package com.gitranker.api.domain.log;

import com.gitranker.api.domain.user.User;
import com.gitranker.api.domain.user.vo.ActivityStatistics;
import com.gitranker.api.global.error.ErrorType;
import com.gitranker.api.global.error.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityLogService {

    private final ActivityLogRepository activityLogRepository;

    @Transactional
    public void saveActivityLog(User user, ActivityStatistics currentStats, ActivityStatistics diffStats, LocalDate logDate) {
        ActivityLog activityLog = ActivityLog.of(user, currentStats, diffStats, logDate);

        activityLogRepository.save(activityLog);

        log.debug("활동 로그 저장 완료 - 사용자: {}, 일자: {}", user.getUsername(), logDate);
    }

    @Transactional
    public void saveBaselineLog(User user, ActivityStatistics baselineStats, LocalDate baselineDate) {

        ActivityLog baselineLog = ActivityLog.baseline(user, baselineStats, baselineDate);

        activityLogRepository.save(baselineLog);

        log.debug("베이스라인 로그 저장 - 사용자: {}, 기준일: {}", user.getUsername(), baselineDate);
    }

    @Transactional(readOnly = true)
    public Optional<ActivityLog> findLatestLog(User user) {
        ActivityLog log = activityLogRepository.getTopByUserOrderByActivityDateDesc(user);

        return Optional.ofNullable(log);
    }

    @Transactional(readOnly = true)
    public ActivityLog getLatestLog(User user) {
        return findLatestLog(user)
                .orElseThrow(() -> new BusinessException(ErrorType.ACTIVITY_LOG_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public Optional<ActivityLog> findByDate(User user, LocalDate date) {
        return activityLogRepository.findByUserAndActivityDate(user, date);
    }

    @Transactional
    public void updateActivityLog(ActivityLog activityLog, ActivityStatistics newStats, ActivityStatistics diff) {
        activityLog.updateStatisticsWithDiff(newStats, diff);
    }

    @Transactional
    public void updateBaselineLog(ActivityLog baselineLog, ActivityStatistics newBaselineStats) {
        baselineLog.updateStatistics(newBaselineStats);
    }

    @Transactional(readOnly = true)
    public Optional<ActivityLog> findPreviousDayLog(User user, LocalDate date) {
        return activityLogRepository.findTopByUserAndActivityDateLessThanOrderByActivityDateDesc(user, date);
    }
}
