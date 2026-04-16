package com.gitranker.api.domain.log;

import com.gitranker.api.domain.user.User;
import com.gitranker.api.domain.user.vo.ActivityStatistics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static com.gitranker.api.support.TestFixtures.activityLog;
import static com.gitranker.api.support.TestFixtures.stats;
import static com.gitranker.api.support.TestFixtures.user;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActivityLogOrchestratorTest {

    @InjectMocks
    private ActivityLogOrchestrator activityLogOrchestrator;

    @Mock
    private ActivityLogService activityLogService;

    @Test
    @DisplayName("신규 사용자에 baseline 통계가 있으면 작년 말 baseline과 오늘 로그를 함께 저장한다")
    void createsBaselineAndTodayLogForNewUser() {
        User user = user("alice");
        ActivityStatistics totalStats = stats(10, 2, 3, 4, 5);
        ActivityStatistics baselineStats = stats(4, 1, 1, 2, 2);
        LocalDate today = LocalDate.now();

        activityLogOrchestrator.createLogsForNewUser(user, totalStats, baselineStats);

        verify(activityLogService).saveBaselineLog(user, baselineStats, LocalDate.of(today.getYear() - 1, 12, 31));
        verify(activityLogService).saveActivityLog(user, totalStats, ActivityStatistics.empty(), today);
    }

    @Test
    @DisplayName("신규 사용자에 baseline 통계가 없으면 오늘 로그만 저장한다")
    void skipsBaselineLogWhenNotProvidedForNewUser() {
        User user = user("alice");
        ActivityStatistics totalStats = stats(10, 2, 3, 4, 5);
        LocalDate today = LocalDate.now();

        activityLogOrchestrator.createLogsForNewUser(user, totalStats, null);

        verify(activityLogService, never()).saveBaselineLog(any(User.class), any(ActivityStatistics.class), any(LocalDate.class));
        verify(activityLogService).saveActivityLog(user, totalStats, ActivityStatistics.empty(), today);
    }

    @Test
    @DisplayName("오늘 로그와 전일 로그가 있으면 refresh 시 diff를 계산해 오늘 로그를 갱신한다")
    void updatesExistingTodayLogWithPreviousDayDiff() {
        User user = user("alice");
        ActivityLog todayLog = activityLog(user, stats(4, 1, 1, 0, 2), ActivityStatistics.empty());
        ActivityLog previousLog = activityLog(user, stats(8, 2, 1, 2, 3), ActivityStatistics.empty());
        ActivityStatistics totalStats = stats(12, 5, 4, 3, 6);
        ActivityStatistics baselineStats = stats(2, 1, 1, 0, 1);
        LocalDate today = LocalDate.now();
        LocalDate baselineDate = LocalDate.of(today.getYear() - 1, 12, 31);

        when(activityLogService.findByDate(user, baselineDate)).thenReturn(Optional.of(previousLog));
        when(activityLogService.findByDate(user, today)).thenReturn(Optional.of(todayLog));
        when(activityLogService.findPreviousDayLog(user, today)).thenReturn(Optional.of(previousLog));

        activityLogOrchestrator.updateLogsForRefresh(user, totalStats, baselineStats);

        verify(activityLogService).updateBaselineLog(previousLog, baselineStats);
        verify(activityLogService).updateActivityLog(todayLog, totalStats, totalStats.calculateDiff(previousLog.toStatistics()));
    }

    @Test
    @DisplayName("오늘 로그는 있지만 전일 로그가 없으면 zero diff로 오늘 로그를 갱신한다")
    void updatesExistingTodayLogWithZeroDiffWhenPreviousLogIsMissing() {
        User user = user("alice");
        ActivityLog todayLog = activityLog(user, stats(4, 1, 1, 0, 2), ActivityStatistics.empty());
        ActivityStatistics totalStats = stats(12, 5, 4, 3, 6);
        LocalDate today = LocalDate.now();

        when(activityLogService.findByDate(user, today)).thenReturn(Optional.of(todayLog));
        when(activityLogService.findPreviousDayLog(user, today)).thenReturn(Optional.empty());

        activityLogOrchestrator.updateLogsForRefresh(user, totalStats, null);

        verify(activityLogService).updateActivityLog(todayLog, totalStats, ActivityStatistics.empty());
        verify(activityLogService, never()).updateBaselineLog(any(ActivityLog.class), any(ActivityStatistics.class));
    }

    @Test
    @DisplayName("오늘 로그가 없고 전일 로그가 있으면 diff를 계산해 새 오늘 로그를 저장한다")
    void createsTodayLogWithPreviousDayDiff() {
        User user = user("alice");
        ActivityLog previousLog = activityLog(user, stats(8, 2, 1, 2, 3), ActivityStatistics.empty());
        ActivityStatistics totalStats = stats(12, 5, 4, 3, 6);
        LocalDate today = LocalDate.now();

        when(activityLogService.findByDate(user, today)).thenReturn(Optional.empty());
        when(activityLogService.findPreviousDayLog(user, today)).thenReturn(Optional.of(previousLog));

        activityLogOrchestrator.updateLogsForRefresh(user, totalStats, null);

        verify(activityLogService).saveActivityLog(user, totalStats, totalStats.calculateDiff(previousLog.toStatistics()), today);
    }

    @Test
    @DisplayName("오늘 로그와 전일 로그가 모두 없으면 zero diff로 새 오늘 로그를 저장한다")
    void createsTodayLogWithZeroDiffWhenNoHistoryExists() {
        User user = user("alice");
        ActivityStatistics totalStats = stats(12, 5, 4, 3, 6);
        LocalDate today = LocalDate.now();
        LocalDate baselineDate = LocalDate.of(today.getYear() - 1, 12, 31);

        when(activityLogService.findByDate(user, baselineDate)).thenReturn(Optional.empty());
        when(activityLogService.findByDate(user, today)).thenReturn(Optional.empty());
        when(activityLogService.findPreviousDayLog(user, today)).thenReturn(Optional.empty());

        activityLogOrchestrator.updateLogsForRefresh(user, totalStats, stats(1, 1, 1, 1, 1));

        verify(activityLogService).saveActivityLog(user, totalStats, ActivityStatistics.empty(), today);
        verify(activityLogService, never()).updateBaselineLog(any(ActivityLog.class), any(ActivityStatistics.class));
    }
}
