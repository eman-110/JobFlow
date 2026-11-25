package com.jobflow.config;

import com.jobflow.service.AuthRateLimiter;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Clock;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthRateLimitFilterTest {
    @Test
    void sends429RetryAfterAndAuditSafeBodyAtLimit() throws Exception {
        AuthRateLimiter limiter = new AuthRateLimiter(1, 1, 1, Clock.systemUTC());
        AuthRateLimitFilter filter = new AuthRateLimitFilter(limiter);
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request(), new MockHttpServletResponse(), chain);
        MockHttpServletResponse rejected = new MockHttpServletResponse();
        filter.doFilter(request(), rejected, chain);

        assertEquals(429, rejected.getStatus());
        assertTrue(Long.parseLong(rejected.getHeader("Retry-After")) >= 1);
        assertTrue(rejected.getContentAsString().contains("\"status\":429"));
        assertFalse(rejected.getContentAsString().contains("password"));
        verify(chain, times(1)).doFilter(any(), any());
    }

    @Test
    void doesNotRateLimitUnprotectedMethodsOrRoutes() throws Exception {
        AuthRateLimitFilter filter = new AuthRateLimitFilter(new AuthRateLimiter(1, 1, 1, Clock.systemUTC()));
        AtomicInteger calls = new AtomicInteger();
        FilterChain chain = (request, response) -> calls.incrementAndGet();

        MockHttpServletRequest get = new MockHttpServletRequest("GET", "/api/auth/login");
        MockHttpServletRequest logout = new MockHttpServletRequest("POST", "/api/auth/logout");
        filter.doFilter(get, new MockHttpServletResponse(), chain);
        filter.doFilter(logout, new MockHttpServletResponse(), chain);

        assertEquals(2, calls.get());
    }

    private static MockHttpServletRequest request() {
        return new MockHttpServletRequest("POST", "/api/auth/login");
    }
}
