# Event Processing Notifier Service

## Project Overview
This is a Spring Boot application for event processing and notification delivery, focusing on webhook events with fairness processing mechanisms.

## Package Structure

### Main Package
- `com.event.processing.notifier`: Root package for the application

### Sub-packages

#### Core Components
- `com.event.processing.notifier.consumer`: Kafka consumer implementations for processing webhook events
  - Contains `WebhookEventKafkaConsumer` and `EventConsumer` interfaces/classes
  
- `com.event.processing.notifier.producer`: Kafka producer components for sending processed events
  
- `com.event.processing.notifier.service`: Business logic services
  - `RateLimiterService`: Handles rate limiting of events
  - `DeduplicationService`: Manages event deduplication logic
  - `WebhookService`: Core webhook processing service
  - `impl`: Contains implementations of service interfaces

#### Domain Layer
- `com.event.processing.notifier.domain`: Core business entities and models
  - `entity`: JPA entities for database persistence
  - `repository`: Spring Data JPA repositories
  - `service`: Domain-specific services
  - `dto`: Data Transfer Objects for API communication

#### Infrastructure and Support
- `com.event.processing.notifier.config`: Application configuration classes
  - Likely contains Kafka, Redis, and database configurations
  
- `com.event.processing.notifier.util`: Utility classes and helper functions
  
- `com.event.processing.notifier.monitoring`: Metrics and monitoring components
  - Integrates with Spring Actuator and Prometheus

- `com.event.processing.notifier.client`: External service clients
  - Likely contains HTTP clients for webhook delivery

- `com.event.processing.notifier.application`: Application-level services
  - Orchestration and coordination of domain services

## Technical Stack
- Java 17
- Spring Boot 3.4.3
- Spring Cloud 2024.0.1
- Spring Data JPA for database access
- Spring Data Redis for caching and rate limiting
- Spring Kafka for event streaming
- Resilience4j for fault tolerance (circuit breakers, retries)
- Micrometer & Prometheus for metrics
- Lombok for boilerplate reduction
- PostgreSQL for persistent storage
- H2 for development/testing
- Maven for build management

## Configuration
The application is configured using:
- `application.yml` for main application settings
- `logback-spring.xml` for logging configuration

## Main Components
1. **Kafka Integration**:
   - Consumes from `webhook-events` topic
   - Provides dead-letter queue handling via `webhook-event-dead-letter-queue` topic
   - Configurable consumer group and concurrency settings

2. **Redis Integration**:
   - Used for rate limiting with configurable limits
   - Likely used for deduplication

3. **PostgreSQL Database**:
   - Stores event and webhook-related data
   - JPA with Hibernate ORM

4. **Resilience Patterns**:
   - Circuit breakers for webhook calls
   - Retry mechanisms with exponential backoff
   - Thread pool configurations for performance optimization

## Monitoring
- Spring Actuator endpoints for health checks and metrics
- Prometheus integration for metric collection 