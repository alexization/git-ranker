package com.gitranker.api.domain.user.dto;

import jakarta.validation.constraints.NotBlank;

public record RegisterUserRequest(@NotBlank(message = "{validation.user.username.not-blank}") String username) {
}
