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
        assertThat(savedLog.getDiffCommitCount()).isEqualTo(2);
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
        assertThat(baselineLog.getDiffCommitCount()).isZero();
        assertThat(baselineLog.getDiffIssueCount()).isZero();
        assertThat(baselineLog.getDiffReviewCount()).isZero();
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
        assertThat(activityLog.getPrCount()).isEqualTo(5);
        assertThat(activityLog.getDiffMergedPrCount()).isEqualTo(1);
    }
}
