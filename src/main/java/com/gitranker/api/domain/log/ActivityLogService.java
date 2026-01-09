package com.gitranker.api.domain.log;

import com.gitranker.api.domain.user.User;
import com.gitranker.api.domain.user.vo.ActivityStatistics;
import com.gitranker.api.global.logging.EventType;
import com.gitranker.api.global.logging.LogCategory;
import com.gitranker.api.global.logging.MdcUtils;
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
        MdcUtils.setLogContext(LogCategory.DOMAIN, EventType.REQUEST);

        ActivityLog activityLog = ActivityLog.builder()
                .user(user)
                .activityDate(logDate)
                .commitCount(currentStats.getCommitCount())
                .issueCount(currentStats.getIssueCount())
                .prCount(currentStats.getPrOpenedCount())
                .mergedPrCount(currentStats.getPrMergedCount())
                .reviewCount(currentStats.getReviewCount())
                .diffCommitCount(diffStats.getCommitCount())
                .diffIssueCount(diffStats.getIssueCount())
                .diffPrCount(diffStats.getPrOpenedCount())
                .diffMergedPrCount(diffStats.getPrMergedCount())
                .diffReviewCount(diffStats.getReviewCount())
                .build();

        activityLogRepository.save(activityLog);

        MdcUtils.setEventType(EventType.SUCCESS);
        log.info("활동 로그 저장 완료 - 사용자: {}, 일자: {}", user.getUsername(), logDate);
    }

    @Transactional
    public void saveBaselineLog(User user, ActivityStatistics baselineStats, LocalDate baselineDate) {
        ActivityLog baselineLog = ActivityLog.builder()
                .user(user)
                .activityDate(baselineDate)
                .commitCount(baselineStats.getCommitCount())
                .issueCount(baselineStats.getIssueCount())
                .prCount(baselineStats.getPrOpenedCount())
                .mergedPrCount(baselineStats.getPrMergedCount())
                .reviewCount(baselineStats.getReviewCount())
                .diffCommitCount(0)
                .diffIssueCount(0)
                .diffPrCount(0)
                .diffMergedPrCount(0)
                .diffReviewCount(0)
                .build();

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
                .orElseThrow(() -> new IllegalArgumentException("활동 로그가 없습니다."));
    }

    public ActivityStatistics toStatistics(ActivityLog log) {
        return ActivityStatistics.of(
                log.getCommitCount(),
                log.getIssueCount(),
                log.getPrCount(),
                log.getMergedPrCount(),
                log.getReviewCount()
        );
    }
}
