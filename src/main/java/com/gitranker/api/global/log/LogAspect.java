package com.gitranker.api.global.log;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
@Aspect
@Component
public class LogAspect {

    @Pointcut("execution(* com.gitranker.api.domain..*Controller.*(..))")
    public void controller() {
    }

    @Around("controller()")
    public Object logging(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();

        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String clientIp = getClientIp(request);

        try {
            Object result = joinPoint.proceed();

            long end = System.currentTimeMillis();

            log.info("[API] {} {} | IP: {} | Latency: {}ms", method, uri, clientIp, end - start);

            return result;
        } catch (Throwable e) {
            long end = System.currentTimeMillis();

            log.error("[API ERROR] {} {} | IP: {} | Latency: {}ms | Exception: {}", method, uri, clientIp, end - start, e.getMessage());
            throw e;
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");

        return (ip != null) ? ip : request.getRemoteAddr();
    }
}
