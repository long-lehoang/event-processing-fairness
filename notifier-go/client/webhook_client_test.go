package client

import (
	"context"
	"encoding/json"
	"io"
	"net/http"
	"net/http/httptest"
	"testing"
	"time"

	"github.com/event-processing/notifier-go/domain/dto"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

func testSubscriberPayload() *dto.SubscriberEventDTO {
	return &dto.SubscriberEventDTO{
		ID:        "event-1",
		EventName: "subscriber.created",
		EventTime: "2023-01-01T00:00:00Z",
		Subscriber: dto.SubscriberDTO{
			ID:    "sub-1",
			Email: "test@example.com",
		},
		WebhookID: "webhook-1",
	}
}

func TestSendWebhook_HTTP200_ReturnsTrue(t *testing.T) {
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusOK)
	}))
	defer server.Close()

	client := NewWebhookClient()
	success, err := client.SendWebhook(context.Background(), server.URL, testSubscriberPayload())

	assert.True(t, success)
	assert.NoError(t, err)
}

func TestSendWebhook_HTTP202_ReturnsTrue(t *testing.T) {
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusAccepted)
	}))
	defer server.Close()

	client := NewWebhookClient()
	success, err := client.SendWebhook(context.Background(), server.URL, testSubscriberPayload())

	assert.True(t, success)
	assert.NoError(t, err)
}

func TestSendWebhook_HTTP500_ReturnsFalse(t *testing.T) {
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusInternalServerError)
	}))
	defer server.Close()

	client := NewWebhookClient()
	success, err := client.SendWebhook(context.Background(), server.URL, testSubscriberPayload())

	assert.False(t, success)
	assert.NoError(t, err)
}

func TestSendWebhook_HTTP400_ReturnsFalse(t *testing.T) {
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusBadRequest)
	}))
	defer server.Close()

	client := NewWebhookClient()
	success, err := client.SendWebhook(context.Background(), server.URL, testSubscriberPayload())

	assert.False(t, success)
	assert.NoError(t, err)
}

func TestSendWebhook_ConnectionRefused_ReturnsFalseWithError(t *testing.T) {
	// Use a server that's already closed
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {}))
	server.Close()

	client := NewWebhookClient()
	success, err := client.SendWebhook(context.Background(), server.URL, testSubscriberPayload())

	assert.False(t, success)
	assert.Error(t, err)
}

func TestSendWebhook_Timeout_ReturnsFalseWithError(t *testing.T) {
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		time.Sleep(200 * time.Millisecond)
		w.WriteHeader(http.StatusOK)
	}))
	defer server.Close()

	// Create client with very short timeout
	client := &WebhookRestClient{
		httpClient: &http.Client{Timeout: 50 * time.Millisecond},
	}
	success, err := client.SendWebhook(context.Background(), server.URL, testSubscriberPayload())

	assert.False(t, success)
	assert.Error(t, err)
}

func TestSendWebhook_PayloadIsSentAsJSON(t *testing.T) {
	var receivedBody []byte
	var receivedContentType string
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		receivedContentType = r.Header.Get("Content-Type")
		receivedBody, _ = io.ReadAll(r.Body)
		w.WriteHeader(http.StatusOK)
	}))
	defer server.Close()

	payload := testSubscriberPayload()
	client := NewWebhookClient()
	client.SendWebhook(context.Background(), server.URL, payload)

	assert.Equal(t, "application/json", receivedContentType)

	// Verify body is valid JSON
	var parsed map[string]interface{}
	err := json.Unmarshal(receivedBody, &parsed)
	require.NoError(t, err)
	assert.Equal(t, "subscriber.created", parsed["event_name"])
}

func TestSendWebhook_UsesHTTPPost(t *testing.T) {
	var receivedMethod string
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		receivedMethod = r.Method
		w.WriteHeader(http.StatusOK)
	}))
	defer server.Close()

	client := NewWebhookClient()
	client.SendWebhook(context.Background(), server.URL, testSubscriberPayload())

	assert.Equal(t, "POST", receivedMethod)
}

func TestSendWebhook_InvalidURL_ReturnsFalseWithError(t *testing.T) {
	client := NewWebhookClient()
	success, err := client.SendWebhook(context.Background(), "://invalid-url", testSubscriberPayload())

	assert.False(t, success)
	assert.Error(t, err)
}
