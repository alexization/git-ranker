package com.gitranker.api.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.ZoneId;
import java.util.TimeZone;

@Configuration
public class TimeZoneConfig {

    @Value("${app.timezone}")
    private String timezone;

    @Bean
    public ZoneId getZoneId() {
        return ZoneId.of(timezone);
    }

    @Bean
    public TimeZone defaultTimeZone() {
        TimeZone defaultTimeZone = TimeZone.getTimeZone(timezone);
        TimeZone.setDefault(defaultTimeZone);

        return defaultTimeZone;
    }
}
