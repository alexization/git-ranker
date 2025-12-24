package com.gitranker.api.global.aop;

import com.gitranker.api.global.logging.MdcUtils;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
public class GitHubApiLoggingAspect {

    @Around("execution(* com.gitranker.api.infrastructure.github.GitHubGraphQLClient.*(..))")
    public Object logGithubApiCall(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        String methodName = joinPoint.getSignature().getName();

        log.info("[External API] GitHub API 요청 - Method: {}", methodName);

        try {
            Object result = joinPoint.proceed();

            long latency = System.currentTimeMillis() - start;
            MdcUtils.setGithubApiCallTime(latency);
            String cost = MdcUtils.getGithubApiCost();

            log.info("[External API] GitHub API 응답 - Method: {}, Latency: {}ms, Cost: {}", methodName, latency, cost != null ? cost : "N/A");

            return result;
        } catch (Exception e) {
            long latency = System.currentTimeMillis() - start;
            MdcUtils.setGithubApiCallTime(latency);

            log.error("[External API] GitHub API 에러 - Method: {}, Latency: {}ms, Reason: {}", methodName, latency, e.getMessage());
            throw e;
        }
    }
}
