package com.gitranker.api.global.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class TimeUtilsTest {

    private final TimeUtils timeUtils = new TimeUtils(ZoneId.of("Asia/Seoul"));

    @Test
    @DisplayName("UTC 시간을 애플리케이션 타임존으로 변환한다")
    void convertsUtcToApplicationZone() {
        ZonedDateTime converted = timeUtils.UTCtoAppZone(LocalDateTime.of(2025, 1, 1, 0, 30));

        assertThat(converted).isEqualTo(ZonedDateTime.of(2025, 1, 1, 9, 30, 0, 0, ZoneId.of("Asia/Seoul")));
    }

    @Test
    @DisplayName("로그 포맷은 ISO offset 형식으로 반환한다")
    void formatsForLog() {
        String formatted = timeUtils.formatForLog(LocalDateTime.of(2025, 1, 1, 0, 30));

        assertThat(formatted).isEqualTo("2025-01-01T09:30:00+09:00");
    }

    @Test
    @DisplayName("표시 포맷은 HH:mm 형식으로 반환하고 null은 빈 문자열을 반환한다")
    void formatsForDisplayAndHandlesNull() {
        assertThat(timeUtils.formatForDisplay(LocalDateTime.of(2025, 1, 1, 0, 30))).isEqualTo("09:30");
        assertThat(timeUtils.formatForDisplay(null)).isEmpty();
    }

    @Test
    @DisplayName("UTCtoAppZone과 로그 포맷은 null 입력을 그대로 비운다")
    void handlesNullInZoneConversionAndLogFormatting() {
        assertThat(timeUtils.UTCtoAppZone(null)).isNull();
        assertThat(timeUtils.formatForLog(null)).isNull();
    }
}
