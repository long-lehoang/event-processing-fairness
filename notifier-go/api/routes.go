package api

import (
	"github.com/event-processing/notifier-go/api/handlers"
	"github.com/event-processing/notifier-go/monitoring"
	"github.com/gin-gonic/gin"
	"github.com/prometheus/client_golang/prometheus/promhttp"
)

// RegisterRoutes registers all API routes
func RegisterRoutes(router *gin.Engine, metrics *monitoring.Metrics) {
	// Health check endpoint
	router.GET("/health", handlers.HealthCheck)
	
	// Metrics endpoint
	router.GET("/metrics", gin.WrapH(promhttp.Handler()))
	
	// API endpoints
	api := router.Group("/api")
	{
		// Webhook endpoints
		webhook := api.Group("/webhook")
		{
			webhook.POST("/event", handlers.CreateWebhookEvent)
		}
		
		// DLQ endpoints
		dlq := api.Group("/dlq")
		{
			dlq.GET("/events", handlers.GetDLQEvents)
			dlq.POST("/replay", handlers.ReplayDLQEvents)
		}
	}
}
