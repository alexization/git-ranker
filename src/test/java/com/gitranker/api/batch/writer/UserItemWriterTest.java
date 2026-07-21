package com.gitranker.api.batch.writer;

import com.gitranker.api.batch.dto.ScoredUserUpdate;
import com.gitranker.api.domain.log.ActivityLog;
import com.gitranker.api.domain.log.ActivityLogService;
import com.gitranker.api.domain.user.User;
import com.gitranker.api.domain.user.UserRepository;
import com.gitranker.api.domain.user.vo.ActivityStatistics;
import com.gitranker.api.global.error.ErrorType;
import com.gitranker.api.global.error.exception.BusinessException;
import com.gitranker.api.support.TestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.item.Chunk;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserItemWriterTest {

    @InjectMocks
    private UserItemWriter userItemWriter;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ActivityLogService activityLogService;

    private ScoredUserUpdate update(User user, ActivityStatistics stats, ActivityStatistics diff, LocalDate date) {
        return new ScoredUserUpdate(user, stats, diff, date);
    }

    @Test
    @DisplayName("청크의 모든 사용자를 저장한다")
    void savesChunkUsers() throws Exception {
        LocalDate today = LocalDate.now();
        User first = TestFixtures.user("alice");
        User second = TestFixtures.user("bob");
        Chunk<ScoredUserUpdate> chunk = new Chunk<>(List.of(
                update(first, TestFixtures.stats(1, 1, 1, 1, 1), ActivityStatistics.empty(), today),
                update(second, TestFixtures.stats(2, 2, 2, 2, 2), ActivityStatistics.empty(), today)
        ));

        userItemWriter.write(chunk);

        verify(userRepository).saveAll(List.of(first, second));
    }

    @Test
    @DisplayName("해당 일자 로그가 없으면 활동 로그를 새로 저장한다")
    void savesActivityLogWhenAbsent() throws Exception {
        LocalDate today = LocalDate.now();
        User user = TestFixtures.user("alice");
        ActivityStatistics stats = TestFixtures.stats(5, 2, 1, 3, 4);
        ActivityStatistics diff = TestFixtures.stats(1, 0, 0, 1, 0);
        Chunk<ScoredUserUpdate> chunk = new Chunk<>(List.of(update(user, stats, diff, today)));

        when(activityLogService.findByDate(user, today)).thenReturn(Optional.empty());

        userItemWriter.write(chunk);

        verify(activityLogService).saveActivityLog(user, stats, diff, today);
        verify(activityLogService, never()).updateActivityLog(any(), any(), any());
    }

    @Test
    @DisplayName("해당 일자 로그가 있으면 새로 저장하지 않고 기존 로그를 갱신한다")
    void updatesActivityLogWhenPresent() throws Exception {
        LocalDate today = LocalDate.now();
        User user = TestFixtures.user("alice");
        ActivityStatistics stats = TestFixtures.stats(5, 2, 1, 3, 4);
        ActivityStatistics diff = TestFixtures.stats(1, 0, 0, 1, 0);
        ActivityLog existingLog = ActivityLog.empty(user, today);
        Chunk<ScoredUserUpdate> chunk = new Chunk<>(List.of(update(user, stats, diff, today)));

        when(activityLogService.findByDate(user, today)).thenReturn(Optional.of(existingLog));

        userItemWriter.write(chunk);

        verify(activityLogService).updateActivityLog(existingLog, stats, diff);
        verify(activityLogService, never()).saveActivityLog(any(), any(), any(), eq(today));
    }

    @Test
    @DisplayName("리포지토리 실패는 batch-step 예외로 감싼다")
    void wrapsRepositoryFailure() {
        LocalDate today = LocalDate.now();
        Chunk<ScoredUserUpdate> chunk = new Chunk<>(List.of(
                update(TestFixtures.user("alice"), TestFixtures.stats(1, 1, 1, 1, 1), ActivityStatistics.empty(), today)
        ));

        when(userRepository.saveAll(anyList())).thenThrow(new RuntimeException("db down"));

        assertThatThrownBy(() -> userItemWriter.write(chunk))
                .isInstanceOf(BusinessException.class)
                .satisfies(throwable -> {
                    BusinessException exception = (BusinessException) throwable;
                    assertThat(exception.getErrorType()).isEqualTo(ErrorType.BATCH_STEP_FAILED);
                    assertThat(exception.getData()).isEqualTo("DB 저장 실패");
                });
    }
}
