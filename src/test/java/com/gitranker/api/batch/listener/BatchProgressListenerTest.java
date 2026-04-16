package com.gitranker.api.batch.listener;

import com.gitranker.api.domain.user.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.scope.context.StepContext;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BatchProgressListenerTest {

    @InjectMocks
    private BatchProgressListener listener;

    @Mock
    private UserRepository userRepository;

    @Test
    @DisplayName("beforeChunk initializes total count once from repository")
    void initializesTotalCountOnlyOnce() {
        when(userRepository.count()).thenReturn(42L, 100L);

        listener.beforeChunk(chunkContext("DailyScoreRecalculationJob", 0));
        listener.beforeChunk(chunkContext("DailyScoreRecalculationJob", 0));

        verify(userRepository, times(1)).count();
        assertThat(ReflectionTestUtils.getField(listener, "totalCount")).isEqualTo(42);
        assertThat(ReflectionTestUtils.getField(listener, "lastLoggedPercentage")).isEqualTo(0);
    }

    @Test
    @DisplayName("afterChunk does nothing when total count is zero")
    void ignoresAfterChunkWhenNothingInitialized() {
        listener.afterChunk(chunkContext("DailyScoreRecalculationJob", 25));

        assertThat(ReflectionTestUtils.getField(listener, "totalCount")).isEqualTo(0);
        assertThat(ReflectionTestUtils.getField(listener, "lastLoggedPercentage")).isEqualTo(0);
    }

    @Test
    @DisplayName("afterChunk rounds progress down to the nearest ten percent interval")
    void updatesProgressAtInterval() {
        ReflectionTestUtils.setField(listener, "totalCount", 100);
        ReflectionTestUtils.setField(listener, "lastLoggedPercentage", 0);

        listener.afterChunk(chunkContext("DailyScoreRecalculationJob", 27));
        assertThat(ReflectionTestUtils.getField(listener, "lastLoggedPercentage")).isEqualTo(20);

        listener.afterChunk(chunkContext("DailyScoreRecalculationJob", 29));
        assertThat(ReflectionTestUtils.getField(listener, "lastLoggedPercentage")).isEqualTo(20);

        listener.afterChunk(chunkContext("DailyScoreRecalculationJob", 31));
        assertThat(ReflectionTestUtils.getField(listener, "lastLoggedPercentage")).isEqualTo(30);
    }

    @Test
    @DisplayName("reset clears cached progress state")
    void resetsCachedProgressState() {
        ReflectionTestUtils.setField(listener, "totalCount", 100);
        ReflectionTestUtils.setField(listener, "lastLoggedPercentage", 40);

        listener.reset();

        assertThat(ReflectionTestUtils.getField(listener, "totalCount")).isEqualTo(0);
        assertThat(ReflectionTestUtils.getField(listener, "lastLoggedPercentage")).isEqualTo(0);
    }

    private ChunkContext chunkContext(String jobName, long writeCount) {
        JobExecution jobExecution = new JobExecution(new JobInstance(1L, jobName), new JobParameters());
        StepExecution stepExecution = jobExecution.createStepExecution("scoreRecalculationStep");
        stepExecution.setWriteCount(writeCount);
        return new ChunkContext(new StepContext(stepExecution));
    }
}
