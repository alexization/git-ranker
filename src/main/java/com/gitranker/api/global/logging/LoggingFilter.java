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

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        MdcUtils.setupHttpRequestContext(httpRequest);

        long start = System.currentTimeMillis();

        log.info("[HTTP Request] {} {}", httpRequest.getMethod(), httpRequest.getRequestURI());

        try {
            chain.doFilter(request, response);

        } finally {
            long latency = System.currentTimeMillis() - start;
            int status = httpResponse.getStatus();

            MdcUtils.setLatency(latency);
            MdcUtils.setHttpStatus(status);

            if (latency > 10_000) {
                log.warn("[HTTP Response] {} {} - Status: {}, Latency: {}ms (Slow Request)",
                        httpRequest.getMethod(), httpRequest.getRequestURI(), status, latency);
            } else {
                log.info("[HTTP Response] {} {} - Status: {}, Latency: {}ms",
                        httpRequest.getMethod(), httpRequest.getRequestURI(), status, latency);
            }

            MdcUtils.clear();
        }
    }
}
