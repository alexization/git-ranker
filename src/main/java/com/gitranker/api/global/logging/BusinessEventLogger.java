package com.gitranker.api.global.logging;

import com.gitranker.api.domain.user.Tier;
import com.gitranker.api.domain.user.User;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class BusinessEventLogger {

    public void userRegistered(User user) {
        try {
            setDomainContext(EventType.USER_REGISTERED);
            setUserContext(user);

            log.info("신규 사용자 등록 완료 [username={}, tier={}, score={}]",
                    user.getUsername(),
                    user.getTier(),
                    user.getTotalScore());
        } finally {
            clearEventContext();
        }
    }

    public void userRefreshed(User user, int oldScore, int newScore) {
        try {
            setDomainContext(EventType.USER_REFRESHED);
            setUserContext(user);
            MDC.put(MdcKey.SCORE_FROM, String.valueOf(oldScore));
            MDC.put(MdcKey.SCORE_TO, String.valueOf(newScore));

            int diff = newScore - oldScore;
            String diffStr = diff >= 0 ? "+" + diff : String.valueOf(diff);

            log.info("사용자 데이터 갱신 완료 [username={}, score={} → {} ({})]",
                    user.getUsername(),
                    oldScore,
                    newScore,
                    diffStr);
        } finally {
            clearEventContext();
        }
    }

    public void tierChanged(User user, Tier fromTier, Tier toTier) {
        try {
            setDomainContext(EventType.TIER_CHANGED);
            setUserContext(user);
            MDC.put(MdcKey.TIER_FROM, fromTier.name());
            MDC.put(MdcKey.TIER_TO, toTier.name());

            String direction = toTier.ordinal() < fromTier.ordinal() ? "승급" : "강등";

            log.info("티어 {} [username={}, {} → {}]",
                    direction,
                    user.getUsername(),
                    fromTier,
                    toTier);
        } finally {
            clearEventContext();
        }
    }

    public void scoreUpdated(User user, int oldScore, int newScore) {
        try {
            setDomainContext(EventType.SCORE_UPDATED);
            setUserContext(user);
            MDC.put(MdcKey.SCORE_FROM, String.valueOf(oldScore));
            MDC.put(MdcKey.SCORE_TO, String.valueOf(newScore));

            log.debug("점수 업데이트 [username={}, score={} → {}]",
                    user.getUsername(),
                    oldScore,
                    newScore);
        } finally {
            clearEventContext();
        }
    }

    public void batchStarted(String jobName, int totalCount) {
        try {
            setBatchContext(EventType.BATCH_STARTED, jobName);
            MDC.put(MdcKey.BATCH_TOTAL_COUNT, String.valueOf(totalCount));

            log.info("배치 작업 시작 [job={}, totalCount={}]", jobName, totalCount);
        } finally {
            clearEventContext();
        }
    }

    public void batchProgress(String jobName, int processed, int total, int percentage) {
        try {
            setBatchContext(EventType.BATCH_PROGRESS, jobName);
            MDC.put(MdcKey.BATCH_PROCESSED_COUNT, String.valueOf(processed));
            MDC.put(MdcKey.BATCH_TOTAL_COUNT, String.valueOf(total));
            MDC.put(MdcKey.BATCH_PROGRESS_PERCENT, String.valueOf(percentage));

            log.info("배치 진행률 [job={}, progress={}% ({}/{})]",
                    jobName, percentage, processed, total);
        } finally {
            clearEventContext();
        }
    }

    public void batchCompleted(String jobName, long totalCount, long successCount,
                               long failCount, long skipCount, long durationMs) {
        try {
            setBatchContext(EventType.BATCH_COMPLETED, jobName);
            MDC.put(MdcKey.BATCH_TOTAL_COUNT, String.valueOf(totalCount));
            MDC.put(MdcKey.BATCH_SUCCESS_COUNT, String.valueOf(successCount));
            MDC.put(MdcKey.BATCH_FAIL_COUNT, String.valueOf(failCount));
            MDC.put(MdcKey.BATCH_SKIP_COUNT, String.valueOf(skipCount));
            MDC.put(MdcKey.BATCH_DURATION_MS, String.valueOf(durationMs));

            double successRate = totalCount > 0 ? (successCount * 100.0 / totalCount) : 0;
            String durationStr = formatDuration(durationMs);
            String successRateStr = String.format("%.1f", successRate);

            if (failCount > 0) {
                log.warn("배치 작업 완료 (일부 실패) [job={}, 성공={}, 실패={}, 스킵={}, 성공률={}%, 소요시간={}]",
                        jobName, successCount, failCount, skipCount, successRateStr, durationStr);
            } else {
                log.info("배치 작업 완료 [job={}, 처리={}, 성공률={}%, 소요시간={}]",
                        jobName, totalCount, successRateStr, durationStr);
            }
        } finally {
            clearEventContext();
        }
    }

    public void batchItemFailed(String jobName, String username, String errorCode, String errorMessage) {
        try {
            setBatchContext(EventType.BATCH_ITEM_FAILED, jobName);
            MDC.put(MdcKey.USERNAME, username);
            MdcUtils.setError(errorCode, errorMessage);

            log.warn("배치 아이템 처리 실패 [job={}, username={}, error={}]",
                    jobName, username, errorCode);
        } finally {
            clearEventContext();
        }
    }

    public void apiCalled(String apiName) {
        try {
            setExternalApiContext(EventType.API_CALLED);
            log.debug("외부 API 호출 [api={}]", apiName);
        } finally {
            clearEventContext();
        }
    }

    public void apiSucceeded(String apiName, long latencyMs, int cost, int remaining) {
        try {
            setExternalApiContext(EventType.API_SUCCEEDED);
            MDC.put(MdcKey.LATENCY_MS, String.valueOf(latencyMs));
            MDC.put(MdcKey.GITHUB_API_COST, String.valueOf(cost));
            MDC.put(MdcKey.GITHUB_API_REMAINING, String.valueOf(remaining));

            log.info("외부 API 호출 성공 [api={}, latency={}ms, cost={}, remaining={}]",
                    apiName, latencyMs, cost, remaining);
        } finally {
            clearEventContext();
        }
    }

    public void apiFailed(String apiName, String errorCode, String errorMessage) {
        try {
            setExternalApiContext(EventType.API_FAILED);
            MdcUtils.setError(errorCode, errorMessage);

            log.error("외부 API 호출 실패 [api={}, error={}, message={}]",
                    apiName, errorCode, truncate(errorMessage, 200));
        } finally {
            clearEventContext();
        }
    }

    public void apiRateThreshold(int remaining, String resetAt) {
        try {
            setExternalApiContext(EventType.API_RATE_THRESHOLD);
            MDC.put(MdcKey.GITHUB_API_REMAINING, String.valueOf(remaining));
            MDC.put(MdcKey.GITHUB_API_RESET_AT, resetAt);

            log.warn("GitHub API Rate Limit 임계치 도달 [remaining={}, resetAt={}]",
                    remaining, resetAt);
        } finally {
            clearEventContext();
        }
    }

    public void authSuccess(String username, String authMethod) {
        try {
            setDomainContext(EventType.AUTH_SUCCESS);
            MDC.put(MdcKey.USERNAME, username);
            MDC.put(MdcKey.AUTH_METHOD, authMethod);

            log.info("인증 성공 [username={}, method={}]", username, authMethod);
        } finally {
            clearEventContext();
        }
    }

    public void authFailed(String reason) {
        try {
            setDomainContext(EventType.AUTH_FAILED);
            MDC.put(MdcKey.AUTH_FAILURE_REASON, reason);

            log.warn("인증 실패 [reason={}]", reason);
        } finally {
            clearEventContext();
        }
    }

    public void tokenIssued(String username) {
        try {
            setDomainContext(EventType.TOKEN_ISSUED);
            MDC.put(MdcKey.USERNAME, username);

            log.info("토큰 발급 완료 [username={}]", username);
        } finally {
            clearEventContext();
        }
    }

    public void tokenRefreshed(String username) {
        try {
            setDomainContext(EventType.TOKEN_REFRESHED);
            MDC.put(MdcKey.USERNAME, username);

            log.debug("토큰 갱신 완료 [username={}]", username);
        } finally {
            clearEventContext();
        }
    }

    private void setDomainContext(EventType eventType) {
        MdcUtils.setLogContext(LogCategory.DOMAIN, eventType);
    }

    private void setBatchContext(EventType eventType, String jobName) {
        MdcUtils.setLogContext(LogCategory.BATCH, eventType);
        MDC.put(MdcKey.JOB_NAME, jobName);
    }

    private void setExternalApiContext(EventType eventType) {
        MdcUtils.setLogContext(LogCategory.EXTERNAL_API, eventType);
    }

    private void setUserContext(User user) {
        if (user != null) {
            MDC.put(MdcKey.USERNAME, user.getUsername());
            MDC.put(MdcKey.NODE_ID, user.getNodeId());
            MDC.put(MdcKey.TIER, user.getTier().name());
            MDC.put(MdcKey.SCORE, String.valueOf(user.getTotalScore()));
            if (user.getRankInfo() != null) {
                MDC.put(MdcKey.RANKING, String.valueOf(user.getRanking()));
            }
        }
    }

    private void clearEventContext() {
        MDC.remove(MdcKey.EVENT_TYPE);
        MDC.remove(MdcKey.TIER);
        MDC.remove(MdcKey.TIER_FROM);
        MDC.remove(MdcKey.TIER_TO);
        MDC.remove(MdcKey.SCORE);
        MDC.remove(MdcKey.SCORE_FROM);
        MDC.remove(MdcKey.SCORE_TO);
        MDC.remove(MdcKey.RANKING);
        MDC.remove(MdcKey.BATCH_TOTAL_COUNT);
        MDC.remove(MdcKey.BATCH_PROCESSED_COUNT);
        MDC.remove(MdcKey.BATCH_SUCCESS_COUNT);
        MDC.remove(MdcKey.BATCH_FAIL_COUNT);
        MDC.remove(MdcKey.BATCH_SKIP_COUNT);
        MDC.remove(MdcKey.BATCH_PROGRESS_PERCENT);
        MDC.remove(MdcKey.BATCH_DURATION_MS);
        MDC.remove(MdcKey.AUTH_METHOD);
        MDC.remove(MdcKey.AUTH_FAILURE_REASON);
    }

    private String formatDuration(long durationMs) {
        if (durationMs < 1000) {
            return durationMs + "ms";
        } else if (durationMs < 60_000) {
            return String.format("%.1fs", durationMs / 1000.0);
        } else {
            long minutes = durationMs / 60_000;
            long seconds = (durationMs % 60_000) / 1000;
            return String.format("%dm %ds", minutes, seconds);
        }
    }

    private String truncate(String message, int maxLength) {
        if (message == null) {
            return null;
        }
        return message.length() <= maxLength ? message : message.substring(0, maxLength) + "...";
    }
}
