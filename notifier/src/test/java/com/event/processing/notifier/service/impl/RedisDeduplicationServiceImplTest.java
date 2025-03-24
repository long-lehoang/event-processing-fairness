package com.event.processing.notifier.service.impl;

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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RedisDeduplicationServiceImpl Tests")
class RedisDeduplicationServiceImplTest {

  @Mock
  private StringRedisTemplate redisTemplate;

  @Mock
  private ValueOperations<String, String> valueOperations;

  private RedisDeduplicationServiceImpl deduplicationService;

  private static final String EVENT_ID = "test-event-id";
  private static final String EXPECTED_KEY = "deduplicate:event:" + EVENT_ID;
  private static final String EXPECTED_VALUE = "1";
  private static final Duration EXPECTED_EXPIRY = Duration.ofMinutes(30);

  @BeforeEach
  void setUp() {
      when(redisTemplate.opsForValue()).thenReturn(valueOperations);
      deduplicationService = new RedisDeduplicationServiceImpl(redisTemplate);
  }

  @Nested
  @DisplayName("isDuplicate Tests")
  class IsDuplicateTests {

    @Test
    @DisplayName("Should return true when event exists in Redis")
    void shouldReturnTrueWhenEventExists() {
      // Arrange
      when(redisTemplate.hasKey(EXPECTED_KEY)).thenReturn(true);

      // Act
      boolean result = deduplicationService.isDuplicate(EVENT_ID);

      // Assert
      assertThat(result).isTrue();
      verify(redisTemplate).hasKey(EXPECTED_KEY);
    }

    @Test
    @DisplayName("Should return false when event does not exist in Redis")
    void shouldReturnFalseWhenEventDoesNotExist() {
      // Arrange
      when(redisTemplate.hasKey(EXPECTED_KEY)).thenReturn(false);

      // Act
      boolean result = deduplicationService.isDuplicate(EVENT_ID);

      // Assert
      assertThat(result).isFalse();
      verify(redisTemplate).hasKey(EXPECTED_KEY);
    }

    @Test
    @DisplayName("Should return false when Redis returns null")
    void shouldReturnFalseWhenRedisReturnsNull() {
      // Arrange
      when(redisTemplate.hasKey(EXPECTED_KEY)).thenReturn(null);

      // Act
      boolean result = deduplicationService.isDuplicate(EVENT_ID);

      // Assert
      assertThat(result).isFalse();
      verify(redisTemplate).hasKey(EXPECTED_KEY);
    }

    @Test
    @DisplayName("Should handle null eventId")
    void shouldHandleNullEventId() {
      // Act & Assert
      assertThat(deduplicationService.isDuplicate(null)).isFalse();
      verify(redisTemplate).hasKey("deduplicate:event:null");
    }
  }

  @Nested
  @DisplayName("markProcessed Tests")
  class MarkProcessedTests {

    @Test
    @DisplayName("Should mark event as processed with correct parameters")
    void shouldMarkEventAsProcessedWithCorrectParameters() {
      // Arrange
      ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
      ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
      ArgumentCaptor<Duration> durationCaptor = ArgumentCaptor.forClass(Duration.class);

      // Act
      deduplicationService.markProcessed(EVENT_ID);

      // Assert
      verify(valueOperations).set(
              keyCaptor.capture(),
              valueCaptor.capture(),
              durationCaptor.capture());

      assertThat(keyCaptor.getValue()).isEqualTo(EXPECTED_KEY);
      assertThat(valueCaptor.getValue()).isEqualTo(EXPECTED_VALUE);
      assertThat(durationCaptor.getValue()).isEqualTo(EXPECTED_EXPIRY);
    }

    @Test
    @DisplayName("Should handle null eventId")
    void shouldHandleNullEventId() {
      // Act
      deduplicationService.markProcessed(null);

      // Assert
      verify(valueOperations).set(
              eq("deduplicate:event:null"),
              eq(EXPECTED_VALUE),
              eq(EXPECTED_EXPIRY));
    }

    @Test
    @DisplayName("Should handle Redis operation failure")
    void shouldHandleRedisOperationFailure() {
      // Arrange
      RuntimeException expectedException = new RuntimeException("Redis operation failed");
      doThrow(expectedException)
              .when(valueOperations)
              .set(anyString(), anyString(), any(Duration.class));

      // Act & Assert
      assertThatThrownBy(() -> deduplicationService.markProcessed(EVENT_ID))
              .isInstanceOf(RuntimeException.class)
              .hasMessage("Redis operation failed");
    }
  }

  @Nested
  @DisplayName("Integration Tests")
  class IntegrationTests {

    @Test
    @DisplayName("Should correctly handle full deduplication flow")
    void shouldHandleFullDeduplicationFlow() {
      // Arrange
      when(redisTemplate.hasKey(EXPECTED_KEY)).thenReturn(false);
      ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
      ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
      ArgumentCaptor<Duration> durationCaptor = ArgumentCaptor.forClass(Duration.class);

      // Act
      boolean isDuplicate = deduplicationService.isDuplicate(EVENT_ID);
      deduplicationService.markProcessed(EVENT_ID);
      boolean isDuplicateAfterMarking = deduplicationService.isDuplicate(EVENT_ID);

      // Assert
      assertThat(isDuplicate).isFalse();
      assertThat(isDuplicateAfterMarking).isTrue();

      verify(valueOperations).set(
              keyCaptor.capture(),
              valueCaptor.capture(),
              durationCaptor.capture());

      assertThat(keyCaptor.getValue()).isEqualTo(EXPECTED_KEY);
      assertThat(valueCaptor.getValue()).isEqualTo(EXPECTED_VALUE);
      assertThat(durationCaptor.getValue()).isEqualTo(EXPECTED_EXPIRY);
    }
  }
}