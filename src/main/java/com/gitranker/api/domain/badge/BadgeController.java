package com.gitranker.api.domain.badge;

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
@RequestMapping("/api/v1/badges")
public class BadgeController {

    private final BadgeService badgeService;

    @GetMapping(value = "/{nodeId}", produces = "image/svg+xml")
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
}
