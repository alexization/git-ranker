package com.gitranker.api.global.interceptor;

import com.gitranker.api.global.error.exception.BusinessException;
import com.gitranker.api.global.error.ErrorType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
public class AdminAuthorizationInterceptor implements HandlerInterceptor {

    @Value("${admin.api-key}")
    private String adminApiKey;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (request.getMethod().equals("OPTIONS")) {
            return true;
        }

        String requestApiKey = request.getHeader("X-API-KEY");

        if (requestApiKey == null || !requestApiKey.equals(adminApiKey)) {
            throw new BusinessException(ErrorType.UNAUTHORIZED_ACCESS);
        }
        return true;
    }
}
