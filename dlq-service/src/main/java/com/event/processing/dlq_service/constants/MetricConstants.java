package com.event.processing.dlq_service.constants;

/**
 * Constants for metric names used throughout the application.
 * This class centralizes all metric names to ensure consistency and prevent typos.
 */
public final class MetricConstants {

  // Dead Letter Queue Event metrics
  public static final String DLQ_EVENTS_RECEIVED = "dlq.events.received";
  public static final String DLQ_EVENTS_CREATED = "dlq.events.created";
  public static final String DLQ_EVENTS_RETRIED = "dlq.events.retried";
  public static final String DLQ_EVENTS_RETRY_FAILED = "dlq.events.retry_failed";
  public static final String DLQ_EVENTS_MAX_RETRIES_EXCEEDED = "dlq.events.max_retries_exceeded";
  public static final String DLQ_EVENTS_PENDING_RETRIES = "dlq.events.pending_retries";
  // Batch processing metrics
  public static final String DLQ_EVENTS_BATCH_RECEIVED = "dlq.events.batch.received";
  public static final String DLQ_EVENTS_BATCH_SIZE = "dlq.events.batch.size";
  public static final String DLQ_EVENTS_BATCH_PROCESSED = "dlq.events.batch.processed";
  public static final String DLQ_EVENTS_BATCH_ERRORS = "dlq.events.batch.errors";
  public static final String DLQ_EVENTS_BATCH_PROCESSING_ERROR = "dlq.events.batch.processing_error";
  // Kafka message metrics
  public static final String DLQ_MESSAGES_BATCH_PROCESSED = "dlq.messages.batch.processed";
  public static final String DLQ_MESSAGES_BATCH_FAILED = "dlq.messages.batch.failed";
  public static final String DLQ_MESSAGES_BATCH_SIZE = "dlq.messages.batch.size";
  public static final String DLQ_MESSAGES_BATCH_PROCESSING_TIME = "dlq.messages.batch.processing_time";
  // Webhook event metrics
  public static final String WEBHOOK_EVENTS_PUBLISHED = "webhook.events.published";
  public static final String WEBHOOK_EVENTS_PUBLISH_FAILED = "webhook.events.publish_failed";
  // API metrics
  public static final String API_WEBHOOK_EVENTS_PUBLISHED = "webhook.events.api.published";
  public static final String API_WEBHOOK_EVENTS_PUBLISH_FAILED = "webhook.events.api.publish_failed";
  public static final String API_WEBHOOK_EVENTS_CREATED = "webhook.events.api.created";
  public static final String API_WEBHOOK_EVENTS_CREATE_FAILED = "webhook.events.api.create_failed";
  public static final String API_WEBHOOK_EVENTS_RETRIED = "webhook.events.api.retried";
  public static final String API_WEBHOOK_EVENTS_RETRY_FAILED = "webhook.events.api.retry_failed";
  public static final String API_WEBHOOK_EVENTS_PUBLISHED_TO_TOPIC = "webhook.events.api.published_to_topic";
  public static final String API_WEBHOOK_EVENTS_PUBLISH_TO_TOPIC_FAILED = "webhook.events.api.publish_to_topic_failed";

  private MetricConstants() {
    // Private constructor to prevent instantiation
  }
}
