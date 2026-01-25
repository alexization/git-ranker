package com.gitranker.api.batch.listener;

import com.gitranker.api.domain.failure.BatchFailureLogService;
import com.gitranker.api.domain.user.User;
import com.gitranker.api.global.error.ErrorType;
import com.gitranker.api.global.error.exception.GitHubApiNonRetryableException;
import com.gitranker.api.global.error.exception.GitHubApiRetryableException;
import com.gitranker.api.global.logging.Event;
import com.gitranker.api.global.logging.LogContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.SkipListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserScoreCalculationSkipListener implements SkipListener<User, User> {

    private static final String JOB_NAME = "DailyScoreRecalculationJob";
    private final BatchFailureLogService batchFailureLogService;

    @Override
    public void onSkipInRead(Throwable t) {
        LogContext.event(Event.BATCH_ITEM_FAILED)
                .with("username", "UNKNOWN")
                .with("phase", "READ")
                .with("error_type", t.getClass().getSimpleName())
                .with("error_message", t.getMessage())
                .with("retryable", isRetryableError(t))
                .error();

        saveFailureLog("UNKNOWN_USER", t, "READ_PHASE");
    }

    @Override
    public void onSkipInWrite(User user, Throwable t) {
        LogContext.event(Event.BATCH_ITEM_FAILED)
                .with("username", user.getUsername())
                .with("phase", "WRITE")
                .with("error_type", t.getClass().getSimpleName())
                .with("error_message", t.getMessage())
                .with("retryable", isRetryableError(t))
                .error();

        saveFailureLog(user.getUsername(), t, "WRITE_PHASE");
    }

    @Override
    public void onSkipInProcess(User user, Throwable t) {
        LogContext.event(Event.BATCH_ITEM_FAILED)
                .with("username", user.getUsername())
                .with("phase", "PROCESS")
                .with("error_type", t.getClass().getSimpleName())
                .with("error_message", t.getMessage())
                .with("retryable", isRetryableError(t))
                .warn();

        saveFailureLog(user.getUsername(), t, "PROCESS_PHASE");
    }

    private void saveFailureLog(String targetId, Throwable t, String phase) {
        ErrorType errorType = extractErrorType(t);
        String errorMessage = String.format("[%s] %s", phase, t.getMessage());

        batchFailureLogService.saveFailureLog(JOB_NAME, targetId, errorType, errorMessage);
    }

    private ErrorType extractErrorType(Throwable t) {
        if (t instanceof GitHubApiNonRetryableException e) {
            return e.getErrorType();
        } else if (t instanceof GitHubApiRetryableException e) {
            return e.getErrorType();
        }
        return ErrorType.DEFAULT_ERROR;
    }

    private boolean isRetryableError(Throwable t) {
        return t instanceof GitHubApiRetryableException;
    }
}
