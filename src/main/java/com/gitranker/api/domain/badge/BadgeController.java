package com.gitranker.api.domain.badge;

import com.gitranker.api.domain.user.Tier;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
@RestController
@Tag(name = "Badges")
@RequestMapping("/api/v1/badges")
public class BadgeController {

    private final BadgeService badgeService;

    @GetMapping(value = "/{nodeId}", produces = "image/svg+xml")
    @Operation(summary = "Render a badge for a GitHub node id", description = "Returns an SVG badge for a user's current Git Ranker profile.")
    public ResponseEntity<String> getBadge(@PathVariable String nodeId) {
        String svgContent = badgeService.generateBadge(nodeId);

        CacheControl cacheControl = CacheControl.maxAge(1, TimeUnit.HOURS)
                .mustRevalidate();

        return ResponseEntity.ok()
                .contentType(MediaType.valueOf("image/svg+xml"))
                .cacheControl(cacheControl)
                .header("Pragma", "no-cache")
                .header("Expires", "0")
                .body(svgContent);
    }

    @GetMapping(value = "/{tier}/badge", produces = "image/svg+xml")
    @Operation(summary = "Render a tier badge", description = "Returns an SVG badge template for the requested tier.")
    public ResponseEntity<String> getBadgeByTier(@PathVariable Tier tier) {
        String svgContent = badgeService.generateBadgeByTier(tier);

        return ResponseEntity.ok()
                .contentType(MediaType.valueOf("image/svg+xml"))
                .body(svgContent);
    }
}
