package dto

import (
	"time"
)

// SubscriberDTO represents a subscriber
type SubscriberDTO struct {
	ID             string       `json:"id"`
	Status         string       `json:"status"`
	Email          string       `json:"email"`
	Source         string       `json:"source"`
	FirstName      string       `json:"first_name"`
	LastName       string       `json:"last_name"`
	Segments       []SegmentDTO `json:"segments"`
	CustomFields   string       `json:"custom_fields"`
	OptinIp        string       `json:"optin_ip"`
	OptinTimestamp string       `json:"optin_timestamp"`
	CreatedAt      string       `json:"created_at"`
}

// SubscriberEventDTO represents an event related to a subscriber
type SubscriberEventDTO struct {
	ID         string        `json:"-"`
	EventName  string        `json:"event_name"`
	EventTime  string        `json:"event_time"`
	Subscriber SubscriberDTO `json:"subscriber"`
	WebhookID  string        `json:"webhook_id"`
}

// NewSubscriberEventDTO creates a new SubscriberEventDTO
func NewSubscriberEventDTO(id, eventName string, eventTime time.Time, subscriber SubscriberDTO, webhookID string) *SubscriberEventDTO {
	return &SubscriberEventDTO{
		ID:         id,
		EventName:  eventName,
		EventTime:  eventTime.Format(time.RFC3339),
		Subscriber: subscriber,
		WebhookID:  webhookID,
	}
}

// IsBaseEvent implements the BaseEventDTO interface
func (s *SubscriberEventDTO) IsBaseEvent() bool {
	return true
}
