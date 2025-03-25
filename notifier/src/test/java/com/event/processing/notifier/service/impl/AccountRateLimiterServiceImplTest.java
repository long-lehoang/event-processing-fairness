package com.event.processing.notifier.service.impl;

import com.event.processing.notifier.util.RateLimitProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AccountRateLimiterServiceImpl Tests")
class AccountRateLimiterServiceImplTest {

  @Mock
  private StringRedisTemplate redisTemplate;

  @Mock
  private ValueOperations<String, String> valueOperations;

  @Mock
  private RateLimitProperties rateLimitProperties;

  private AccountRateLimiterServiceImpl rateLimiterService;

  private static final String ACCOUNT_ID = "test-account-id";
  private static final String EXPECTED_KEY = "rate-limit:" + ACCOUNT_ID;
  private static final int MAX_EVENTS = 100;
  private static final int TIME_WINDOW_MINUTES = 5;

  @BeforeEach
  void setUp() {
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    when(rateLimitProperties.getEvent()).thenReturn(MAX_EVENTS);
    lenient().when(rateLimitProperties.getTime()).thenReturn(TIME_WINDOW_MINUTES);
    rateLimiterService = new AccountRateLimiterServiceImpl(redisTemplate, rateLimitProperties);
  }

  @Nested
  @DisplayName("isAllow Tests")
  class IsAllowTests {

    @Test
    @DisplayName("Should allow request when within rate limit")
    void shouldAllowRequestWhenWithinRateLimit() {
      // Arrange
      when(valueOperations.increment(anyString(), eq(1L))).thenReturn(1L);

      // Act
      boolean result = rateLimiterService.isAllow(ACCOUNT_ID);

      // Assert
      assertThat(result).isTrue();
      verify(valueOperations).increment(EXPECTED_KEY, 1L);
      verify(redisTemplate).expire(eq(EXPECTED_KEY), eq(Duration.ofMinutes(TIME_WINDOW_MINUTES)));
    }

    @Test
    @DisplayName("Should not allow request when rate limit exceeded")
    void shouldNotAllowRequestWhenRateLimitExceeded() {
      // Arrange
      when(valueOperations.increment(anyString(), eq(1L))).thenReturn(MAX_EVENTS + 1L);

      // Act
      boolean result = rateLimiterService.isAllow(ACCOUNT_ID);

      // Assert
      assertThat(result).isFalse();
      verify(valueOperations).increment(EXPECTED_KEY, 1L);
    }

    @Test
    @DisplayName("Should handle null increment result")
    void shouldHandleNullIncrementResult() {
      // Arrange
      when(valueOperations.increment(anyString(), eq(1))).thenReturn(null);

      // Act
      boolean result = rateLimiterService.isAllow(ACCOUNT_ID);

      // Assert
      assertThat(result).isTrue();
      verify(valueOperations).increment(EXPECTED_KEY, 1);
    }

    @Test
    @DisplayName("Should handle Redis operation failure")
    void shouldHandleRedisOperationFailure() {
      // Arrange
      doThrow(new RuntimeException("Redis operation failed"))
          .when(valueOperations).increment(anyString(), eq(1));

      // Act & Assert
      boolean result = rateLimiterService.isAllow(ACCOUNT_ID);

      // Assert
      assertThat(result).isTrue(); // Fail-safe behavior
      verify(valueOperations).increment(EXPECTED_KEY, 1);
    }

    @Test
    @DisplayName("Should handle null accountId")
    void shouldHandleNullAccountId() {
      // Act
      boolean result = rateLimiterService.isAllow(null);

      // Assert
      assertThat(result).isTrue(); // Fail-safe behavior
      verify(valueOperations).increment("rate-limit:null", 1L);
    }
  }

  @Nested
  @DisplayName("Integration Tests")
  class IntegrationTests {

    @Test
    @DisplayName("Should correctly handle rate limiting flow")
    void shouldHandleRateLimitingFlow() {
      // Arrange
      when(valueOperations.increment(anyString(), eq(1L)))
          .thenReturn(1L, 2L, 3L, MAX_EVENTS + 1L);

      // Act
      boolean firstRequest = rateLimiterService.isAllow(ACCOUNT_ID);
      boolean secondRequest = rateLimiterService.isAllow(ACCOUNT_ID);
      boolean thirdRequest = rateLimiterService.isAllow(ACCOUNT_ID);
      boolean exceededRequest = rateLimiterService.isAllow(ACCOUNT_ID);

      // Assert
      assertThat(firstRequest).isTrue();
      assertThat(secondRequest).isTrue();
      assertThat(thirdRequest).isTrue();
      assertThat(exceededRequest).isFalse();

      verify(valueOperations, times(4)).increment(EXPECTED_KEY, 1L);
      verify(redisTemplate).expire(eq(EXPECTED_KEY), eq(Duration.ofMinutes(TIME_WINDOW_MINUTES)));
    }
  }
}