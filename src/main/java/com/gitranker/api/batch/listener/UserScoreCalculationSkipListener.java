package com.gitranker.api.batch.listener;

import com.gitranker.api.domain.failure.BatchFailureLogService;
import com.gitranker.api.domain.user.User;
import com.gitranker.api.global.exception.ErrorType;
import com.gitranker.api.global.exception.GitHubApiNonRetryableException;
import com.gitranker.api.global.exception.GitHubApiRetryableException;
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
        log.error("[Batch Skip] 읽기 단계 실패 - Reason: {}", t.getMessage());

        saveFailureLog("UNKNOWN_USER", t, "READ_PHASE");
    }

    @Override
    public void onSkipInWrite(User user, Throwable t) {
        log.error("[Batch Skip] 쓰기 단계 실패 - 사용자: {}, Reason: {}", user.getUsername(), t.getMessage());

        saveFailureLog(user.getUsername(), t, "WRITE_PHASE");
    }

    @Override
    public void onSkipInProcess(User user, Throwable t) {
        log.warn("[Batch Skip] 처리 단계 건너뜀 - 사용자: {}, Reason: {}", user.getUsername(), t.getMessage());

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
}
