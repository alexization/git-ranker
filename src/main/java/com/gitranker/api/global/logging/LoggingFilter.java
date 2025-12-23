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

        try {
            MdcUtils.setupHttpRequestContext(httpRequest);

            log.info("{} 요청 시작", httpRequest.getMethod());

            long start = System.currentTimeMillis();
            chain.doFilter(request, response);
            long latency = System.currentTimeMillis() - start;

            MdcUtils.setLatency(latency);
            int status = httpResponse.getStatus();
            MdcUtils.setHttpStatus(status);

            log.info("{} 요청 완료", httpRequest.getMethod());
        } finally {
            MdcUtils.clear();
        }
    }
}
