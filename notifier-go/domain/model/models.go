package model

import "time"

// SubscriberCreatedEvent represents the subscriber_created_event table
type SubscriberCreatedEvent struct {
	ID           string
	EventName    string
	EventTime    *time.Time
	SubscriberID string
	WebhookID    string
}

// Subscriber represents the subscriber table
type Subscriber struct {
	ID             string
	Email          string
	Status         string
	Source         string
	FirstName      string
	LastName       string
	CustomFields   string
	OptinIp        string
	OptinTimestamp *time.Time
	CreatedAt      *time.Time
	CreatedBy      string
}

// Webhook represents the webhook table
type Webhook struct {
	ID        string
	Name      string
	PostUrl   string
	CreatedAt *time.Time
	CreatedBy string
}

// Segment represents the segment table
type Segment struct {
	ID        string
	Name      string
	CreatedAt *time.Time
	CreatedBy string
}

// SubscriberSegment represents the subscriber_segment table
type SubscriberSegment struct {
	ID           string
	SegmentID    string
	SubscriberID string
	CreatedAt    *time.Time
	CreatedBy    string
}
