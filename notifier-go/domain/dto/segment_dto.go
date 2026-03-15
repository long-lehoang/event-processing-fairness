package dto

// SegmentDTO represents a subscriber segment
type SegmentDTO struct {
	ID           string `json:"id"`
	Name         string `json:"name"`
	SubscriberID string `json:"-"`
}
