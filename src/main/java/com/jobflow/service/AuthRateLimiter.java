package com.jobflow.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AuthRateLimiter {
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
    private final int loginLimit;
    private final int registrationLimit;
    private final int refreshLimit;
    private final Clock clock;

    @Autowired
    public AuthRateLimiter(
            @Value("${jobflow.security.rate-limit.login:10}") int loginLimit,
            @Value("${jobflow.security.rate-limit.registration:5}") int registrationLimit,
            @Value("${jobflow.security.rate-limit.refresh:30}") int refreshLimit) {
        this(loginLimit, registrationLimit, refreshLimit, Clock.systemUTC());
    }

    public AuthRateLimiter(int loginLimit, int registrationLimit, int refreshLimit, Clock clock) {
        if (loginLimit < 1 || registrationLimit < 1 || refreshLimit < 1) {
            throw new IllegalArgumentException("Rate limits must be positive.");
        }
        this.loginLimit = loginLimit;
        this.registrationLimit = registrationLimit;
        this.refreshLimit = refreshLimit;
        this.clock = clock;
    }

    public Result check(String endpoint, String clientAddress) {
        int limit = switch (endpoint) {
            case "/api/auth/login" -> loginLimit;
            case "/api/auth/register" -> registrationLimit;
            case "/api/auth/refresh" -> refreshLimit;
            default -> throw new IllegalArgumentException("Unsupported rate-limited endpoint.");
        };
        long now = clock.instant().getEpochSecond();
        long bucket = Math.floorDiv(now, 60);
        String key = endpoint + ":" + clientAddress + ":" + bucket;
        windows.keySet().removeIf(candidate -> !candidate.endsWith(":" + bucket));
        Window window = windows.computeIfAbsent(key, ignored -> new Window());
        int count = window.increment();
        return new Result(count <= limit, Math.max(1, (bucket + 1) * 60 - now));
    }

    private static final class Window {
        private int count;
        synchronized int increment() { return ++count; }
    }

    public record Result(boolean allowed, long retryAfterSeconds) {}
}
