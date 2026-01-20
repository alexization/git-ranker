package com.gitranker.api.global.aop;

import com.gitranker.api.global.logging.EventType;
import com.gitranker.api.global.logging.LogCategory;
import com.gitranker.api.global.logging.MdcUtils;
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

        MdcUtils.setLogContext(LogCategory.EXTERNAL_API, EventType.API_CALLED);
        log.debug("GitHub API 호출 시작 - Method: {}", methodName);

        long start = System.currentTimeMillis();

        try {
            Object result = joinPoint.proceed();

            long latency = System.currentTimeMillis() - start;
            MdcUtils.setGithubApiCallTime(latency);

            String cost = MdcUtils.getGithubApiCost();

            MdcUtils.setEventType(EventType.API_SUCCEEDED);
            log.info("GitHub API 응답 수신 - Method: {}, Latency: {}ms, Cost: {}",
                    methodName, latency, cost);

            apiMetrics.recordSuccess(latency);

            return result;

        } catch (Exception e) {
            long latency = System.currentTimeMillis() - start;
            MdcUtils.setGithubApiCallTime(latency);

            MdcUtils.setEventType(EventType.API_FAILED);
            MdcUtils.setError(e.getClass().getSimpleName(), e.getMessage());
            log.error("GitHub API 실패 - Method: {}, Latency: {}ms, Error: {}",
                    methodName, latency, e.getMessage());

            throw e;
        }
    }
}
