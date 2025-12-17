package com.gitranker.api.infrastructure.github.util;

import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Slf4j
public class GraphQLQueryBuilder {

    public static String buildAllActivitiesQuery(String username, LocalDateTime githubJoinDate) {
        int joinYear = githubJoinDate.getYear();
        int currentYear = LocalDate.now(ZoneId.of("UTC")).getYear();

        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("{\n");

        for (int year = joinYear; year <= currentYear; year++) {
            String fromDate = buildFromDate(year, joinYear, githubJoinDate);
            String toDate = buildToDate(year, currentYear);

            queryBuilder.append(buildYearContributionBlock(year, username, fromDate, toDate));
        }

        queryBuilder.append(buildMergedPRBlock(username));
        queryBuilder.append("}\n");

        return queryBuilder.toString();
    }

    private static String buildYearContributionBlock(int year, String username,
                                                     String fromDate, String toDate) {
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

    private static String buildMergedPRBlock(String username) {
        return String.format("""
                mergedPRs: search(query: "author:%s type:pr is:merged", type: ISSUE, first: 1) {
                  issueCount
                }
                """, username);
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
        return dateTime.toString().substring(0, 19) + "Z";
    }

    public static String buildUserCreatedAtQuery(String username) {
        return String.format("""
                {
                  user(login: "%s") {
                    id
                    createdAt
                    login
                    avatarUrl
                  }
                }
                """, username);
    }
}
