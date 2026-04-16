package com.gitranker.api.batch.strategy;

import com.gitranker.api.domain.log.ActivityLog;
import com.gitranker.api.global.error.message.BatchMessages;
import com.gitranker.api.support.TestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ActivityUpdateContextTest {

    @Test
    @DisplayName("incremental context keeps baseline log and year")
    void createsIncrementalContext() {
        ActivityLog baselineLog = ActivityLog.baseline(
                TestFixtures.user(),
                TestFixtures.stats(10, 2, 3, 4, 5),
                LocalDate.of(2025, 12, 31)
        );

        ActivityUpdateContext context = ActivityUpdateContext.forIncremental(baselineLog, 2026);

        assertThat(context.baselineLog()).isSameAs(baselineLog);
        assertThat(context.currentYear()).isEqualTo(2026);
    }

    @Test
    @DisplayName("incremental context rejects missing baseline log")
    void rejectsMissingBaselineForIncrementalUpdate() {
        assertThatThrownBy(() -> ActivityUpdateContext.forIncremental(null, 2026))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(BatchMessages.BASELINE_LOG_REQUIRED_FOR_INCREMENTAL);
    }

    @Test
    @DisplayName("full context does not require baseline log")
    void createsFullContext() {
        ActivityUpdateContext context = ActivityUpdateContext.forFull(2026);

        assertThat(context.baselineLog()).isNull();
        assertThat(context.currentYear()).isEqualTo(2026);
    }
}
