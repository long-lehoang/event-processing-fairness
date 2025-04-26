package client

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"log"
	"net/http"
	"time"
	
	"github.com/event-processing/notifier-go/domain/dto"
	"github.com/event-processing/notifier-go/service"
)

// WebhookRestClient implements the WebhookClient interface
type WebhookRestClient struct {
	httpClient *http.Client
}

// NewWebhookClient creates a new WebhookRestClient
func NewWebhookClient() *WebhookRestClient {
	return &WebhookRestClient{
		httpClient: &http.Client{
			Timeout: 10 * time.Second,
		},
	}
}

// SendWebhook sends a webhook notification using HTTP POST
func (c *WebhookRestClient) SendWebhook(ctx context.Context, webhookURL string, payload dto.BaseEventDTO) (bool, error) {
	log.Printf("Sending webhook event: url %s payload %+v", webhookURL, payload)
	
	// Convert payload to JSON
	jsonPayload, err := json.Marshal(payload)
	if err != nil {
		log.Printf("Failed to marshal payload: %v", err)
		return false, fmt.Errorf("failed to marshal payload: %w", err)
	}
	
	// Create request
	req, err := http.NewRequestWithContext(ctx, http.MethodPost, webhookURL, bytes.NewBuffer(jsonPayload))
	if err != nil {
		log.Printf("Failed to create request: %v", err)
		return false, fmt.Errorf("failed to create request: %w", err)
	}
	
	// Set headers
	req.Header.Set("Content-Type", "application/json")
	
	// Send request
	resp, err := c.httpClient.Do(req)
	if err != nil {
		log.Printf("Webhook failed: %s, Error: %v", webhookURL, err)
		return false, err
	}
	defer resp.Body.Close()
	
	// Check response status
	success := resp.StatusCode >= 200 && resp.StatusCode < 300
	if success {
		log.Printf("Webhook success: %s, Response status: %d", webhookURL, resp.StatusCode)
	} else {
		log.Printf("Webhook failed: %s, Response status: %d", webhookURL, resp.StatusCode)
	}
	
	return success, nil
}

// Ensure WebhookRestClient implements WebhookClient interface
var _ service.WebhookClient = (*WebhookRestClient)(nil)
