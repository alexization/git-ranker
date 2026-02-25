package com.gitranker.api.global.aop;

import com.gitranker.api.global.logging.Event;
import com.gitranker.api.global.logging.LogContext;
import com.gitranker.api.infrastructure.github.GitHubApiMetrics;
import com.gitranker.api.infrastructure.github.dto.GitHubAllActivitiesResponse;
import com.gitranker.api.infrastructure.github.dto.GitHubRateLimitInfo;
import com.gitranker.api.infrastructure.github.dto.GitHubUserInfoResponse;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class GitHubApiLoggingAspect {

    private final GitHubApiMetrics apiMetrics;

    @Around("execution(* com.gitranker.api.infrastructure.github.GitHubGraphQLClient.*(..))")
    public Object logGithubApiCall(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();

        long start = System.currentTimeMillis();

        try {
            Object result = joinPoint.proceed();

            long latency = System.currentTimeMillis() - start;

            GitHubRateLimitInfo rateLimit = extractRateLimit(result);

            LogContext ctx = LogContext.event(Event.GITHUB_API_CALLED)
                    .with("operation", methodName)
                    .with("target", "github_api")
                    .with("latency_ms", latency)
                    .with("outcome", "success");

            if (rateLimit != null) {
                ctx.with("cost", rateLimit.cost())
                   .with("remaining", rateLimit.remaining());
            }

            ctx.info();

            apiMetrics.recordSuccess(latency);

            return result;

        } catch (Exception e) {
            long latency = System.currentTimeMillis() - start;

            LogContext.event(Event.GITHUB_API_CALLED)
                    .with("operation", methodName)
                    .with("target", "github_api")
                    .with("latency_ms", latency)
                    .with("outcome", "failure")
                    .with("error_type", e.getClass().getSimpleName())
                    .with("error_message", e.getMessage())
                    .error();

            throw e;
        }
    }

    private GitHubRateLimitInfo extractRateLimit(Object result) {
        if (result instanceof GitHubAllActivitiesResponse r
                && r.data() != null && r.data().rateLimit() != null) {
            return r.data().rateLimit();
        }
        if (result instanceof GitHubUserInfoResponse r
                && r.data() != null && r.data().rateLimit() != null) {
            return r.data().rateLimit();
        }
        return null;
    }
}
