package com.gitranker.api.domain.user.dto;

import com.gitranker.api.domain.log.ActivityLog;
import com.gitranker.api.domain.user.Tier;
import com.gitranker.api.domain.user.User;

import java.time.LocalDateTime;

public record RegisterUserResponse(
        Long userId,
        String nodeId,
        String username,
        String profileImage,
        LocalDateTime updatedAt,
        LocalDateTime lastFullScanAt,
        int totalScore,
        int ranking,
        Tier tier,
        double percentile,
        int commitCount,
        int issueCount,
        int prCount,
        int mergedPrCount,
        int reviewCount,
        int diffCommitCount,
        int diffIssueCount,
        int diffPrCount,
        int diffMergedPrCount,
        int diffReviewCount,
        boolean isNewUser
) {
    public static RegisterUserResponse of(User user, ActivityLog latestLog, boolean isNewUser) {
        return new RegisterUserResponse(
                user.getId(),
                user.getNodeId(),
                user.getUsername(),
                user.getProfileImage(),
                user.getUpdatedAt(),
                user.getLastFullScanAt(),
                user.getTotalScore(),
                user.getRanking(),
                user.getTier(),
                user.getPercentile(),
                latestLog.getCommitCount(),
                latestLog.getIssueCount(),
                latestLog.getPrCount(),
                latestLog.getMergedPrCount(),
                latestLog.getReviewCount(),
                latestLog.getDiffCommitCount(),
                latestLog.getDiffIssueCount(),
                latestLog.getDiffPrCount(),
                latestLog.getDiffMergedPrCount(),
                latestLog.getDiffReviewCount(),
                isNewUser
        );
    }

    public static RegisterUserResponse register(User user, ActivityLog latestLog, boolean isNewUser) {
        return new RegisterUserResponse(
                user.getId(),
                user.getNodeId(),
                user.getUsername(),
                user.getProfileImage(),
                user.getUpdatedAt(),
                user.getLastFullScanAt(),
                user.getTotalScore(),
                user.getRanking(),
                user.getTier(),
                user.getPercentile(),
                latestLog.getCommitCount(),
                latestLog.getIssueCount(),
                latestLog.getPrCount(),
                latestLog.getMergedPrCount(),
                latestLog.getReviewCount(),
                0, 0, 0, 0, 0,
                isNewUser
        );
    }
}
