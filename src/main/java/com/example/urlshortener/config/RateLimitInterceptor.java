package com.example.urlshortener.config;

import com.example.urlshortener.service.RateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimitService rateLimitService;
    private final RateLimitConfig rateLimitConfig;

    public RateLimitInterceptor(RateLimitService rateLimitService, RateLimitConfig rateLimitConfig) {
        this.rateLimitService = rateLimitService;
        this.rateLimitConfig = rateLimitConfig;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String clientIp = getClientIp(request);
        String path = request.getRequestURI();
        String method = request.getMethod();

        boolean allowed;
        long retryAfter;

        // Apply different rate limits based on the endpoint
        if (path.equals("/shorten") && method.equals("POST")) {
            int limit = rateLimitConfig.getShortenRequestsPerMinute();
            allowed = rateLimitService.allowShortenRequest(clientIp, limit);
            retryAfter = allowed ? 0 : rateLimitService.getSecondsUntilShortenRefill(clientIp, limit);
        } else if (path.matches("/[a-zA-Z0-9]+") && method.equals("GET")) {
            // Match redirect endpoints (GET /{code})
            int limit = rateLimitConfig.getRedirectRequestsPerMinute();
            allowed = rateLimitService.allowRedirectRequest(clientIp, limit);
            retryAfter = allowed ? 0 : rateLimitService.getSecondsUntilRedirectRefill(clientIp, limit);
        } else {
            // No rate limiting for other endpoints
            return true;
        }

        if (!allowed) {
            response.setStatus(429);
            response.setHeader("Retry-After", String.valueOf(Math.max(1, retryAfter)));
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Too many requests. Please try again later.\"}");
            return false;
        }

        return true;
    }

    /**
     * Extracts the client IP address from the request, considering proxy headers.
     *
     * @param request The HTTP request
     * @return The client IP address
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty()) {
            // X-Forwarded-For can contain multiple IPs, take the first one
            return ip.split(",")[0].trim();
        }
        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isEmpty()) {
            return ip;
        }
        return request.getRemoteAddr();
    }
}
