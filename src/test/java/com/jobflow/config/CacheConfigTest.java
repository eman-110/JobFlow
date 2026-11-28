package com.jobflow.config;

import com.jobflow.model.Job;
import com.jobflow.model.JobStatus;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CacheConfigTest {
    @Test
    void registersRedisCacheManagerWhenCachingIsEnabled() {
        try (AnnotationConfigApplicationContext context =
                     new AnnotationConfigApplicationContext()) {
            context.getEnvironment().setActiveProfiles("mysql");
            context.registerBean(RedisConnectionFactory.class,
                    () -> mock(RedisConnectionFactory.class));
            context.register(CacheConfig.class);
            context.refresh();

            assertInstanceOf(RedisCacheManager.class, context.getBean(CacheManager.class));
        }
    }

    @Test
    void configuredJsonSerializerRoundTripsJobListingsAndDetails() {
        RedisCacheConfiguration configuration = newCacheConfig().redisCacheConfiguration();
        RedisSerializationContext.SerializationPair<Object> serializer =
                configuration.getValueSerializationPair();
        Job job = new Job(7, "Engineer", "Example Co", "Remote", 120000);
        job.updateStatus(JobStatus.APPLIED);

        Object restoredDetail = serializer.read(serializer.write(job));
        Job restoredJob = assertInstanceOf(Job.class, restoredDetail);
        assertEquals(job.getId(), restoredJob.getId());
        assertEquals(job.getStatus(), restoredJob.getStatus());

        Object restoredListing = serializer.read(serializer.write(new ArrayList<>(List.of(job))));
        List<?> restoredJobs = assertInstanceOf(List.class, restoredListing);
        Job restoredListingJob = assertInstanceOf(Job.class, restoredJobs.getFirst());
        assertEquals(job.getTitle(), restoredListingJob.getTitle());
        assertEquals(job.getStatus(), restoredListingJob.getStatus());
    }

    @Test
    void cacheErrorsAreLoggedAndDoNotFailApplicationOperations() {
        Cache cache = mock(Cache.class);
        when(cache.getName()).thenReturn("jobs");
        CacheErrorHandler errorHandler = newCacheConfig().errorHandler();
        RuntimeException failure = new IllegalStateException("Redis unavailable");

        assertDoesNotThrow(() -> errorHandler.handleCacheGetError(failure, cache, "all"));
        assertDoesNotThrow(() -> errorHandler.handleCachePutError(failure, cache, "all", "value"));
        assertDoesNotThrow(() -> errorHandler.handleCacheEvictError(failure, cache, "all"));
        assertDoesNotThrow(() -> errorHandler.handleCacheClearError(failure, cache));
    }

    private static CacheConfig newCacheConfig() {
        return new CacheConfig(mock(RedisConnectionFactory.class));
    }
}
