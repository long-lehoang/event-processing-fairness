package handlers

import (
	"context"
	"net/http"

	"github.com/event-processing/notifier-go/domain/dto"
	"github.com/gin-gonic/gin"
)

// EventPublisher defines the interface for publishing events to Kafka
type EventPublisher interface {
	Publish(ctx context.Context, topic, key string, payload *dto.WebhookEventDTO) error
}

// WebhookHandler handles webhook-related HTTP requests
type WebhookHandler struct {
	producer EventPublisher
}

// NewWebhookHandler creates a new WebhookHandler
func NewWebhookHandler(producer EventPublisher) *WebhookHandler {
	return &WebhookHandler{
		producer: producer,
	}
}

// CreateWebhookEvent handles the creation of a new webhook event
func (h *WebhookHandler) CreateWebhookEvent(c *gin.Context) {
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

	// Publish event to Kafka
	if err := h.producer.Publish(c.Request.Context(), "", event.AccountID, &event); err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"error": "Failed to publish event",
		})
		return
	}

	c.JSON(http.StatusAccepted, gin.H{
		"message":  "Event accepted for processing",
		"event_id": event.EventID,
	})
}
