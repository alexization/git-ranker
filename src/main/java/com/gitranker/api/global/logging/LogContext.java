package com.gitranker.api.global.logging;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringJoiner;

@Slf4j
public class LogContext {

    private static final String MDC_KEY_EVENT = "event";
    private static final String MDC_KEY_TRACE_ID = "trace_id";

    private final Event event;
    private final Map<String, Object> fields = new LinkedHashMap<>();

    private LogContext(Event event) {
        this.event = event;
    }

    public static LogContext event(Event event) {
        clearEventFields();

        return new LogContext(event);
    }

    public LogContext with(String key, Object value) {
        if (value != null) {
            fields.put(key, value);
        }

        return this;
    }

    public void info() {
        logWithLevel(LogLevel.INFO);
    }

    public void warn() {
        logWithLevel(LogLevel.WARN);
    }

    public void error() {
        logWithLevel(LogLevel.ERROR);
    }

    public void debug() {
        logWithLevel(LogLevel.DEBUG);
    }

    private void logWithLevel(LogLevel level) {
        try {
            setupMdc();
            String message = buildMessage();
            writeLog(level, message);
        } finally {
            clearEventFields();
        }
    }

    private void setupMdc() {
        MDC.put(MDC_KEY_EVENT, event.name());

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
               key.equals("method") ||
               key.equals("uri") ||
               key.equals("status");
    }

    private void writeLog(LogLevel level, String message) {
        switch (level) {
            case DEBUG -> log.debug(message);
            case INFO -> log.info(message);
            case WARN -> log.warn(message);
            case ERROR -> log.error(message);
        }
    }

    private static void clearEventFields() {
        String traceId = MDC.get(MDC_KEY_TRACE_ID);

        MDC.clear();

        if (traceId != null) {
            MDC.put(MDC_KEY_TRACE_ID, traceId);
        }
    }

    public static void setTraceId(String traceId) {
        MDC.put(MDC_KEY_TRACE_ID, traceId);
    }

    public static String getTraceId() {
        return MDC.get(MDC_KEY_TRACE_ID);
    }

    public static void clear() {
        MDC.clear();
    }

    public static String generateTraceId() {
        return java.util.UUID.randomUUID().toString().substring(0, 8);
    }

    private enum LogLevel {
        DEBUG, INFO, WARN, ERROR
    }
}