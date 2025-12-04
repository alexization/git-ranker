package com.gitranker.api.infrastructure.github.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record GitHubSearchResonse<T>(
        @JsonProperty("total_count")
        int totalCount,

        @JsonProperty("incomplete_results")
        boolean incompleteResults,

        List<T> itmes
) {
}
