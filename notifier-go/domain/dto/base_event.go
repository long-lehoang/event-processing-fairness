package dto

// BaseEventDTO is the base interface for all event DTOs
// This serves as a marker interface similar to the Java implementation
type BaseEventDTO interface {
	// IsBaseEvent is a marker method to identify BaseEventDTO implementations
	IsBaseEvent() bool
}
