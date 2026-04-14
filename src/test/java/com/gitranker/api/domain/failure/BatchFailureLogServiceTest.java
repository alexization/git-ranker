package com.gitranker.api.domain.failure;

import com.gitranker.api.global.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BatchFailureLogServiceTest {

    @InjectMocks
    private BatchFailureLogService batchFailureLogService;

    @Mock
    private BatchFailureLogRepository batchFailureLogRepository;

    @Test
    @DisplayName("배치 실패 로그 저장 요청이 오면 repository에 구조화된 로그를 저장한다")
    void savesBatchFailureLog() {
        batchFailureLogService.saveFailureLog("daily-job", "alice", ErrorType.BATCH_JOB_FAILED, "something went wrong");

        ArgumentCaptor<BatchFailureLog> captor = ArgumentCaptor.forClass(BatchFailureLog.class);
        verify(batchFailureLogRepository).save(captor.capture());

        BatchFailureLog savedLog = captor.getValue();
        assertThat(savedLog.getJobName()).isEqualTo("daily-job");
        assertThat(savedLog.getTargetId()).isEqualTo("alice");
        assertThat(savedLog.getErrorType()).isEqualTo(ErrorType.BATCH_JOB_FAILED);
        assertThat(savedLog.getErrorMessage()).isEqualTo("something went wrong");
    }

    @Test
    @DisplayName("실패 로그 저장 중 예외가 나더라도 외부로 전파하지 않는다")
    void swallowsRepositoryFailure() {
        doThrow(new RuntimeException("db error")).when(batchFailureLogRepository).save(any(BatchFailureLog.class));

        assertThatNoException()
                .isThrownBy(() -> batchFailureLogService.saveFailureLog("daily-job", "alice", ErrorType.BATCH_JOB_FAILED, "error"));
    }
}
