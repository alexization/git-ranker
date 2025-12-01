package com.gitranker.api.domain.user.dto;

import jakarta.validation.constraints.NotBlank;

public record RegisterUserReq(@NotBlank(message = "GitHub username은 필수값입니다.") String username) {
}
