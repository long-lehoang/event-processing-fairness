package com.event.processing.dlq_service.util;

import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Utility class for retry-related operations.
 * This class provides common methods used across different services.
 */
public final class RetryUtils {

  private RetryUtils() {
    // Private constructor to prevent instantiation
  }

  /**
   * Calculates the next retry time based on the retry count and configured delay parameters.
   * Uses an exponential backoff strategy.
   *
   * @param retryCount          The current retry count
   * @param initialDelaySeconds The initial delay in seconds
   * @param multiplier          The multiplier for exponential backoff
   * @return The next retry time
   */
  public static Instant calculateNextRetryTime(int retryCount, long initialDelaySeconds, double multiplier) {
    long delaySeconds = (long) (initialDelaySeconds * Math.pow(multiplier, retryCount));
    return Instant.now().plusSeconds(delaySeconds);
  }

  /**
   * Safely shuts down an executor service.
   *
   * @param executor       The executor service to shut down
   * @param timeoutSeconds The timeout in seconds to wait for termination
   */
  public static void shutdownExecutorService(ExecutorService executor, int timeoutSeconds) {
    if (executor != null) {
      executor.shutdown();
      try {
        if (!executor.awaitTermination(timeoutSeconds, TimeUnit.SECONDS)) {
          executor.shutdownNow();
        }
      } catch (InterruptedException e) {
        executor.shutdownNow();
        Thread.currentThread().interrupt();
      }
    }
  }
}
