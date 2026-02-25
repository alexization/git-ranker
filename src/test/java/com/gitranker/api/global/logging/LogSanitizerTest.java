package com.gitranker.api.global.logging;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LogSanitizerTest {

    @Test
    @DisplayName("username 필드는 구조화 로그에서 자동 마스킹한다")
    void should_maskUsernameField_when_structuredFieldIsUsername() {
        Object sanitized = LogSanitizer.sanitizeStructuredField("username", "tester");

        assertThat(sanitized).isEqualTo("te****");
    }

    @Test
    @DisplayName("target_username 필드도 자동 마스킹한다")
    void should_maskTargetUsernameField_when_structuredFieldIsTargetUsername() {
        Object sanitized = LogSanitizer.sanitizeStructuredField("target_username", "octocat");

        assertThat(sanitized).isEqualTo("oc*****");
    }

    @Test
    @DisplayName("username 외 필드는 원본 값을 유지한다")
    void should_keepOriginalValue_when_fieldIsNotUsername() {
        Object sanitized = LogSanitizer.sanitizeStructuredField("node_id", "MDQ6VXNlcjE=");

        assertThat(sanitized).isEqualTo("MDQ6VXNlcjE=");
    }

    @Test
    @DisplayName("username 해시는 고정 길이 식별자로 반환한다")
    void should_hashUsername_withDeterministicLength() {
        String hash = LogSanitizer.hashUsername("tester");

        assertThat(hash).hasSize(12);
        assertThat(hash).matches("[0-9a-f]{12}");
        assertThat(hash).isEqualTo(LogSanitizer.hashUsername("tester"));
    }

    @Test
    @DisplayName("길이가 짧은 username도 원문 없이 마스킹한다")
    void should_maskShortUsername_withoutExposingRawValue() {
        assertThat(LogSanitizer.maskUsername("a")).isEqualTo("**");
        assertThat(LogSanitizer.maskUsername("ab")).isEqualTo("**");
    }
}
