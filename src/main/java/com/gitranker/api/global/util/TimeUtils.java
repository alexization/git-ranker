package com.gitranker.api.global.util;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@Component
@RequiredArgsConstructor
public class TimeUtils {

    private final ZoneId appZoneId;

    private static final DateTimeFormatter LOG_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    private static final DateTimeFormatter DISPLAY_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    public ZonedDateTime UTCtoAppZone(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }

        return dateTime.atZone(ZoneId.of("UTC"))
                .withZoneSameInstant(appZoneId);
    }

    public String formatForLog(LocalDateTime dateTime) {
        ZonedDateTime zdt = UTCtoAppZone(dateTime);

        return zdt != null ? zdt.format(LOG_FORMATTER) : null;
    }

    public String formatForDisplay(LocalDateTime dateTime) {
        ZonedDateTime zdt = UTCtoAppZone(dateTime);

        return zdt != null ? zdt.format(DISPLAY_FORMATTER) : "";
    }
}
