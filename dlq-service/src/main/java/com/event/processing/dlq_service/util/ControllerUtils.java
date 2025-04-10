package com.event.processing.dlq_service.util;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;

import java.util.function.Supplier;

/**
 * Utility class for controller operations.
 * This class provides common methods for handling controller operations.
 */
@Slf4j
public final class ControllerUtils {

  private ControllerUtils() {
    // Private constructor to prevent instantiation
  }

  /**
   * Executes an operation with error handling and metrics.
   * This method wraps a controller operation with standard error handling and metrics.
   *
   * @param operation     The operation to execute
   * @param successMetric The metric to increment on success
   * @param failureMetric The metric to increment on failure
   * @param errorMessage  The error message to log on failure
   * @param meterRegistry The meter registry for recording metrics
   * @param <T>           The type of result
   * @return A ResponseEntity with the result or an error
   */
  public static <T> ResponseEntity<T> executeWithErrorHandling(
      Supplier<T> operation,
      String successMetric,
      String failureMetric,
      String errorMessage,
      MeterRegistry meterRegistry) {

    try {
      T result = operation.get();
      meterRegistry.counter(successMetric).increment();
      return ResponseEntity.ok(result);
    } catch (Exception e) {
      log.error(errorMessage, e);
      meterRegistry.counter(failureMetric).increment();
      return ResponseEntity.internalServerError().body(null);
    }
  }

  /**
   * Executes an operation with error handling, metrics, and a custom error response.
   * This method wraps a controller operation with standard error handling and metrics.
   *
   * @param operation             The operation to execute
   * @param successMetric         The metric to increment on success
   * @param failureMetric         The metric to increment on failure
   * @param errorMessage          The error message to log on failure
   * @param errorResponseSupplier A supplier for the error response
   * @param meterRegistry         The meter registry for recording metrics
   * @param <T>                   The type of result
   * @return A ResponseEntity with the result or an error
   */
  public static <T> ResponseEntity<T> executeWithErrorHandling(
      Supplier<T> operation,
      String successMetric,
      String failureMetric,
      String errorMessage,
      Supplier<T> errorResponseSupplier,
      MeterRegistry meterRegistry) {

    try {
      T result = operation.get();
      meterRegistry.counter(successMetric).increment();
      return ResponseEntity.ok(result);
    } catch (Exception e) {
      log.error(errorMessage, e);
      meterRegistry.counter(failureMetric).increment();
      return ResponseEntity.internalServerError().body(errorResponseSupplier.get());
    }
  }
}
