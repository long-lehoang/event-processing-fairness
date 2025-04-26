package dto

// WebhookEventDTO represents a webhook event
type WebhookEventDTO struct {
	EventID   string `json:"event_id"`
	EventType string `json:"event_type"`
	AccountID string `json:"account_id"`
}

// NewWebhookEventDTO creates a new WebhookEventDTO
func NewWebhookEventDTO(eventID, eventType, accountID string) *WebhookEventDTO {
	return &WebhookEventDTO{
		EventID:   eventID,
		EventType: eventType,
		AccountID: accountID,
	}
}
