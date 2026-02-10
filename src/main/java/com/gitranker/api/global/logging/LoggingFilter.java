package com.gitranker.api.global.logging;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class LoggingFilter implements Filter {

    private static final String[] STATIC_PREFIXES = {"/js/", "/css/", "/favicon.ico"};
    private static final long SLOW_REQUEST_THRESHOLD_MS = 10_000L;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        String requestUri = httpRequest.getRequestURI();

        if (isStaticResource(requestUri)) {
            chain.doFilter(request, response);
            return;
        }

        LogContext.initRequest(
                LogContext.generateTraceId(),
                resolveClientIp(httpRequest),
                httpRequest.getHeader("User-Agent"),
                httpRequest.getMethod(),
                requestUri
        );

        long start = System.currentTimeMillis();

        try {
            chain.doFilter(request, response);
        } finally {
            long latency = System.currentTimeMillis() - start;
            int status = httpResponse.getStatus();

            LogContext logContext = LogContext.event(Event.HTTP_RESPONSE)
                    .with("status", status)
                    .with("latency_ms", latency);

            if (latency > SLOW_REQUEST_THRESHOLD_MS) {
                logContext.warn();
            } else {
                logContext.info();
            }

            LogContext.clear();
        }
    }

    private String resolveClientIp(HttpServletRequest request) {
        String[] headers = {"X-Forwarded-For", "X-Real-IP", "Proxy-Client-IP"};
        for (String header : headers) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                return ip.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }

    private boolean isStaticResource(String uri) {
        for (String prefix : STATIC_PREFIXES) {
            if (uri.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
}
