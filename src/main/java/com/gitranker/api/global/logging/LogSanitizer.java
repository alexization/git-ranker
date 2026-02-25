package com.gitranker.api.global.logging;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Set;

public final class LogSanitizer {

    private static final Set<String> USERNAME_KEYS = Set.of("username", "target_username");

    private LogSanitizer() {
    }

    public static Object sanitizeStructuredField(String key, Object value) {
        if (value == null || !USERNAME_KEYS.contains(key)) {
            return value;
        }
        return maskUsername(String.valueOf(value));
    }

    public static String maskUsername(String username) {
        if (username == null || username.isBlank()) {
            return username;
        }

        int visibleLength = Math.min(2, username.length());
        int maskedLength = Math.max(2, username.length() - visibleLength);

        return username.substring(0, visibleLength) + "*".repeat(maskedLength);
    }

    public static String hashUsername(String username) {
        if (username == null || username.isBlank()) {
            return username;
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(username.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash).substring(0, 12);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
