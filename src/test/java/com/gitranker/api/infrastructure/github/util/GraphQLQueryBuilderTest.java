package com.gitranker.api.infrastructure.github.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class GraphQLQueryBuilderTest {

    private final GraphQLQueryBuilder queryBuilder = new GraphQLQueryBuilder(ZoneId.of("Asia/Seoul"));

    @Test
    @DisplayName("merged PR query targets author merged pull requests")
    void buildsMergedPrBlock() {
        String query = queryBuilder.buildMergedPRBlock("alice");

        assertThat(query).contains("author:alice type:pr is:merged");
        assertThat(query).contains("issueCount");
    }

    @Test
    @DisplayName("user created-at query requests rate-limit and profile fields")
    void buildsUserCreatedAtQuery() {
        String query = queryBuilder.buildUserCreatedAtQuery("alice");

        assertThat(query).contains("user(login: \"alice\")");
        assertThat(query).contains("rateLimit");
        assertThat(query).contains("avatarUrl");
    }

    @Test
    @DisplayName("yearly contribution query uses join date for the first year")
    void buildsYearlyContributionQueryWithJoinDateBoundary() {
        String query = queryBuilder.buildYearlyContributionQuery(
                "alice",
                2020,
                LocalDateTime.of(2020, 5, 10, 15, 30)
        );

        assertThat(query).contains("year2020");
        assertThat(query).contains("from: \"2020-05-10T15:30:00+09:00\"");
        assertThat(query).contains("to: \"2020-12-31T23:59:59+09:00\"");
    }

    @Test
    @DisplayName("batch query includes merged PR block and full-year range for past years")
    void buildsBatchQueryForPastYear() {
        int pastYear = LocalDate.now(ZoneId.of("Asia/Seoul")).getYear() - 1;

        String query = queryBuilder.buildBatchQuery("alice", pastYear);

        assertThat(query).contains("mergedPRs: search(query: \"author:alice type:pr is:merged\"");
        assertThat(query).contains("year" + pastYear);
        assertThat(query).contains("from: \"" + pastYear + "-01-01T00:00:00+09:00\"");
        assertThat(query).contains("to: \"" + pastYear + "-12-31T23:59:59+09:00\"");
    }

    @Test
    @DisplayName("node lookup query targets the given node id")
    void buildsUserLookupByNodeIdQuery() {
        String query = queryBuilder.buildUserLookupByNodeIdQuery("node-123");

        assertThat(query).contains("node(id: \"node-123\")");
        assertThat(query).contains("email");
        assertThat(query).contains("avatarUrl");
    }
}
