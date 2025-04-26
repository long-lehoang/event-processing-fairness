package dto

import (
	"time"
)

// SubscriberDTO represents a subscriber
type SubscriberDTO struct {
	ID        string `json:"id"`
	Email     string `json:"email"`
	Name      string `json:"name"`
	AccountID string `json:"account_id"`
}

// SubscriberEventDTO represents an event related to a subscriber
type SubscriberEventDTO struct {
	ID        string        `json:"-"`
	EventName string        `json:"event_name"`
	EventTime string        `json:"event_time"`
	Subscriber SubscriberDTO `json:"subscriber"`
	WebhookID string        `json:"webhook_id"`
}

// NewSubscriberEventDTO creates a new SubscriberEventDTO
func NewSubscriberEventDTO(id, eventName string, eventTime time.Time, subscriber SubscriberDTO, webhookID string) *SubscriberEventDTO {
	return &SubscriberEventDTO{
		ID:        id,
		EventName: eventName,
		EventTime: eventTime.Format(time.RFC3339),
		Subscriber: subscriber,
		WebhookID: webhookID,
	}
}

// IsBaseEvent implements the BaseEventDTO interface
func (s *SubscriberEventDTO) IsBaseEvent() bool {
	return true
}
