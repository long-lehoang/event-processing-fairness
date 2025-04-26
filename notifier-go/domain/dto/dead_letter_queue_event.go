package dto

// DeadLetterQueueEventDTO represents an event that has been sent to the Dead Letter Queue
type DeadLetterQueueEventDTO struct {
	EventID         string `json:"event_id"`
	AccountID       string `json:"account_id"`
	EventType       string `json:"event_type"`
	LastErrorMessage string `json:"last_error_message"`
	FailureReason   string `json:"failure_reason"`
}

// NewDeadLetterQueueEventDTO creates a new DeadLetterQueueEventDTO
func NewDeadLetterQueueEventDTO(eventID, accountID, eventType, lastErrorMessage, failureReason string) *DeadLetterQueueEventDTO {
	return &DeadLetterQueueEventDTO{
		EventID:         eventID,
		AccountID:       accountID,
		EventType:       eventType,
		LastErrorMessage: lastErrorMessage,
		FailureReason:   failureReason,
	}
}

// FromWebhookEvent creates a DeadLetterQueueEventDTO from a WebhookEventDTO
func FromWebhookEvent(event *WebhookEventDTO, errorMessage, failureReason string) *DeadLetterQueueEventDTO {
	return &DeadLetterQueueEventDTO{
		EventID:         event.EventID,
		AccountID:       event.AccountID,
		EventType:       event.EventType,
		LastErrorMessage: errorMessage,
		FailureReason:   failureReason,
	}
}
