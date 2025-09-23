package com.hawkins.xtreamjson.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.hawkins.xtreamjson.service.IptvProviderService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class ProviderSetupInterceptor implements HandlerInterceptor {
    @Autowired
    private IptvProviderService providerService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();
        // Allow access to /providers and static resources
        if (uri.startsWith("/providers") || uri.startsWith("/css") || uri.startsWith("/js") || uri.startsWith("/images") || uri.startsWith("/webjars")) {
            return true;
        }
        if (providerService.getAllProviders().isEmpty()) {
            response.sendRedirect("/providers");
            return false;
        }
        return true;
    }
}
