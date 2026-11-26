package com.jobflow.config;

import com.jobflow.model.Job;
import com.jobflow.model.JobStatus;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
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
    void configuredJsonSerializerRoundTripsJobListingsAndDetails() {
        RedisCacheConfiguration configuration = new CacheConfig().redisCacheConfiguration();
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
        CacheErrorHandler errorHandler = new CacheConfig().errorHandler();
        RuntimeException failure = new IllegalStateException("Redis unavailable");

        assertDoesNotThrow(() -> errorHandler.handleCacheGetError(failure, cache, "all"));
        assertDoesNotThrow(() -> errorHandler.handleCachePutError(failure, cache, "all", "value"));
        assertDoesNotThrow(() -> errorHandler.handleCacheEvictError(failure, cache, "all"));
        assertDoesNotThrow(() -> errorHandler.handleCacheClearError(failure, cache));
    }
}
