package com.gitranker.api.domain.ranking.dto;

import com.gitranker.api.domain.user.Tier;
import com.gitranker.api.domain.user.User;
import org.springframework.data.domain.Page;

import java.util.List;

public record RankingList(
        List<UserInfo> rankings,
        PageInfo pageInfo
) {
    public static RankingList from(Page<UserInfo> page) {
        return new RankingList(
                page.getContent(),
                new PageInfo(
                        page.getNumber(),
                        page.getSize(),
                        page.getTotalElements(),
                        page.getTotalPages(),
                        page.isFirst(),
                        page.isLast()
                )
        );
    }

    public record UserInfo(
            String username,
            String profileImage,
            int ranking,
            int totalScore,
            Tier tier,
            double percentile
    ) {
        public static UserInfo from(User user) {
            return new UserInfo(
                    user.getUsername(),
                    user.getProfileImage(),
                    user.getRanking(),
                    user.getTotalScore(),
                    user.getTier(),
                    user.getPercentile()
            );
        }
    }

    public record PageInfo(
            int currentPage,
            int pageSize,
            long totalElements,
            int totalPages,
            boolean isFirst,
            boolean isLast
    ){}
}
