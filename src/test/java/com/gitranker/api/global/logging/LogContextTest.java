package com.gitranker.api.global.logging;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class LogContextTest {

    @AfterEach
    void tearDown() {
        LogContext.clear();
    }

    @Test
    @DisplayName("요청 컨텍스트가 있으면 필수 구조화 필드를 포함해 로그를 남긴다")
    void should_includeRequiredStructuredFields_when_requestContextExists() {
        ListAppender<ILoggingEvent> appender = attachAppender();
        try {
            LogContext.initRequest(
                    "trace-1234",
                    "127.0.0.1",
                    "JUnit",
                    "GET",
                    "/api/v1/users/tester"
            );

            LogContext.event(Event.PROFILE_VIEWED)
                    .with("username", "tester")
                    .info();

            assertThat(appender.list).hasSize(1);
            Map<String, String> mdc = appender.list.getFirst().getMDCPropertyMap();

            assertThat(mdc)
                    .containsEntry("trace_id", "trace-1234")
                    .containsEntry("event", "PROFILE_VIEWED")
                    .containsEntry("log_category", "USER")
                    .containsEntry("phase", "user")
                    .containsEntry("outcome", "success")
                    .containsEntry("username", "te****")
                    .containsEntry("request_method", "GET")
                    .containsEntry("request_uri", "/api/v1/users/tester");
        } finally {
            detachAppender(appender);
        }
    }

    @Test
    @DisplayName("trace_id가 없으면 자동 생성하고 error 로그는 failure outcome을 기록한다")
    void should_generateTraceIdAndFailureOutcome_when_errorWithoutTraceId() {
        ListAppender<ILoggingEvent> appender = attachAppender();
        try {
            LogContext.event(Event.ERROR_HANDLED)
                    .with("error_code", "DEFAULT_ERROR")
                    .error();

            assertThat(appender.list).hasSize(1);
            Map<String, String> mdc = appender.list.getFirst().getMDCPropertyMap();

            assertThat(mdc)
                    .containsEntry("event", "ERROR_HANDLED")
                    .containsEntry("log_category", "ERROR")
                    .containsEntry("phase", "error")
                    .containsEntry("outcome", "failure");
            assertThat(mdc.get("trace_id")).isNotBlank();
        } finally {
            detachAppender(appender);
        }
    }

    @Test
    @DisplayName("phase와 outcome을 명시하면 기본값을 덮어쓰지 않는다")
    void should_keepExplicitPhaseAndOutcome_when_overridden() {
        ListAppender<ILoggingEvent> appender = attachAppender();
        try {
            LogContext.setTraceId("trace-batch");

            LogContext.event(Event.BATCH_ITEM_FAILED)
                    .with("phase", "PROCESS")
                    .with("outcome", "failure")
                    .warn();

            assertThat(appender.list).hasSize(1);
            Map<String, String> mdc = appender.list.getFirst().getMDCPropertyMap();

            assertThat(mdc)
                    .containsEntry("phase", "PROCESS")
                    .containsEntry("outcome", "failure");
        } finally {
            detachAppender(appender);
        }
    }

    @Test
    @DisplayName("인증 컨텍스트 username은 마스킹되어 기록한다")
    void should_maskUsername_when_setAuthContextIsUsed() {
        ListAppender<ILoggingEvent> appender = attachAppender();
        try {
            LogContext.initRequest("trace-auth", "127.0.0.1", "JUnit", "GET", "/api/v1/auth/me");
            LogContext.setAuthContext("octocat");

            LogContext.event(Event.USER_LOGIN).info();

            assertThat(appender.list).hasSize(1);
            Map<String, String> mdc = appender.list.getFirst().getMDCPropertyMap();
            assertThat(mdc).containsEntry("username", "oc*****");
        } finally {
            detachAppender(appender);
        }
    }

    private ListAppender<ILoggingEvent> attachAppender() {
        Logger logger = (Logger) LoggerFactory.getLogger(LogContext.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        return appender;
    }

    private void detachAppender(ListAppender<ILoggingEvent> appender) {
        Logger logger = (Logger) LoggerFactory.getLogger(LogContext.class);
        logger.detachAppender(appender);
        appender.stop();
        MDC.clear();
    }
}
