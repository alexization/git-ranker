package com.gitranker.api.infrastructure.github.dto;

import java.time.LocalDateTime;

public interface GitHubRateLimitInfo {

    int limit();

    int cost();

    int remaining();

    LocalDateTime resetAt();
}
