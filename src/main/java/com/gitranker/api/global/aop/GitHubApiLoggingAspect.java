package com.gitranker.api.global.aop;

import com.gitranker.api.global.logging.Event;
import com.gitranker.api.global.logging.LogContext;
import com.gitranker.api.infrastructure.github.GitHubApiMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class GitHubApiLoggingAspect {

    private final GitHubApiMetrics apiMetrics;

    @Around("execution(* com.gitranker.api.infrastructure.github.GitHubGraphQLClient.*(..))")
    public Object logGithubApiCall(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();

        log.debug("GitHub API 호출 시작 - Method: {}", methodName);

        long start = System.currentTimeMillis();

        try {
            Object result = joinPoint.proceed();

            long latency = System.currentTimeMillis() - start;

            LogContext.event(Event.GITHUB_API_CALLED)
                    .with("method", methodName)
                    .with("latency_ms", latency)
                    .with("success", true)
                    .info();

            apiMetrics.recordSuccess(latency);

            return result;

        } catch (Exception e) {
            long latency = System.currentTimeMillis() - start;

            LogContext.event(Event.GITHUB_API_CALLED)
                    .with("method", methodName)
                    .with("latency_ms", latency)
                    .with("success", false)
                    .with("error_type", e.getClass().getSimpleName())
                    .with("error_message", e.getMessage())
                    .error();

            throw e;
        }
    }
}
