package com.event.processing.notifier.util;

public class PromeTheusMetricContants {
  public static final String METRIC_KAFKA_BATCH_PROCESSING_TIME = "kafka.batch.processing.time";
  public static final String METRIC_KAFKA_EVENT_PROCESSING_TIME = "kafka.event.processing.time";
  public static final String WEBHOOK_EXECUTION_TIME = "webhook.execution.time";
  public static final String WEBHOOK_FAILURE_COUNT = "webhook.failure";
  public static final String CIRCUIT_BREAKER_OPEN_COUNT = "webhook.circuit.open";
  private PromeTheusMetricContants() {
    throw new IllegalStateException("Utility class");
  }
}
