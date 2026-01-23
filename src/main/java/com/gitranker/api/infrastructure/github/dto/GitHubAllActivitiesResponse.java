package com.gitranker.api.infrastructure.github.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public record GitHubAllActivitiesResponse(
        @JsonProperty("data") Data data,
        @JsonProperty("errors") List<Object> errors
) {
    public static GitHubAllActivitiesResponse empty() {
        return new GitHubAllActivitiesResponse(new Data(), null);
    }

    public boolean hasErrors() {
        return errors != null && !errors.isEmpty();
    }

    public void merge(GitHubAllActivitiesResponse other) {
        if (other != null && other.data != null) {
            this.data.merge(other.data);
        }
    }

    public int getCommitCount() {
        if (data == null || data.getYearDataMap() == null) return 0;

        return data.getYearDataMap().values().stream()
                .mapToInt(yearData -> yearData.contributionsCollection().totalCommitContributions())
                .sum();
    }

    public int getIssueCount() {
        if (data == null || data.getYearDataMap() == null) return 0;

        return data.getYearDataMap().values().stream()
                .mapToInt(yearData -> yearData.contributionsCollection().totalIssueContributions())
                .sum();
    }

    public int getPRCount() {
        if (data == null || data.getYearDataMap() == null) return 0;

        return data.getYearDataMap().values().stream()
                .mapToInt(yearData -> yearData.contributionsCollection().totalPullRequestContributions())
                .sum();
    }

    public int getMergedPRCount() {
        if (data == null || data.getYearDataMap() == null) return 0;

        return data.mergedPRs() != null && data.mergedPRs().issueCount() != null
                ? data.mergedPRs().issueCount()
                : 0;
    }

    public int getReviewCount() {
        if (data == null || data.getYearDataMap() == null) return 0;

        return data.getYearDataMap().values().stream()
                .mapToInt(yearData -> yearData.contributionsCollection().totalPullRequestReviewContributions())
                .sum();
    }

    public record RateLimit(
            int limit,
            int cost,
            int remaining,
            LocalDateTime resetAt
    ) implements GitHubRateLimitInfo {
    }

    public static class Data {
        @Getter
        private final Map<String, YearData> yearDataMap = new HashMap<>();
        @JsonProperty("rateLimit")
        RateLimit rateLimit;
        @JsonProperty("mergedPRs")
        private Search mergedPRs;
        @JsonProperty("reviewedPRs")
        private Search reviewedPRs;

        @JsonAnySetter
        public void setYearData(String key, YearData value) {
            if (key.startsWith("year")) {
                yearDataMap.put(key, value);
            }
        }

        public void merge(Data other) {
            if (other.yearDataMap != null) {
                this.yearDataMap.putAll(other.yearDataMap);
            }

            if (other.mergedPRs != null) {
                this.mergedPRs = other.mergedPRs;
            }

            if (other.rateLimit != null) {
                int currentCost = (this.rateLimit != null) ? this.rateLimit.cost() : 0;
                this.rateLimit = new RateLimit(
                        other.rateLimit.limit(),
                        currentCost + other.rateLimit.cost(),
                        other.rateLimit.remaining(),
                        other.rateLimit.resetAt()
                );
            }
        }

        public Search mergedPRs() {
            return mergedPRs;
        }

        public RateLimit rateLimit() {
            return rateLimit;
        }
    }

    public record YearData(
            @JsonProperty("contributionsCollection")
            ContributionsCollection contributionsCollection
    ) {
    }

    public record ContributionsCollection(
            @JsonProperty("totalCommitContributions")
            int totalCommitContributions,

            @JsonProperty("totalIssueContributions")
            int totalIssueContributions,

            @JsonProperty("totalPullRequestContributions")
            int totalPullRequestContributions,

            @JsonProperty("totalPullRequestReviewContributions")
            int totalPullRequestReviewContributions
    ) {
    }

    public record Search(
            @JsonProperty("issueCount")
            Integer issueCount,

            @JsonProperty("nodes")
            List<Node> nodes
    ) {
    }

    public record Node(
            @JsonProperty("reviews")
            Reviews reviews
    ) {
    }

    public record Reviews(
            @JsonProperty("totalCount")
            int totalCount
    ) {
    }
}
