package com.jobflow.config;

import com.jobflow.service.AuthRateLimiter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class AuthRateLimitFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(AuthRateLimitFilter.class);
    private final AuthRateLimiter limiter;

    public AuthRateLimitFilter(AuthRateLimiter limiter) {
        this.limiter = limiter;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        if (!"POST".equals(request.getMethod())
                || !path.equals("/api/auth/login") && !path.equals("/api/auth/register")
                && !path.equals("/api/auth/refresh")) {
            filterChain.doFilter(request, response);
            return;
        }
        AuthRateLimiter.Result result = limiter.check(path, request.getRemoteAddr());
        if (!result.allowed()) {
            log.warn("audit event=rate_limit_rejected endpoint={}", path);
            response.setStatus(429);
            response.setHeader("Retry-After", Long.toString(result.retryAfterSeconds()));
            response.setContentType("application/json");
            response.getWriter().write("{\"status\":429,\"message\":\"Too many requests\",\"retryAfterSeconds\":"
                    + result.retryAfterSeconds() + "}");
            return;
        }
        filterChain.doFilter(request, response);
    }
}
