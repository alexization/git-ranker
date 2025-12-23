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

        try {
            log.info("[GitHub API Request] Method: {}", methodName);

            Object result = joinPoint.proceed();

            long latency = System.currentTimeMillis() - start;
            MdcUtils.setGithubApiCallTime(latency);

            log.info("[GitHub API Response] Method: {}, Latency: {}", methodName, latency);

            return result;
        } catch (Exception e) {
            long latency = System.currentTimeMillis() - start;
            MdcUtils.setGithubApiCallTime(latency);

            log.error("[GitHub API Error] Method: {}, Latency: {}, Error: {}", methodName, latency, e.getMessage());
            throw e;
        }
    }
}
