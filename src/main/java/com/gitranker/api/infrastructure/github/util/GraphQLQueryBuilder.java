package com.gitranker.api.infrastructure.github.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
@RequiredArgsConstructor
public class GraphQLQueryBuilder {

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");
    private final ZoneId appZoneId;

    public String buildMergedPRBlock(String username) {
        return String.format("""
                {
                    mergedPRs: search(query: "author:%s type:pr is:merged", type: ISSUE, first: 1) {
                      issueCount
                    }
                }
                """, username);
    }

    public String buildUserCreatedAtQuery(String username) {
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

    private String buildYearContributionBlock(
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

    private String toISOString(ZonedDateTime zonedDateTime) {
        return zonedDateTime.format(ISO_FORMATTER);
    }

    public String buildYearlyContributionQuery(String username, int year, LocalDateTime githubJoinDate) {
        int joinYear = githubJoinDate.getYear();
        int currentYear = LocalDate.now(appZoneId).getYear();

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

    public String buildBatchQuery(String username, int year) {
        int currentYear = LocalDate.now(appZoneId).getYear();

        String fromDate = buildFromDate(year, -1, null);
        String toDate = buildToDate(year, currentYear);

        return String.format("""
                        {
                            rateLimit {
                                limit
                                remaining
                                resetAt
                                cost
                            }
                            mergedPRs: search(query: "author:%s type:pr is:merged", type: ISSUE, first: 1) {
                              issueCount
                            }
                            %s
                        }
                        """,
                username,
                buildYearContributionBlock(year, username, fromDate, toDate)
        );
    }

    private String buildFromDate(int year, int joinYear, LocalDateTime githubJoinDate) {
        if (year == joinYear && githubJoinDate != null) {
            return toISOString(githubJoinDate.atZone(appZoneId));
        }

        ZonedDateTime zdt = ZonedDateTime.of(year, 1, 1, 0, 0, 0, 0, appZoneId);
        return toISOString(zdt);
    }

    private String buildToDate(int year, int currentYear) {
        if (year == currentYear) {
            return toISOString(ZonedDateTime.now(appZoneId));
        }

        ZonedDateTime zdt = ZonedDateTime.of(year, 12, 31, 23, 59, 59, 0, appZoneId);
        return toISOString(zdt);
    }
}
