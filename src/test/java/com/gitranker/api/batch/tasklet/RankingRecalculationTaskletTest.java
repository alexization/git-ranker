package com.gitranker.api.batch.tasklet;

import com.gitranker.api.domain.user.UserRepository;
import com.gitranker.api.global.error.ErrorType;
import com.gitranker.api.global.error.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.scope.context.StepContext;
import org.springframework.batch.repeat.RepeatStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RankingRecalculationTaskletTest {

    @InjectMocks
    private RankingRecalculationTasklet tasklet;

    @Mock
    private UserRepository userRepository;

    @Test
    @DisplayName("tasklet performs ranking bulk update and finishes")
    void recalculatesRankings() throws Exception {
        RepeatStatus status = tasklet.execute(stepContribution(), chunkContext());

        assertThat(status).isEqualTo(RepeatStatus.FINISHED);
        verify(userRepository).bulkUpdateRanking();
    }

    @Test
    @DisplayName("tasklet wraps bulk update failures with batch-step exception")
    void wrapsBulkUpdateFailure() {
        doThrow(new RuntimeException("bulk failed")).when(userRepository).bulkUpdateRanking();

        assertThatThrownBy(() -> tasklet.execute(stepContribution(), chunkContext()))
                .isInstanceOf(BusinessException.class)
                .satisfies(throwable -> {
                    BusinessException exception = (BusinessException) throwable;
                    assertThat(exception.getErrorType()).isEqualTo(ErrorType.BATCH_STEP_FAILED);
                    assertThat(exception.getData()).isEqualTo("랭킹 재산정 실패");
                });
    }

    private StepContribution stepContribution() {
        StepExecution stepExecution = new StepExecution(
                "rankingStep",
                new JobExecution(new JobInstance(1L, "DailyScoreRecalculationJob"), new org.springframework.batch.core.JobParameters())
        );
        return new StepContribution(stepExecution);
    }

    private ChunkContext chunkContext() {
        StepExecution stepExecution = new StepExecution(
                "rankingStep",
                new JobExecution(new JobInstance(1L, "DailyScoreRecalculationJob"), new org.springframework.batch.core.JobParameters())
        );
        return new ChunkContext(new StepContext(stepExecution));
    }
}
