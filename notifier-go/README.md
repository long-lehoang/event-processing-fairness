# Event Processing Notifier Service (Go)

## Project Overview
This is a Gin-based Go application for event processing and notification delivery, focusing on webhook events with fairness processing mechanisms. This is a Go port of the original Java Spring Boot notifier service.

## Package Structure

### Main Components
- `main.go`: Application entry point
- `config`: Configuration loading and management
- `domain`: Core business entities and models
- `kafka`: Kafka consumer and producer implementations
- `redis`: Redis client, rate limiting, and deduplication
- `service`: Business logic services
- `application`: Application-level services
- `client`: External service clients
- `provider`: Event provider implementations
- `monitoring`: Metrics and monitoring
- `util`: Utility functions and constants
- `api`: API routes and handlers

## Technical Stack
- Go 1.21+
- Gin Web Framework
- Kafka-go for event streaming
- Go-Redis for caching and rate limiting
- Sony/gobreaker for circuit breaking
- Cenkalti/backoff for retry mechanisms
- Prometheus client for metrics
- Viper for configuration management

## Configuration
The application is configured using environment variables and/or a configuration file (config.yaml).

## Main Components
1. **Kafka Integration**:
   - Consumes from `webhook-events` topic
   - Provides dead-letter queue handling via `webhook-event-dead-letter-queue` topic
   - Configurable consumer group and concurrency settings

2. **Redis Integration**:
   - Used for rate limiting with configurable limits
   - Used for deduplication

3. **PostgreSQL Database**:
   - Stores event and webhook-related data

4. **Resilience Patterns**:
   - Circuit breakers for webhook calls
   - Retry mechanisms with exponential backoff
   - Concurrency control for performance optimization

## Monitoring
- Prometheus metrics for monitoring
- Health check endpoints

## Running the Application
```bash
# Build the application
go build -o notifier-go

# Run the application
./notifier-go
```

## Docker Support
```bash
# Build Docker image
docker build -t notifier-go:latest .

# Run Docker container
docker run -p 8080:8080 notifier-go:latest
```
