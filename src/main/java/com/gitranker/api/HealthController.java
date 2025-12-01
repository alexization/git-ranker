package com.gitranker.api;

import com.gitranker.api.global.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

    @GetMapping
    public ApiResponse<String> health() {
        String result = "Healthy Server";

        return ApiResponse.success(result);
    }
}
