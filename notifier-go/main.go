package main

import (
	"context"
	"log"
	"net/http"
	"os"
	"os/signal"
	"syscall"
	"time"

	"github.com/event-processing/notifier-go/api"
	"github.com/event-processing/notifier-go/application"
	"github.com/event-processing/notifier-go/client"
	"github.com/event-processing/notifier-go/config"
	"github.com/event-processing/notifier-go/database"
	"github.com/event-processing/notifier-go/domain/repository"
	"github.com/event-processing/notifier-go/kafka"
	"github.com/event-processing/notifier-go/monitoring"
	"github.com/event-processing/notifier-go/provider"
	"github.com/event-processing/notifier-go/provider/implementations"
	"github.com/event-processing/notifier-go/redis"
	"github.com/event-processing/notifier-go/service"
	webhookImpl "github.com/event-processing/notifier-go/service/impl"
	"github.com/gin-gonic/gin"
)

func main() {
	// Load configuration
	cfg, err := config.Load()
	if err != nil {
		log.Fatalf("Failed to load configuration: %v", err)
	}

	// Setup context with cancellation
	ctx, cancel := context.WithCancel(context.Background())
	defer cancel()

	// Initialize database connection
	db, err := database.NewConnection(cfg)
	if err != nil {
		log.Fatalf("Failed to initialize database connection: %v", err)
	}
	defer db.Close()

	// Initialize Redis client
	redisClient, err := redis.NewClient(cfg)
	if err != nil {
		log.Fatalf("Failed to initialize Redis client: %v", err)
	}
	defer redisClient.Close()

	// Initialize Kafka consumer
	consumer, err := kafka.NewConsumer(cfg)
	if err != nil {
		log.Fatalf("Failed to initialize Kafka consumer: %v", err)
	}
	defer consumer.Close()

	// Initialize Kafka producer
	producer, err := kafka.NewProducer(cfg)
	if err != nil {
		log.Fatalf("Failed to initialize Kafka producer: %v", err)
	}
	defer producer.Close()

	// Initialize DLQ producer
	dlqProducer, err := kafka.NewDLQProducer(cfg)
	if err != nil {
		log.Fatalf("Failed to initialize DLQ producer: %v", err)
	}
	defer dlqProducer.Close()

	// Initialize metrics
	metrics := monitoring.NewMetrics()

	// Initialize repositories
	subscriberEventRepo := repository.NewSubscriberCreatedEventRepository(db)
	segmentRepo := repository.NewSegmentRepository(db)

	// Initialize providers
	subscriberProvider := implementations.NewSubscriberEventProvider(subscriberEventRepo, segmentRepo)

	// Initialize WebhookEventService
	webhookEventService := service.NewWebhookEventService([]provider.WebhookEventProvider{subscriberProvider})

	// Initialize Redis services
	deduplicationService := redis.NewDeduplicationService(redisClient)
	rateLimiterService := redis.NewRateLimiterService(redisClient, cfg)

	// Initialize webhook client and service
	webhookClient := client.NewWebhookClient()
	webhookService := webhookImpl.NewWebhookService(webhookClient, dlqProducer, metrics, cfg)

	// Initialize fairness-aware event processing
	eventProcessing := application.NewWebhookEventFairnessProcessing(
		deduplicationService,
		webhookService,
		producer,
		cfg.Kafka.Topics.WebhookEvent.Name,
		metrics,
	)

	// Wire dependencies into Kafka consumer
	consumer.SetEventProcessing(eventProcessing)
	consumer.SetRateLimiter(rateLimiterService)
	consumer.SetEventService(webhookEventService)
	consumer.SetMetrics(metrics)
	consumer.SetEventProducer(producer)

	// Setup Gin router
	router := gin.Default()

	// Register API routes
	api.RegisterRoutes(router, producer)

	// Start HTTP server
	server := &http.Server{
		Addr:    ":" + cfg.Server.Port,
		Handler: router,
	}

	// Start server in a goroutine
	go func() {
		log.Printf("Starting server on port %s", cfg.Server.Port)
		if err := server.ListenAndServe(); err != nil && err != http.ErrServerClosed {
			log.Fatalf("Failed to start server: %v", err)
		}
	}()

	// Start Kafka consumer in a goroutine
	go func() {
		log.Println("Starting Kafka consumer")
		if err := consumer.Start(ctx); err != nil {
			log.Fatalf("Failed to start Kafka consumer: %v", err)
		}
	}()

	// Wait for interrupt signal
	quit := make(chan os.Signal, 1)
	signal.Notify(quit, syscall.SIGINT, syscall.SIGTERM)
	<-quit
	log.Println("Shutting down server...")

	// Create a deadline for graceful shutdown
	ctx, cancel = context.WithTimeout(context.Background(), 5*time.Second)
	defer cancel()

	// Shutdown the server
	if err := server.Shutdown(ctx); err != nil {
		log.Fatalf("Server forced to shutdown: %v", err)
	}

	log.Println("Server exiting")
}
