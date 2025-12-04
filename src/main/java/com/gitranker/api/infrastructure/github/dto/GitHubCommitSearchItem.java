package com.gitranker.api.infrastructure.github.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.ZonedDateTime;

public record GitHubCommitSearchItem(
        String sha,
        Commit commit,

        @JsonProperty("committer")
        CommitAuthor committer
) {
    public ZonedDateTime getCommitDate() {
        return commit.committer.date;
    }

    public record Commit(
            String message,
            Author author,

            @JsonProperty("committer")
            Author committer
    ) {
        public record Author(
                String name,
                String email,
                ZonedDateTime date
        ) {

        }
    }

    public record CommitAuthor(
            String login,
            String id
    ) {

    }
}
