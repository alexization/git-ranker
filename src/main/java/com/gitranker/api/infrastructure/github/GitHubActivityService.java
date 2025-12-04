package com.gitranker.api.infrastructure.github;

import com.gitranker.api.infrastructure.github.dto.GitHubActivitySummary;
import com.gitranker.api.infrastructure.github.dto.GitHubCommitSearchItem;
import com.gitranker.api.infrastructure.github.dto.GitHubIssueSearchItem;
import com.gitranker.api.infrastructure.github.dto.GitHubSearchResonse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class GitHubActivityService {
    private final GitHubApiClient gitHubApiClient;

    public GitHubActivitySummary collectAllActivities(String username) {
        log.info("전체 GitHub 활동 수집 시작 - 사용자: {}", username);

        int commitCount = collectCommitCount(username);
        int prOpenCount = collectPrOpenCount(username);
        int prMergedCount = collectPrMergedCount(username);
        int issueCount = collectIssueCount(username);
        int reviewCount = collectReviewCount(username);

        GitHubActivitySummary summary = new GitHubActivitySummary(
                commitCount,
                prOpenCount,
                prMergedCount,
                issueCount,
                reviewCount
        );

        log.info("전체 수집 완료 - 사용자 : {}, 총점 : {}, 상세 : {}", username, summary.calculateTotalScore(), summary);

        return summary;
    }

    private int collectCommitCount(String username) {
        try {
            String query = String.format("author:%s", username);

            GitHubSearchResonse<GitHubCommitSearchItem> response =
                    gitHubApiClient.searchCommits(query, 1, 1);

            return response.totalCount();
        } catch (Exception e) {
            log.error("Commit 수집 실패 : {}", e.getMessage());
            return 0;
        }
    }

    private int collectPrOpenCount(String username) {
        try {
            String query = String.format("author:%s type:pr", username);

            GitHubSearchResonse<GitHubIssueSearchItem> response =
                    gitHubApiClient.searchIssues(query, 1, 1);

            return response.totalCount();
        } catch (Exception e) {
            log.error("PR Open 수집 실패 : {}", e.getMessage());
            return 0;
        }
    }

    private int collectPrMergedCount(String username) {
        try {
            String query = String.format("author:%s type:pr is:merged", username);

            GitHubSearchResonse<GitHubIssueSearchItem> response =
                    gitHubApiClient.searchIssues(query, 1, 1);

            return response.totalCount();
        } catch (Exception e) {
            log.error("PR Merged 수집 실패 : {}", e.getMessage());
            return 0;
        }
    }

    private int collectIssueCount(String username) {
        try {
            String query = String.format("author:%s type:issue", username);

            GitHubSearchResonse<GitHubIssueSearchItem> response =
                    gitHubApiClient.searchIssues(query, 1, 1);

            return response.totalCount();
        } catch (Exception e) {
            log.error("Issue 수집 실패 : {}", e.getMessage());
            return 0;
        }
    }

    private int collectReviewCount(String username) {
        try {
            String query = String.format("reviewed-by:%s type:pr", username);

            GitHubSearchResonse<GitHubIssueSearchItem> response =
                    gitHubApiClient.searchIssues(query, 1, 1);

            return response.totalCount();
        } catch (Exception e) {
            log.error("Code Review 수집 실패 : {}", e.getMessage());
            return 0;
        }
    }
}
