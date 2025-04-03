package com.event.processing.notifier.service.impl;

import com.event.processing.notifier.util.RateLimitProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountRateLimiterServiceImplTest {

    private static final String ACCOUNT_ID = "test-account-123";
    private static final String RATE_LIMIT_KEY_PREFIX = "rate-limit:";
    private static final String EXPECTED_REDIS_KEY = RATE_LIMIT_KEY_PREFIX + ACCOUNT_ID;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private RateLimitProperties rateLimitProperties;

    @Captor
    private ArgumentCaptor<RedisScript<Boolean>> scriptCaptor;

    @Captor
    private ArgumentCaptor<List<String>> keysCaptor;

    @Captor
    private ArgumentCaptor<String[]> argsCaptor;

    private AccountRateLimiterServiceImpl rateLimiterService;

    @BeforeEach
    void setUp() {
        rateLimiterService = new AccountRateLimiterServiceImpl(redisTemplate, rateLimitProperties);
        
        // Set default properties
        when(rateLimitProperties.getEvent()).thenReturn(100);
        when(rateLimitProperties.getTime()).thenReturn(1);
    }

    @Test
    @DisplayName("isAllow should call areEventsAllowed with count 1")
    void isAllow_ShouldCallAreEventsAllowedWithCountOne() {
        // Arrange
        when(redisTemplate.execute(
            any(RedisScript.class),
            anyList(),
            any(String[].class)
        )).thenReturn(true);

        // Act
        boolean result = rateLimiterService.isAllow(ACCOUNT_ID);

        // Assert
        assertTrue(result);
        verify(redisTemplate).execute(
            any(RedisScript.class),
            eq(Collections.singletonList(EXPECTED_REDIS_KEY)),
            eq(new String[]{"1", "100", "60"})
        );
    }

    @Test
    @DisplayName("areEventsAllowed should return true when Redis allows the operation")
    void areEventsAllowed_ShouldReturnTrue_WhenRedisAllowsOperation() {
        // Arrange
        int count = 5;
        when(redisTemplate.execute(
            any(RedisScript.class),
            anyList(),
            any(String[].class)
        )).thenReturn(true);

        // Act
        boolean result = rateLimiterService.areEventsAllowed(ACCOUNT_ID, count);

        // Assert
        assertTrue(result);
        verify(redisTemplate).execute(
            any(RedisScript.class),
            eq(Collections.singletonList(EXPECTED_REDIS_KEY)),
            eq(new String[]{String.valueOf(count), "100", "60"})
        );
    }

    @Test
    @DisplayName("areEventsAllowed should return false when Redis denies the operation")
    void areEventsAllowed_ShouldReturnFalse_WhenRedisDeniesOperation() {
        // Arrange
        int count = 5;
        when(redisTemplate.execute(
            any(),
            anyList(),
            any(),
            any(),
            any()
        )).thenReturn(false);

        // Act
        boolean result = rateLimiterService.areEventsAllowed(ACCOUNT_ID, count);

        // Assert
        assertFalse(result);
        verify(redisTemplate).execute(
            any(),
            eq(Collections.singletonList(EXPECTED_REDIS_KEY)),
            eq(new String[]{String.valueOf(count), "100", "60"})
        );
    }

    @Test
    @DisplayName("areEventsAllowed should return true when Redis throws an exception (fail-safe behavior)")
    void areEventsAllowed_ShouldReturnTrue_WhenRedisThrowsException() {
        // Arrange
        int count = 5;
        when(redisTemplate.execute(
            any(RedisScript.class),
            anyList(),
            any(String[].class)
        )).thenThrow(new RuntimeException("Redis connection error"));

        // Act
        boolean result = rateLimiterService.areEventsAllowed(ACCOUNT_ID, count);

        // Assert
        assertTrue(result, "Should return true in fail-safe mode when Redis fails");
        verify(redisTemplate).execute(
            any(RedisScript.class),
            eq(Collections.singletonList(EXPECTED_REDIS_KEY)),
            eq(new String[]{String.valueOf(count), "100", "60"})
        );
    }

    @Test
    @DisplayName("areEventsAllowed should use custom rate limit values from properties")
    void areEventsAllowed_ShouldUseCustomRateLimitValues() {
        // Arrange
        int count = 5;
        int customEventLimit = 200;
        int customTimeMinutes = 2;
        
        when(rateLimitProperties.getEvent()).thenReturn(customEventLimit);
        when(rateLimitProperties.getTime()).thenReturn(customTimeMinutes);
        
        when(redisTemplate.execute(
            any(RedisScript.class),
            anyList(),
            any(String[].class)
        )).thenReturn(true);

        // Act
        boolean result = rateLimiterService.areEventsAllowed(ACCOUNT_ID, count);

        // Assert
        assertTrue(result);
        verify(redisTemplate).execute(
            any(RedisScript.class),
            eq(Collections.singletonList(EXPECTED_REDIS_KEY)),
            eq(new String[]{String.valueOf(count), String.valueOf(customEventLimit), String.valueOf(customTimeMinutes * 60)})
        );
    }
} 