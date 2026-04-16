package com.gitranker.api.domain.log;

import com.gitranker.api.domain.user.User;
import com.gitranker.api.domain.user.vo.ActivityStatistics;
import com.gitranker.api.global.error.ErrorType;
import com.gitranker.api.global.error.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static com.gitranker.api.support.TestFixtures.activityLog;
import static com.gitranker.api.support.TestFixtures.stats;
import static com.gitranker.api.support.TestFixtures.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActivityLogServiceTest {

    @InjectMocks
    private ActivityLogService activityLogService;

    @Mock
    private ActivityLogRepository activityLogRepository;

    @Test
    @DisplayName("활동 로그 저장 시 총계와 diff를 반영한 로그를 저장한다")
    void savesActivityLog() {
        User user = user("alice");
        ActivityStatistics totals = stats(10, 2, 3, 1, 4);
        ActivityStatistics diff = stats(2, 1, 0, 0, 1);
        LocalDate logDate = LocalDate.of(2025, 1, 2);

        activityLogService.saveActivityLog(user, totals, diff, logDate);

        ArgumentCaptor<ActivityLog> captor = ArgumentCaptor.forClass(ActivityLog.class);
        verify(activityLogRepository).save(captor.capture());

        ActivityLog savedLog = captor.getValue();
        assertThat(savedLog.getActivityDate()).isEqualTo(logDate);
        assertThat(savedLog.getCommitCount()).isEqualTo(10);
        assertThat(savedLog.getIssueCount()).isEqualTo(2);
        assertThat(savedLog.getPrCount()).isEqualTo(3);
        assertThat(savedLog.getMergedPrCount()).isEqualTo(1);
        assertThat(savedLog.getReviewCount()).isEqualTo(4);
        assertThat(savedLog.getDiffCommitCount()).isEqualTo(2);
        assertThat(savedLog.getDiffIssueCount()).isEqualTo(1);
        assertThat(savedLog.getDiffPrCount()).isZero();
        assertThat(savedLog.getDiffMergedPrCount()).isZero();
        assertThat(savedLog.getDiffReviewCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("baseline 로그 저장 시 diff 값은 모두 0으로 저장된다")
    void savesBaselineLogWithZeroDiff() {
        User user = user("alice");
        ActivityStatistics baseline = stats(100, 10, 20, 15, 30);

        activityLogService.saveBaselineLog(user, baseline, LocalDate.of(2024, 12, 31));

        ArgumentCaptor<ActivityLog> captor = ArgumentCaptor.forClass(ActivityLog.class);
        verify(activityLogRepository).save(captor.capture());

        ActivityLog baselineLog = captor.getValue();
        assertThat(baselineLog.getCommitCount()).isEqualTo(100);
        assertThat(baselineLog.getIssueCount()).isEqualTo(10);
        assertThat(baselineLog.getPrCount()).isEqualTo(20);
        assertThat(baselineLog.getMergedPrCount()).isEqualTo(15);
        assertThat(baselineLog.getReviewCount()).isEqualTo(30);
        assertThat(baselineLog.getDiffCommitCount()).isZero();
        assertThat(baselineLog.getDiffIssueCount()).isZero();
        assertThat(baselineLog.getDiffPrCount()).isZero();
        assertThat(baselineLog.getDiffMergedPrCount()).isZero();
        assertThat(baselineLog.getDiffReviewCount()).isZero();
    }

    @Test
    @DisplayName("최신 로그가 있으면 Optional로 감싸 반환한다")
    void returnsLatestLogWhenItExists() {
        User user = user("alice");
        ActivityLog latestLog = activityLog(user, stats(5, 1, 1, 0, 2), stats(1, 0, 0, 0, 1));
        when(activityLogRepository.getTopByUserOrderByActivityDateDesc(user)).thenReturn(latestLog);

        Optional<ActivityLog> result = activityLogService.findLatestLog(user);

        assertThat(result).contains(latestLog);
    }

    @Test
    @DisplayName("findByDate와 findPreviousDayLog는 repository 결과를 그대로 위임한다")
    void delegatesDateBasedLookups() {
        User user = user("alice");
        LocalDate date = LocalDate.of(2025, 1, 2);
        ActivityLog sameDayLog = activityLog(user, stats(2, 2, 2, 2, 2), ActivityStatistics.empty());
        ActivityLog previousDayLog = activityLog(user, stats(1, 1, 1, 1, 1), ActivityStatistics.empty());

        when(activityLogRepository.findByUserAndActivityDate(user, date)).thenReturn(Optional.of(sameDayLog));
        when(activityLogRepository.findTopByUserAndActivityDateLessThanOrderByActivityDateDesc(user, date))
                .thenReturn(Optional.of(previousDayLog));

        assertThat(activityLogService.findByDate(user, date)).contains(sameDayLog);
        assertThat(activityLogService.findPreviousDayLog(user, date)).contains(previousDayLog);
    }

    @Test
    @DisplayName("최신 로그가 없으면 ACTIVITY_LOG_NOT_FOUND 예외가 발생한다")
    void throwsWhenLatestLogDoesNotExist() {
        User user = user("alice");
        when(activityLogRepository.getTopByUserOrderByActivityDateDesc(user)).thenReturn(null);

        assertThatThrownBy(() -> activityLogService.getLatestLog(user))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorType())
                .isEqualTo(ErrorType.ACTIVITY_LOG_NOT_FOUND);
    }

    @Test
    @DisplayName("기존 로그 업데이트 시 총계와 diff가 새 값으로 교체된다")
    void updatesExistingActivityLog() {
        User user = user("alice");
        ActivityLog activityLog = activityLog(user, stats(5, 1, 1, 0, 2), stats(1, 0, 0, 0, 1));
        ActivityStatistics newTotals = stats(20, 3, 5, 2, 7);
        ActivityStatistics newDiff = stats(3, 1, 1, 1, 2);

        activityLogService.updateActivityLog(activityLog, newTotals, newDiff);

        assertThat(activityLog.getCommitCount()).isEqualTo(20);
        assertThat(activityLog.getIssueCount()).isEqualTo(3);
        assertThat(activityLog.getPrCount()).isEqualTo(5);
        assertThat(activityLog.getMergedPrCount()).isEqualTo(2);
        assertThat(activityLog.getReviewCount()).isEqualTo(7);
        assertThat(activityLog.getDiffCommitCount()).isEqualTo(3);
        assertThat(activityLog.getDiffIssueCount()).isEqualTo(1);
        assertThat(activityLog.getDiffPrCount()).isEqualTo(1);
        assertThat(activityLog.getDiffMergedPrCount()).isEqualTo(1);
        assertThat(activityLog.getDiffReviewCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("baseline 로그 업데이트는 diff를 유지한 채 총계만 갱신한다")
    void updatesBaselineLogWithoutTouchingDiff() {
        User user = user("alice");
        ActivityLog baselineLog = ActivityLog.baseline(user, stats(10, 2, 3, 4, 5), LocalDate.of(2024, 12, 31));

        activityLogService.updateBaselineLog(baselineLog, stats(30, 20, 10, 5, 1));

        assertThat(baselineLog.getCommitCount()).isEqualTo(30);
        assertThat(baselineLog.getIssueCount()).isEqualTo(20);
        assertThat(baselineLog.getPrCount()).isEqualTo(10);
        assertThat(baselineLog.getMergedPrCount()).isEqualTo(5);
        assertThat(baselineLog.getReviewCount()).isEqualTo(1);
        assertThat(baselineLog.getDiffCommitCount()).isZero();
        assertThat(baselineLog.getDiffReviewCount()).isZero();
    }
}
