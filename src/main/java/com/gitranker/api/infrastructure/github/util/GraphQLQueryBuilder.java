package com.gitranker.api.infrastructure.github.util;

import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Slf4j
public class GraphQLQueryBuilder {

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

    public static String buildYearlyContributionQuery(String username, int year, LocalDateTime githubJoinDate) {
        int joinYear = githubJoinDate.getYear();
        int currentYear = LocalDate.now(ZoneId.of("UTC")).getYear();

        String fromDate = buildFromDate(year, joinYear, githubJoinDate);
        String toDate = buildToDate(year, currentYear);

        return String.format("""
                {
                    rateLimit {
                        limit
                        remaining
                        resetAt
                        cost
                    }
                    %s
                }
                """, buildYearContributionBlock(year, username, fromDate, toDate));
    }

    public static String buildBatchQuery(String username, int year) {
        int currentYear = LocalDate.now(ZoneId.of("UTC")).getYear();

        String fromDate = String.format("%d-01-01T00:00:00Z", year);
        String toDate = buildToDate(year, currentYear);

        return String.format("""
                {
                    rateLimit {
                        limit
                        remaining
                        resetAt
                        cost
                    }
                    %s
                    %s
                }
                """,
                buildYearContributionBlock(year, username, fromDate, toDate),
                buildMergedPRBlock(username)
        );
    }

    public static String buildMergedPRBlock(String username) {
        return String.format("""
                mergedPRs: search(query: "author:%s type:pr is:merged", type: ISSUE, first: 1) {
                  issueCount
                }
                """, username);
    }

    public static String buildUserCreatedAtQuery(String username) {
        return String.format("""
                {
                  rateLimit {
                    limit
                    remaining
                    resetAt
                    cost
                  }
                  user(login: "%s") {
                    id
                    createdAt
                    login
                    avatarUrl
                  }
                }
                """, username);
    }

    private static String buildYearContributionBlock(
            int year, String username, String fromDate, String toDate
    ) {
        return String.format("""
                year%d: user(login: "%s") {
                  contributionsCollection(from: "%s", to: "%s") {
                    totalCommitContributions
                    totalIssueContributions
                    totalPullRequestContributions
                    totalPullRequestReviewContributions
                  }
                }
                """, year, username, fromDate, toDate);
    }

    private static String buildFromDate(int year, int joinYear, LocalDateTime githubJoinDate) {
        if (year == joinYear) {
            return toISOString(githubJoinDate);
        }
        return String.format("%d-01-01T00:00:00Z", year);
    }

    private static String buildToDate(int year, int currentYear) {
        if (year == currentYear) {
            return toISOString(LocalDateTime.now(ZoneId.of("UTC")));
        }
        return String.format("%d-12-31T23:59:59Z", year);
    }

    private static String toISOString(LocalDateTime dateTime) {
        return dateTime.format(ISO_FORMATTER);
    }
}
