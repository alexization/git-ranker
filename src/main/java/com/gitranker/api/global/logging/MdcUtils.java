package com.gitranker.api.global.logging;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.util.StringUtils;

import java.util.UUID;

public final class MdcUtils {
    private MdcUtils() {
        throw new AssertionError("유틸 클래스는 인스턴스화 할 수 없습니다.");
    }

    public static void setupHttpRequestContext(HttpServletRequest request) {
        String traceId = UUID.randomUUID().toString().substring(0, 8);

        MDC.put(MdcKey.TRACE_ID, traceId);
        MDC.put(MdcKey.REQUEST_URI, request.getRequestURI());
        MDC.put(MdcKey.CLIENT_IP, extractClientIp(request));
        MDC.put(MdcKey.REQUEST_URI, request.getRequestURI());
    }

    public static void setUserContext(String username, String nodeId) {
        setUsername(username);
        setNodeId(nodeId);
    }

    public static void setUsername(String username) {
        if (StringUtils.hasText(username)) {
            MDC.put(MdcKey.USERNAME, username);
        }
    }

    public static void setNodeId(String nodeId) {
        if (StringUtils.hasText(nodeId)) {
            MDC.put(MdcKey.NODE_ID, nodeId);
        }
    }

    public static void setLatency(long latencyMs) {
        MDC.put(MdcKey.LATENCY_MS, String.valueOf(latencyMs));
    }

    public static void setHttpStatus(int statusCode) {
        MDC.put(MdcKey.HTTP_STATUS, String.valueOf(statusCode));
    }

    public static void setupBatchJobContext(String jobName) {
        String traceId = UUID.randomUUID().toString().substring(0, 8);

        MDC.put(MdcKey.TRACE_ID, traceId);
        MDC.put(MdcKey.JOB_NAME, jobName);
        MDC.put(MdcKey.CLIENT_IP, "SYSTEM");
    }

    public static void setDBQueryTime(long queryTimeMs) {
        MDC.put(MdcKey.DB_QUERY_TIME_MS, String.valueOf(queryTimeMs));
    }

    public static void setGithubApiCallTime(long githubApiCallTimeMs) {
        MDC.put(MdcKey.GITHUB_API_CALL_TIME_MS, String.valueOf(githubApiCallTimeMs));
    }

    public static void setGithubApiCost(int cost) {
        MDC.put(MdcKey.GITHUB_API_COST, String.valueOf(cost));
    }

    public static void setError(String errorCode, String errorMessage) {
        if (StringUtils.hasText(errorCode)) {
            MDC.put(MdcKey.ERROR_CODE, errorCode);
        }

        if (StringUtils.hasText(errorMessage)) {
            MDC.put(MdcKey.ERROR_MESSAGE, errorMessage);
        }
    }

    public static void setException(Exception exception) {
        if (exception != null) {
            MDC.put(MdcKey.ERROR_MESSAGE, exception.getMessage());
        }
    }

    public static void remove(String key) {
        MDC.remove(key);
    }

    public static void clear() {
        MDC.clear();
    }

    private static String extractClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");

        if (StringUtils.hasText(ip)) {
            return ip.split(",")[0].trim();
        }

        ip = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(ip)) {
            return ip;
        }

        return request.getRemoteAddr();
    }
}
