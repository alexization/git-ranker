package com.gitranker.api.domain.log;

import com.gitranker.api.domain.user.User;
import com.gitranker.api.domain.user.vo.ActivityStatistics;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Activity Log 생성 및 업데이트 조율을 담당하는 컴포넌트.
 * 신규 사용자 등록, 수동 갱신 시 Activity Log 관리 로직을 캡슐화합니다.
 */
@Component
@RequiredArgsConstructor
public class ActivityLogOrchestrator {

    private final ActivityLogService activityLogService;

    public void createLogsForNewUser(User user,
                                     ActivityStatistics totalStats,
                                     ActivityStatistics baselineStats) {
        LocalDate today = LocalDate.now();
        ActivityStatistics zeroDiff = ActivityStatistics.empty();

        if (baselineStats != null) {
            int lastYear = today.getYear() - 1;
            activityLogService.saveBaselineLog(user, baselineStats, LocalDate.of(lastYear, 12, 31));
        }

        activityLogService.saveActivityLog(user, totalStats, zeroDiff, today);
    }

    public void updateLogsForRefresh(User user,
                                     ActivityStatistics totalStats,
                                     ActivityStatistics baselineStats) {
        LocalDate today = LocalDate.now();
        int currentYear = today.getYear();
        LocalDate baselineDate = LocalDate.of(currentYear - 1, 12, 31);

        if (baselineStats != null) {
            activityLogService.findByDate(user, baselineDate)
                    .ifPresent(baselineLog -> activityLogService.updateBaselineLog(baselineLog, baselineStats));
        }

        activityLogService.findByDate(user, today)
                .ifPresentOrElse(
                        todayLog -> updateExistingTodayLog(user, todayLog, today, totalStats),
                        () -> createNewTodayLog(user, today, totalStats)
                );
    }

    private void updateExistingTodayLog(User user,
                                        ActivityLog todayLog,
                                        LocalDate today,
                                        ActivityStatistics totalStats) {
        activityLogService.findPreviousDayLog(user, today)
                .ifPresentOrElse(
                        previousLog -> {
                            ActivityStatistics previousStats = previousLog.toStatistics();
                            ActivityStatistics diff = totalStats.calculateDiff(previousStats);
                            activityLogService.updateActivityLog(todayLog, totalStats, diff);
                        },
                        () -> activityLogService.updateActivityLog(todayLog, totalStats, ActivityStatistics.empty())
                );
    }

    private void createNewTodayLog(User user, LocalDate today, ActivityStatistics totalStats) {
        activityLogService.findPreviousDayLog(user, today)
                .ifPresentOrElse(
                        previousLog -> {
                            ActivityStatistics previousStats = previousLog.toStatistics();
                            ActivityStatistics diff = totalStats.calculateDiff(previousStats);
                            activityLogService.saveActivityLog(user, totalStats, diff, today);
                        },
                        () -> activityLogService.saveActivityLog(user, totalStats, ActivityStatistics.empty(), today)
                );
    }
}
