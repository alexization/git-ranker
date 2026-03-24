package com.gitranker.api.domain.ranking;

import com.gitranker.api.domain.ranking.dto.RankingList;
import com.gitranker.api.domain.user.Tier;
import com.gitranker.api.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RequiredArgsConstructor
@RestController
@Tag(name = "Ranking")
@RequestMapping("/api/v1/ranking")
public class RankingController {

    private final RankingService rankingService;

    @GetMapping
    @Operation(summary = "List ranking entries", description = "Returns paginated ranking results with an optional tier filter.")
    public ApiResponse<RankingList> getRankings(
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "{validation.ranking.page.min}") int page,
            @RequestParam(required = false) Tier tier
    ) {
        RankingList response = rankingService.getRankingList(page, tier);

        return ApiResponse.success(response);
    }
}
