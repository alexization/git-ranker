package com.gitranker.api.batch.listener;

import com.gitranker.api.domain.user.User;
import com.gitranker.api.global.exception.GitHubApiNonRetryableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.SkipListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class UserScoreCalculationSkipListener implements SkipListener<User, User> {

    @Override
    public void onSkipInRead(Throwable t) {
        log.error("[Batch Skip] 읽기 단계 실패 - Reason: {}", t.getMessage());
    }

    @Override
    public void onSkipInWrite(User user, Throwable t) {
        log.error("[Batch Skip] 쓰기 단계 실패 - 사용자: {}, Reason: {}", user.getUsername(), t.getMessage());
    }

    @Override
    public void onSkipInProcess(User user, Throwable t) {
        if (t instanceof GitHubApiNonRetryableException e) {
            log.warn("[Batch Skip] 처리 단계 건너뜀 - 사용자: {}, Code: {}, Reason: {}",
                    user.getUsername(), e.getErrorType(), e.getMessage());
        } else {
            log.error("[Batch Skip] 처리 단계 실패 - 사용자: {}, Reason: {}",
                    user.getUsername(), t.getMessage());
        }
    }
}
