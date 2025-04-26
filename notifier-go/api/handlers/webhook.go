package handlers

import (
	"net/http"
	
	"github.com/event-processing/notifier-go/domain/dto"
	"github.com/gin-gonic/gin"
)

// CreateWebhookEvent handles the creation of a new webhook event
func CreateWebhookEvent(c *gin.Context) {
	var event dto.WebhookEventDTO
	
	if err := c.ShouldBindJSON(&event); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{
			"error": "Invalid request payload",
		})
		return
	}
	
	// Validate required fields
	if event.EventID == "" || event.EventType == "" || event.AccountID == "" {
		c.JSON(http.StatusBadRequest, gin.H{
			"error": "Missing required fields",
		})
		return
	}
	
	// TODO: Publish event to Kafka
	// This would be implemented by injecting the Kafka producer
	
	c.JSON(http.StatusAccepted, gin.H{
		"message": "Event accepted for processing",
		"event_id": event.EventID,
	})
}
