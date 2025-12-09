package com.gitranker.api.domain.badge;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/badges")
public class BadgeController {

    private final BadgeService badgeService;

    @GetMapping(value = "/{nodeId}", produces = "image/svg+xml")
    public ResponseEntity<String> getBadge(@PathVariable String nodeId) {
        String svgContent = badgeService.generateBadge(nodeId);

        return ResponseEntity.ok()
                .contentType(MediaType.valueOf("image/svg+xml"))
                .body(svgContent);
    }
}
