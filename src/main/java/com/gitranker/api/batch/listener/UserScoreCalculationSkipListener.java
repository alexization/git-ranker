package com.gitranker.api.batch.listener;

import com.gitranker.api.domain.failure.BatchFailureLogService;
import com.gitranker.api.domain.user.User;
import com.gitranker.api.global.error.ErrorType;
import com.gitranker.api.global.error.exception.GitHubApiNonRetryableException;
import com.gitranker.api.global.error.exception.GitHubApiRetryableException;
import com.gitranker.api.global.logging.EventType;
import com.gitranker.api.global.logging.LogCategory;
import com.gitranker.api.global.logging.MdcUtils;
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
        MdcUtils.setLogContext(LogCategory.BATCH, EventType.SKIP);
        MdcUtils.setError(t.getClass().getSimpleName(), t.getMessage());

        log.error("배치 읽기 단계 Skip - Reason: {}", t.getMessage());

        saveFailureLog("UNKNOWN_USER", t, "READ_PHASE");
    }

    @Override
    public void onSkipInWrite(User user, Throwable t) {
        MdcUtils.setLogContext(LogCategory.BATCH, EventType.SKIP);
        MdcUtils.setUserContext(user.getUsername(), user.getNodeId());
        MdcUtils.setError(t.getClass().getSimpleName(), t.getMessage());

        log.error("배치 쓰기 단계 Skip - 사용자: {}, Reason: {}", user.getUsername(), t.getMessage());

        saveFailureLog(user.getUsername(), t, "WRITE_PHASE");
    }

    @Override
    public void onSkipInProcess(User user, Throwable t) {
        MdcUtils.setLogContext(LogCategory.BATCH, EventType.SKIP);
        MdcUtils.setUserContext(user.getUsername(), user.getNodeId());
        MdcUtils.setError(t.getClass().getSimpleName(), t.getMessage());

        if (isRetryableError(t)) {
            log.warn("배치 처리 단계 Skip (재시도 소진) - 사용자: {}, Reason: {}", user.getUsername(), t.getMessage());
        } else {
            log.info("배치 처리 단계 Skip (재시도 불가) - 사용자: {}, Reason: {}", user.getUsername(), t.getMessage());
        }

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
