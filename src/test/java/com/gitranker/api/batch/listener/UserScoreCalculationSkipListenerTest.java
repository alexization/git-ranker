package com.gitranker.api.batch.listener;

import com.gitranker.api.domain.failure.BatchFailureLogService;
import com.gitranker.api.domain.user.User;
import com.gitranker.api.global.error.ErrorType;
import com.gitranker.api.global.error.exception.GitHubApiNonRetryableException;
import com.gitranker.api.global.error.exception.GitHubApiRetryableException;
import com.gitranker.api.support.TestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserScoreCalculationSkipListenerTest {

    private static final String JOB_NAME_DAILY_SCORE = "DailyScoreRecalculationJob";
    private static final String UNKNOWN_USER_ID = "UNKNOWN_USER";

    @InjectMocks
    private UserScoreCalculationSkipListener listener;

    @Mock
    private BatchFailureLogService batchFailureLogService;

    @Test
    @DisplayName("read skip stores retryable error metadata for unknown user")
    void logsRetryableReadSkip() {
        listener.onSkipInRead(new GitHubApiRetryableException(ErrorType.GITHUB_API_TIMEOUT, "timeout"));

        verify(batchFailureLogService).saveFailureLog(
                JOB_NAME_DAILY_SCORE,
                UNKNOWN_USER_ID,
                ErrorType.GITHUB_API_TIMEOUT,
                "[READ_PHASE] error.github.api-timeout: timeout"
        );
    }

    @Test
    @DisplayName("process skip stores default error type for generic failures")
    void logsGenericProcessSkip() {
        User user = TestFixtures.user("alice");

        listener.onSkipInProcess(user, new IllegalStateException("boom"));

        verify(batchFailureLogService).saveFailureLog(
                JOB_NAME_DAILY_SCORE,
                "alice",
                ErrorType.DEFAULT_ERROR,
                "[PROCESS_PHASE] boom"
        );
    }

    @Test
    @DisplayName("write skip stores non-retryable GitHub error for the target user")
    void logsNonRetryableWriteSkip() {
        User user = TestFixtures.user("alice");

        listener.onSkipInWrite(user, new GitHubApiNonRetryableException(ErrorType.GITHUB_USER_NOT_FOUND));

        verify(batchFailureLogService).saveFailureLog(
                JOB_NAME_DAILY_SCORE,
                "alice",
                ErrorType.GITHUB_USER_NOT_FOUND,
                "[WRITE_PHASE] error.github.user-not-found"
        );
    }
}
