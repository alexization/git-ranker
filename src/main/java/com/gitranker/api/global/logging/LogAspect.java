package com.gitranker.api.global.logging;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
public class LogAspect {

    @Around("execution(* com.gitranker.api.domain..*Controller.*(..))")
    public Object logging(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();

        try {
            Object result = joinPoint.proceed();

            long end = System.currentTimeMillis();
            MDC.put("latency_ms", String.valueOf(end - start));
            log.info("API Request");

            return result;

        } catch (Throwable e) {
            long end = System.currentTimeMillis();
            MDC.put("latency_ms", String.valueOf(end - start));

            throw e;
        }
    }
}
