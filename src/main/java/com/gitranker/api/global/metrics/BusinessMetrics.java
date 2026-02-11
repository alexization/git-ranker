package com.gitranker.api.global.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class BusinessMetrics {

    private final MeterRegistry registry;

    private final Counter registrationCounter;
    private final Counter loginCounter;
    private final Counter profileViewCounter;
    private final Counter badgeViewCounter;
    private final Counter refreshCounter;
    private final Counter deletionCounter;

    public BusinessMetrics(MeterRegistry registry) {
        this.registry = registry;

        registrationCounter = Counter.builder("user_registrations_total")
                .description("Total user registrations")
                .register(registry);

        loginCounter = Counter.builder("user_logins_total")
                .description("Total user logins")
                .register(registry);

        profileViewCounter = Counter.builder("profile_views_total")
                .description("Total profile views")
                .register(registry);

        badgeViewCounter = Counter.builder("badge_views_total")
                .description("Total badge views")
                .register(registry);

        refreshCounter = Counter.builder("user_refreshes_total")
                .description("Total user manual refreshes")
                .register(registry);

        deletionCounter = Counter.builder("user_deletions_total")
                .description("Total user deletions")
                .register(registry);
    }

    public void incrementRegistrations() {
        registrationCounter.increment();
    }

    public void incrementLogins() {
        loginCounter.increment();
    }

    public void incrementProfileViews() {
        profileViewCounter.increment();
    }

    public void incrementBadgeViews() {
        badgeViewCounter.increment();
    }

    public void incrementRefreshes() {
        refreshCounter.increment();
    }

    public void incrementDeletions() {
        deletionCounter.increment();
    }

    public void recordError(String errorCode) {
        Counter.builder("errors_total")
                .tag("error_code", errorCode)
                .register(registry)
                .increment();
    }
}
