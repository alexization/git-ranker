package com.gitranker.api.global.logging;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class LoggingFilterTest {

    @AfterEach
    void tearDown() {
        LogContext.clear();
    }

    @Test
    @DisplayName("HTTP 2xx 응답은 outcome=success 로 기록한다")
    void should_logSuccessOutcome_for2xx() throws ServletException, IOException {
        ListAppender<ILoggingEvent> appender = attachAppender();
        try {
            TestableLoggingFilter filter = new TestableLoggingFilter(1_000L, 1_100L);
            executeFilter(filter, 200);

            Map<String, String> mdc = appender.list.getFirst().getMDCPropertyMap();
            assertThat(mdc).containsEntry("outcome", "success");
        } finally {
            detachAppender(appender);
        }
    }

    @Test
    @DisplayName("HTTP 4xx 응답은 outcome=failure 로 기록한다")
    void should_logFailureOutcome_for4xx() throws ServletException, IOException {
        ListAppender<ILoggingEvent> appender = attachAppender();
        try {
            TestableLoggingFilter filter = new TestableLoggingFilter(2_000L, 2_200L);
            executeFilter(filter, 404);

            Map<String, String> mdc = appender.list.getFirst().getMDCPropertyMap();
            assertThat(mdc).containsEntry("outcome", "failure");
        } finally {
            detachAppender(appender);
        }
    }

    @Test
    @DisplayName("HTTP 5xx 응답은 outcome=failure 로 기록한다")
    void should_logFailureOutcome_for5xx() throws ServletException, IOException {
        ListAppender<ILoggingEvent> appender = attachAppender();
        try {
            TestableLoggingFilter filter = new TestableLoggingFilter(3_000L, 3_150L);
            executeFilter(filter, 503);

            Map<String, String> mdc = appender.list.getFirst().getMDCPropertyMap();
            assertThat(mdc).containsEntry("outcome", "failure");
        } finally {
            detachAppender(appender);
        }
    }

    @Test
    @DisplayName("지연이 임계치를 넘는 2xx 응답은 outcome=warning 으로 기록한다")
    void should_logWarningOutcome_forSlowNonErrorResponse() throws ServletException, IOException {
        ListAppender<ILoggingEvent> appender = attachAppender();
        try {
            TestableLoggingFilter filter = new TestableLoggingFilter(5_000L, 15_100L);
            executeFilter(filter, 204);

            Map<String, String> mdc = appender.list.getFirst().getMDCPropertyMap();
            assertThat(mdc).containsEntry("outcome", "warning");
        } finally {
            detachAppender(appender);
        }
    }

    private void executeFilter(LoggingFilter filter, int status) throws IOException, ServletException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/test");
        request.addHeader("User-Agent", "JUnit");
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain chain = (req, res) -> ((MockHttpServletResponse) res).setStatus(status);
        filter.doFilter(request, response, chain);
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
    }

    private static final class TestableLoggingFilter extends LoggingFilter {
        private final long[] times;
        private int index;

        private TestableLoggingFilter(long... times) {
            this.times = times;
        }

        @Override
        protected long currentTimeMillis() {
            return times[index++];
        }
    }
}
