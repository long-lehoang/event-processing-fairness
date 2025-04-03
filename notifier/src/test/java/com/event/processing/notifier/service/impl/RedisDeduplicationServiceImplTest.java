package com.event.processing.notifier.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisDeduplicationServiceImplTest {

    private static final String EVENT_ID = "test-event-123";
    private static final String KEY_PREFIX = "deduplicate:event:";
    private static final String EXPECTED_REDIS_KEY = KEY_PREFIX + EVENT_ID;
    private static final Duration EXPIRY_TIME = Duration.ofMinutes(30);

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Captor
    private ArgumentCaptor<String> keyCaptor;

    @Captor
    private ArgumentCaptor<Duration> durationCaptor;

    private RedisDeduplicationServiceImpl deduplicationService;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        deduplicationService = new RedisDeduplicationServiceImpl(redisTemplate);
    }

    @Test
    @DisplayName("isDuplicate should return true when event exists in Redis")
    void isDuplicate_ShouldReturnTrue_WhenEventExistsInRedis() {
        // Arrange
        when(redisTemplate.hasKey(any())).thenReturn(true);

        // Act
        boolean result = deduplicationService.isDuplicate(EVENT_ID);

        // Assert
        assertTrue(result);
        verify(redisTemplate).hasKey(eq(EXPECTED_REDIS_KEY));
    }

    @Test
    @DisplayName("isDuplicate should return false when event doesn't exist in Redis")
    void isDuplicate_ShouldReturnFalse_WhenEventDoesNotExistInRedis() {
        // Arrange
        when(redisTemplate.hasKey(any())).thenReturn(false);

        // Act
        boolean result = deduplicationService.isDuplicate(EVENT_ID);

        // Assert
        assertFalse(result);
        verify(redisTemplate).hasKey(eq(EXPECTED_REDIS_KEY));
    }

    @Test
    @DisplayName("isDuplicate should return false when Redis returns null")
    void isDuplicate_ShouldReturnFalse_WhenRedisReturnsNull() {
        // Arrange
        when(redisTemplate.hasKey(any())).thenReturn(null);

        // Act
        boolean result = deduplicationService.isDuplicate(EVENT_ID);

        // Assert
        assertFalse(result);
        verify(redisTemplate).hasKey(any());
    }

    @Test
    @DisplayName("markProcessed should store event ID in Redis with correct expiration")
    void markProcessed_ShouldStoreEventIdInRedisWithExpiration() {
        // Act
        deduplicationService.markProcessed(EVENT_ID);

        // Assert
        verify(valueOperations).set(eq(EXPECTED_REDIS_KEY), eq("1"), eq(EXPIRY_TIME));
    }

    @Test
    @DisplayName("markProcessed should use correctly formatted Redis key")
    void markProcessed_ShouldUseCorrectlyFormattedRedisKey() {
        // Arrange
        doNothing().when(valueOperations).set(keyCaptor.capture(), any(), any());

        // Act
        deduplicationService.markProcessed(EVENT_ID);

        // Assert
        assertEquals(EXPECTED_REDIS_KEY, keyCaptor.getValue());
    }

    @Test
    @DisplayName("markProcessed should use configured expiry time")
    void markProcessed_ShouldUseConfiguredExpiryTime() {
        // Arrange
        doNothing().when(valueOperations).set(any(), any(), durationCaptor.capture());

        // Act
        deduplicationService.markProcessed(EVENT_ID);

        // Assert
        assertEquals(EXPIRY_TIME, durationCaptor.getValue());
    }
} 