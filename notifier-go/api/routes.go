package api

import (
	"github.com/event-processing/notifier-go/api/handlers"
	"github.com/gin-gonic/gin"
	"github.com/prometheus/client_golang/prometheus/promhttp"
)

// RegisterRoutes registers all API routes
func RegisterRoutes(router *gin.Engine, producer handlers.EventPublisher) {
	// Health check endpoint
	router.GET("/health", handlers.HealthCheck)

	// Metrics endpoint
	router.GET("/metrics", gin.WrapH(promhttp.Handler()))

	// Create handlers with dependencies
	webhookHandler := handlers.NewWebhookHandler(producer)

	// API endpoints
	api := router.Group("/api")
	{
		// Webhook endpoints
		webhook := api.Group("/webhook")
		{
			webhook.POST("/event", webhookHandler.CreateWebhookEvent)
		}

		// DLQ endpoints
		dlq := api.Group("/dlq")
		{
			dlq.GET("/events", handlers.GetDLQEvents)
			dlq.POST("/replay", handlers.ReplayDLQEvents)
		}
	}
}
