package com.gitranker.api.global.logging;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class LoggingFilter implements Filter {

    private static final String[] STATIC_PREFIXES = {"/js/", "/css/", "/favicon.ico"};

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        String requestUri = httpRequest.getRequestURI();

        if (isStaticResource(requestUri)) {
            chain.doFilter(request, response);
            return;
        }

        MdcUtils.setupHttpRequestContext(httpRequest);

        long start = System.currentTimeMillis();

        log.info("[HTTP Request] {} {}", httpRequest.getMethod(), requestUri);

        try {
            chain.doFilter(request, response);

        } finally {
            long latency = System.currentTimeMillis() - start;
            int status = httpResponse.getStatus();

            MdcUtils.setLatency(latency);
            MdcUtils.setHttpStatus(status);

            if (latency > 10_000) {
                log.warn("[HTTP Response] {} {} - Status: {}, Latency: {}ms (Slow Request)",
                        httpRequest.getMethod(), requestUri, status, latency);
            } else {
                log.info("[HTTP Response] {} {} - Status: {}, Latency: {}ms",
                        httpRequest.getMethod(), requestUri, status, latency);
            }

            MdcUtils.clear();
        }
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
