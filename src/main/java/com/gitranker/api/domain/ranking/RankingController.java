package com.gitranker.api.domain.ranking;

import com.gitranker.api.domain.ranking.dto.RankingList;
import com.gitranker.api.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/ranking")
public class RankingController {
    private final RankingService rankingService;

    @GetMapping
    public ApiResponse<RankingList> getRankings(
            @RequestParam(defaultValue = "0") int page
    ) {
        RankingList response = rankingService.getRankingList(page);

        return ApiResponse.success(response);
    }
}
