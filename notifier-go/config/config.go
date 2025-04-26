package config

import (
	"fmt"
	"time"

	"github.com/spf13/viper"
)

// Config holds all configuration for the application
type Config struct {
	Server     ServerConfig
	Kafka      KafkaConfig
	Redis      RedisConfig
	Database   DatabaseConfig
	Resilience ResilienceConfig
	ThreadPool ThreadPoolConfig
}

// ServerConfig holds server-related configuration
type ServerConfig struct {
	Port    string
	Address string
}

// KafkaConfig holds Kafka-related configuration
type KafkaConfig struct {
	BootstrapServers string
	Topics           KafkaTopics
	Consumer         KafkaConsumer
	Producer         KafkaProducer
}

// KafkaTopics holds Kafka topic configuration
type KafkaTopics struct {
	WebhookEvent struct {
		Name             string
		Partitions       int
		ReplicationFactor int
	}
	DeadLetterQueue struct {
		Name             string
		Partitions       int
		ReplicationFactor int
	}
}

// KafkaConsumer holds Kafka consumer configuration
type KafkaConsumer struct {
	GroupID         string
	AutoOffsetReset string
	PollTimeout     time.Duration
	MaxPollRecords  int
	Concurrency     int
}

// KafkaProducer holds Kafka producer configuration
type KafkaProducer struct {
	Retries             int
	Acks                string
	DeliveryTimeoutMs   int
	RequestTimeoutMs    int
	RetryBackoffMs      int
}

// RedisConfig holds Redis-related configuration
type RedisConfig struct {
	Host  string
	Port  int
	Limit struct {
		Event int
		Time  int
	}
}

// DatabaseConfig holds database-related configuration
type DatabaseConfig struct {
	Host     string
	Port     int
	Username string
	Password string
	Database string
}

// ResilienceConfig holds resilience-related configuration
type ResilienceConfig struct {
	Retry struct {
		WebhookRetry struct {
			MaxAttempts               int
			WaitDuration              time.Duration
			ExponentialBackoffMultiplier float64
		}
	}
	CircuitBreaker struct {
		WebhookCircuitBreaker struct {
			FailureRateThreshold          float64
			SlowCallRateThreshold         float64
			SlowCallDurationThreshold     time.Duration
			WaitDurationInOpenState       time.Duration
			PermittedNumberOfCallsInHalfOpenState int
			MinimumNumberOfCalls          int
		}
	}
}

// ThreadPoolConfig holds thread pool configuration
type ThreadPoolConfig struct {
	KafkaConsumer struct {
		CoreSize      int
		MaxSize       int
		QueueCapacity int
	}
}

// Load loads the configuration from environment variables and config file
func Load() (*Config, error) {
	viper.SetConfigName("config")
	viper.SetConfigType("yaml")
	viper.AddConfigPath(".")
	viper.AddConfigPath("./config")
	viper.AutomaticEnv()

	// Set defaults
	setDefaults()

	// Read the config file
	if err := viper.ReadInConfig(); err != nil {
		// It's okay if config file doesn't exist
		if _, ok := err.(viper.ConfigFileNotFoundError); !ok {
			return nil, fmt.Errorf("error reading config file: %w", err)
		}
	}

	var config Config
	if err := viper.Unmarshal(&config); err != nil {
		return nil, fmt.Errorf("unable to decode config into struct: %w", err)
	}

	return &config, nil
}

// setDefaults sets default values for configuration
func setDefaults() {
	// Server defaults
	viper.SetDefault("server.port", "8080")
	viper.SetDefault("server.address", "0.0.0.0")

	// Kafka defaults
	viper.SetDefault("kafka.bootstrapServers", "localhost:9092")
	viper.SetDefault("kafka.topics.webhookEvent.name", "webhook-events")
	viper.SetDefault("kafka.topics.webhookEvent.partitions", 3)
	viper.SetDefault("kafka.topics.webhookEvent.replicationFactor", 1)
	viper.SetDefault("kafka.topics.deadLetterQueue.name", "webhook-event-dead-letter-queue")
	viper.SetDefault("kafka.topics.deadLetterQueue.partitions", 3)
	viper.SetDefault("kafka.topics.deadLetterQueue.replicationFactor", 1)
	viper.SetDefault("kafka.consumer.groupID", "event-processing-group")
	viper.SetDefault("kafka.consumer.autoOffsetReset", "earliest")
	viper.SetDefault("kafka.consumer.pollTimeout", 3000)
	viper.SetDefault("kafka.consumer.maxPollRecords", 100)
	viper.SetDefault("kafka.consumer.concurrency", 1)
	viper.SetDefault("kafka.producer.retries", 5)
	viper.SetDefault("kafka.producer.acks", "all")
	viper.SetDefault("kafka.producer.deliveryTimeoutMs", 30000)
	viper.SetDefault("kafka.producer.requestTimeoutMs", 5000)
	viper.SetDefault("kafka.producer.retryBackoffMs", 500)

	// Redis defaults
	viper.SetDefault("redis.host", "localhost")
	viper.SetDefault("redis.port", 6379)
	viper.SetDefault("redis.limit.event", 400)
	viper.SetDefault("redis.limit.time", 1)

	// Database defaults
	viper.SetDefault("database.host", "localhost")
	viper.SetDefault("database.port", 5432)
	viper.SetDefault("database.username", "postgres")
	viper.SetDefault("database.password", "postgres")
	viper.SetDefault("database.database", "webhook")

	// Resilience defaults
	viper.SetDefault("resilience.retry.webhookRetry.maxAttempts", 5)
	viper.SetDefault("resilience.retry.webhookRetry.waitDuration", "2s")
	viper.SetDefault("resilience.retry.webhookRetry.exponentialBackoffMultiplier", 2.0)
	viper.SetDefault("resilience.circuitBreaker.webhookCircuitBreaker.failureRateThreshold", 50.0)
	viper.SetDefault("resilience.circuitBreaker.webhookCircuitBreaker.slowCallRateThreshold", 60.0)
	viper.SetDefault("resilience.circuitBreaker.webhookCircuitBreaker.slowCallDurationThreshold", "2s")
	viper.SetDefault("resilience.circuitBreaker.webhookCircuitBreaker.waitDurationInOpenState", "10s")
	viper.SetDefault("resilience.circuitBreaker.webhookCircuitBreaker.permittedNumberOfCallsInHalfOpenState", 3)
	viper.SetDefault("resilience.circuitBreaker.webhookCircuitBreaker.minimumNumberOfCalls", 5)

	// Thread pool defaults
	viper.SetDefault("threadPool.kafkaConsumer.coreSize", 10)
	viper.SetDefault("threadPool.kafkaConsumer.maxSize", 50)
	viper.SetDefault("threadPool.kafkaConsumer.queueCapacity", 100)
}
