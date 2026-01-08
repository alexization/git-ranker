package com.gitranker.api.domain.badge;

import com.gitranker.api.domain.user.Tier;
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

    @GetMapping(value = "/{tier}/badge", produces = "image/svg+xml")
    public ResponseEntity<String> getBadgeByTier(@PathVariable Tier tier) {
        String svgContent = badgeService.generateBadgeByTier(tier);

        return ResponseEntity.ok()
                .contentType(MediaType.valueOf("image/svg+xml"))
                .body(svgContent);
    }

    @GetMapping(value = "/preview/all", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> getAllTierBadgesPreview() {
        StringBuilder html = new StringBuilder();
        html.append("""
                    <!DOCTYPE html>
                    <html>
                    <head>
                        <style>
                            body {
                                display: flex;
                                flex-direction: row;
                                flex-wrap: wrap;
                                justify-content: center;
                                align-items: center;
                                padding: 40px;
                                gap: 20px;
                            }
                            h1 { color: #fff; margin-bottom: 20px; }
                            .badge-container {
                                transition: transform 0.2s;
                            }
                            .badge-container:hover { transform: scale(1.02); }
                        </style>
                    </head>
                    <body>
                """);

        for (Tier tier : Tier.values()) {
            html.append(String.format(
                    "<div class='badge-container'><img src='/api/v1/badges/%s/badge' alt='%s Tier Badge' /></div>",
                    tier.name(), tier.name()
            ));
        }

        html.append("</body></html>");

        return ResponseEntity.ok(html.toString());
    }
}