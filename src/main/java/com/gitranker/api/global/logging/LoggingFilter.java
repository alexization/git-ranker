package com.gitranker.api.global.logging;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class LoggingFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;

        String traceId = UUID.randomUUID().toString().substring(0, 8);
        MDC.put("trace_id", traceId);
        MDC.put("client_ip", getClientIp(httpRequest));
        MDC.put("uri", httpRequest.getRequestURI());
        MDC.put("method", httpRequest.getMethod());

        try {
            chain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        return (ip != null) ? ip : request.getRemoteAddr();
    }
}
