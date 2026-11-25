package com.jobflow.service;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.*;

class AuthRateLimiterTest {
    @Test
    void allowsRequestsUpToConfiguredLimitThenReturnsRetryTime() {
        MutableClock clock = new MutableClock(Instant.parse("2026-01-01T00:00:15Z"));
        AuthRateLimiter limiter = new AuthRateLimiter(2, 3, 4, clock);

        assertTrue(limiter.check("/api/auth/login", "127.0.0.1").allowed());
        assertTrue(limiter.check("/api/auth/login", "127.0.0.1").allowed());
        AuthRateLimiter.Result denied = limiter.check("/api/auth/login", "127.0.0.1");

        assertFalse(denied.allowed());
        assertEquals(45, denied.retryAfterSeconds());
    }

    @Test
    void keepsRegistrationLoginAndRefreshLimitsIndependent() {
        AuthRateLimiter limiter = new AuthRateLimiter(1, 1, 1, Clock.systemUTC());

        assertTrue(limiter.check("/api/auth/login", "client").allowed());
        assertTrue(limiter.check("/api/auth/register", "client").allowed());
        assertTrue(limiter.check("/api/auth/refresh", "client").allowed());
    }

    @Test
    void keepsClientAddressBucketsIndependent() {
        AuthRateLimiter limiter = new AuthRateLimiter(1, 1, 1, Clock.systemUTC());

        assertTrue(limiter.check("/api/auth/login", "client-one").allowed());
        assertTrue(limiter.check("/api/auth/login", "client-two").allowed());
    }

    @Test
    void startsNewWindowAtNextUtcMinute() {
        MutableClock clock = new MutableClock(Instant.parse("2026-01-01T00:00:59Z"));
        AuthRateLimiter limiter = new AuthRateLimiter(1, 1, 1, clock);
        assertTrue(limiter.check("/api/auth/login", "client").allowed());
        assertFalse(limiter.check("/api/auth/login", "client").allowed());

        clock.set(Instant.parse("2026-01-01T00:01:00Z"));
        assertTrue(limiter.check("/api/auth/login", "client").allowed());
    }

    @Test
    void rejectsUnconfiguredRoute() {
        AuthRateLimiter limiter = new AuthRateLimiter(1, 1, 1, Clock.systemUTC());
        assertThrows(IllegalArgumentException.class, () -> limiter.check("/api/auth/logout", "client"));
    }

    @Test
    void rejectsNonpositiveConfiguredLimits() {
        assertThrows(IllegalArgumentException.class,
                () -> new AuthRateLimiter(0, 1, 1, Clock.systemUTC()));
        assertThrows(IllegalArgumentException.class,
                () -> new AuthRateLimiter(1, -1, 1, Clock.systemUTC()));
        assertThrows(IllegalArgumentException.class,
                () -> new AuthRateLimiter(1, 1, 0, Clock.systemUTC()));
    }

    private static final class MutableClock extends Clock {
        private Instant current;

        private MutableClock(Instant current) { this.current = current; }
        void set(Instant instant) { current = instant; }
        @Override public ZoneId getZone() { return ZoneId.of("UTC"); }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return current; }
    }
}
