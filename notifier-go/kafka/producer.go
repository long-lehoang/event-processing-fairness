package kafka

import (
	"context"
	"encoding/json"
	"fmt"
	"log"
	"time"
	
	"github.com/event-processing/notifier-go/config"
	"github.com/event-processing/notifier-go/domain/dto"
	"github.com/segmentio/kafka-go"
)

// Producer handles producing events to Kafka
type Producer struct {
	writer *kafka.Writer
	config *config.Config
}

// NewProducer creates a new Kafka producer
func NewProducer(cfg *config.Config) (*Producer, error) {
	writer := &kafka.Writer{
		Addr:         kafka.TCP(cfg.Kafka.BootstrapServers),
		Topic:        cfg.Kafka.Topics.WebhookEvent.Name,
		Balancer:     &kafka.Hash{},
		RequiredAcks: kafka.RequireAll,
		Async:        true,
	}
	
	return &Producer{
		writer: writer,
		config: cfg,
	}, nil
}

// Publish publishes a webhook event to Kafka
func (p *Producer) Publish(ctx context.Context, topic, key string, payload *dto.WebhookEventDTO) error {
	if topic == "" {
		topic = p.config.Kafka.Topics.WebhookEvent.Name
	}
	
	// Override the topic if needed
	if p.writer.Topic != topic {
		p.writer.Topic = topic
	}
	
	// Marshal the payload
	value, err := json.Marshal(payload)
	if err != nil {
		return fmt.Errorf("failed to marshal payload: %w", err)
	}
	
	// Create the message
	message := kafka.Message{
		Key:   []byte(key),
		Value: value,
		Time:  time.Now(),
	}
	
	// Send the message
	log.Printf("Publishing event to Kafka. Topic: %s, Key: %s", topic, key)
	if err := p.writer.WriteMessages(ctx, message); err != nil {
		log.Printf("Failed to publish event. Topic: %s, Key: %s, Error: %v", topic, key, err)
		return fmt.Errorf("failed to publish event: %w", err)
	}
	
	return nil
}

// Close closes the Kafka producer
func (p *Producer) Close() error {
	return p.writer.Close()
}

// DLQProducer handles producing events to the Dead Letter Queue
type DLQProducer struct {
	writer *kafka.Writer
	config *config.Config
}

// NewDLQProducer creates a new Dead Letter Queue producer
func NewDLQProducer(cfg *config.Config) (*DLQProducer, error) {
	writer := &kafka.Writer{
		Addr:         kafka.TCP(cfg.Kafka.BootstrapServers),
		Topic:        cfg.Kafka.Topics.DeadLetterQueue.Name,
		Balancer:     &kafka.Hash{},
		RequiredAcks: kafka.RequireAll,
		Async:        true,
	}
	
	return &DLQProducer{
		writer: writer,
		config: cfg,
	}, nil
}

// Publish publishes a dead letter queue event to Kafka
func (p *DLQProducer) Publish(ctx context.Context, topic, key string, payload *dto.DeadLetterQueueEventDTO) error {
	if topic == "" {
		topic = p.config.Kafka.Topics.DeadLetterQueue.Name
	}
	
	// Override the topic if needed
	if p.writer.Topic != topic {
		p.writer.Topic = topic
	}
	
	// Marshal the payload
	value, err := json.Marshal(payload)
	if err != nil {
		return fmt.Errorf("failed to marshal payload: %w", err)
	}
	
	// Create the message
	message := kafka.Message{
		Key:   []byte(key),
		Value: value,
		Time:  time.Now(),
	}
	
	// Send the message
	log.Printf("Publishing event to DLQ. Topic: %s, Key: %s", topic, key)
	if err := p.writer.WriteMessages(ctx, message); err != nil {
		log.Printf("Failed to publish event to DLQ. Topic: %s, Key: %s, Error: %v", topic, key, err)
		return fmt.Errorf("failed to publish event to DLQ: %w", err)
	}
	
	return nil
}

// Close closes the Dead Letter Queue producer
func (p *DLQProducer) Close() error {
	return p.writer.Close()
}
