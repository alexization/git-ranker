package com.gitranker.api.global.logging;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.UUID;

@Slf4j
public class LogContext {

    private static final String MDC_KEY_EVENT = "event";
    private static final String MDC_KEY_LOG_CATEGORY = "log_category";
    private static final String MDC_KEY_TRACE_ID = "trace_id";
    private static final String MDC_KEY_PHASE = "phase";
    private static final String MDC_KEY_OUTCOME = "outcome";

    private static final String OUTCOME_SUCCESS = "success";
    private static final String OUTCOME_WARNING = "warning";
    private static final String OUTCOME_FAILURE = "failure";

    private static final Set<String> REQUEST_SCOPED_KEYS = Set.of(
            "trace_id", "username", "client_ip", "user_agent", "request_method", "request_uri"
    );

    private final Event event;
    private final Map<String, Object> fields = new LinkedHashMap<>();

    private LogContext(Event event) {
        this.event = event;
    }

    public static LogContext event(Event event) {
        clearEventFields();

        return new LogContext(event);
    }

    private static void clearEventFields() {
        Map<String, String> ctx = MDC.getCopyOfContextMap();
        if (ctx == null) return;

        ctx.keySet().stream()
                .filter(key -> !REQUEST_SCOPED_KEYS.contains(key))
                .forEach(MDC::remove);
    }

    public static void initRequest(String traceId, String clientIp, String userAgent,
                                   String method, String uri) {
        MDC.put("trace_id", traceId);
        MDC.put("client_ip", clientIp);
        if (userAgent != null) {
            MDC.put("user_agent", userAgent);
        }
        MDC.put("request_method", method);
        MDC.put("request_uri", uri);
    }

    public static void setAuthContext(String username) {
        if (username != null) {
            MDC.put("username", LogSanitizer.maskUsername(username));
        }
    }

    public static String getTraceId() {
        return MDC.get(MDC_KEY_TRACE_ID);
    }

    public static void setTraceId(String traceId) {
        if (traceId != null && !traceId.isBlank()) {
            MDC.put(MDC_KEY_TRACE_ID, traceId);
        }
    }

    public static void clear() {
        MDC.clear();
    }

    public static String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public LogContext with(String key, Object value) {
        Object sanitizedValue = LogSanitizer.sanitizeStructuredField(key, value);
        if (sanitizedValue != null) {
            fields.put(key, sanitizedValue);
        }

        return this;
    }

    public void info() {
        logWithLevel(LogLevel.INFO, null);
    }

    public void warn() {
        logWithLevel(LogLevel.WARN, null);
    }

    public void warn(Throwable throwable) {
        logWithLevel(LogLevel.WARN, throwable);
    }

    public void error() {
        logWithLevel(LogLevel.ERROR, null);
    }

    public void error(Throwable throwable) {
        logWithLevel(LogLevel.ERROR, throwable);
    }

    public void debug() {
        logWithLevel(LogLevel.DEBUG, null);
    }

    private void logWithLevel(LogLevel level, Throwable throwable) {
        try {
            applyStructuredFieldContract(level);
            setupMdc();
            String message = buildMessage();
            writeLog(level, message, throwable);
        } finally {
            clearEventFields();
        }
    }

    private void applyStructuredFieldContract(LogLevel level) {
        if (getTraceId() == null || getTraceId().isBlank()) {
            setTraceId(generateTraceId());
        }

        fields.putIfAbsent(MDC_KEY_PHASE, event.getCategory().name().toLowerCase());
        fields.putIfAbsent(MDC_KEY_OUTCOME, defaultOutcome(level));
    }

    private String defaultOutcome(LogLevel level) {
        return switch (level) {
            case DEBUG, INFO -> OUTCOME_SUCCESS;
            case WARN -> OUTCOME_WARNING;
            case ERROR -> OUTCOME_FAILURE;
        };
    }

    private void setupMdc() {
        MDC.put(MDC_KEY_EVENT, event.name());
        MDC.put(MDC_KEY_LOG_CATEGORY, event.getCategory().name());

        fields.forEach((key, value) -> MDC.put(key, String.valueOf(value)));
    }

    private String buildMessage() {
        StringJoiner details = new StringJoiner(", ");

        fields.forEach((key, value) -> {
            if (shouldIncludeInMessage(key)) {
                details.add(key + "=" + value);
            }
        });

        if (details.length() > 0) {
            return event.getDescription() + " [" + details + "]";
        }
        return event.getDescription();
    }

    private boolean shouldIncludeInMessage(String key) {
        return key.equals("username") ||
               key.equals("target_username") ||
               key.equals("job_name") ||
               key.equals("error_code") ||
               key.equals("token_id") ||
               key.equals("status") ||
               key.equals(MDC_KEY_PHASE) ||
               key.equals(MDC_KEY_OUTCOME);
    }

    private void writeLog(LogLevel level, String message, Throwable throwable) {
        switch (level) {
            case DEBUG -> { if (throwable != null) log.debug(message, throwable); else log.debug(message); }
            case INFO -> { if (throwable != null) log.info(message, throwable); else log.info(message); }
            case WARN -> { if (throwable != null) log.warn(message, throwable); else log.warn(message); }
            case ERROR -> { if (throwable != null) log.error(message, throwable); else log.error(message); }
        }
    }

    private enum LogLevel {
        DEBUG, INFO, WARN, ERROR
    }
}
