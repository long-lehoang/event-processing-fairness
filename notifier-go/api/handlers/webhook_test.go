package handlers

import (
	"bytes"
	"context"
	"encoding/json"
	"errors"
	"net/http"
	"net/http/httptest"
	"testing"

	"github.com/event-processing/notifier-go/domain/dto"
	"github.com/gin-gonic/gin"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/mock"
)

// --- Mock ---

type mockEventPublisher struct {
	mock.Mock
}

func (m *mockEventPublisher) Publish(ctx context.Context, topic, key string, payload *dto.WebhookEventDTO) error {
	args := m.Called(ctx, topic, key, payload)
	return args.Error(0)
}

// --- Helpers ---

func setupTestRouter(publisher *mockEventPublisher) *gin.Engine {
	gin.SetMode(gin.TestMode)
	router := gin.New()
	handler := NewWebhookHandler(publisher)
	router.POST("/api/webhook/event", handler.CreateWebhookEvent)
	return router
}

func performRequest(router *gin.Engine, method, path string, body interface{}) *httptest.ResponseRecorder {
	var reqBody *bytes.Buffer
	if body != nil {
		jsonBytes, _ := json.Marshal(body)
		reqBody = bytes.NewBuffer(jsonBytes)
	} else {
		reqBody = bytes.NewBuffer(nil)
	}
	req := httptest.NewRequest(method, path, reqBody)
	req.Header.Set("Content-Type", "application/json")
	w := httptest.NewRecorder()
	router.ServeHTTP(w, req)
	return w
}

// --- Tests ---

func TestCreateWebhookEvent_Success(t *testing.T) {
	publisher := new(mockEventPublisher)
	router := setupTestRouter(publisher)

	event := dto.WebhookEventDTO{
		EventID:   "event-1",
		EventType: "subscriber.created",
		AccountID: "account-1",
	}
	publisher.On("Publish", mock.Anything, "", "account-1", mock.AnythingOfType("*dto.WebhookEventDTO")).Return(nil)

	w := performRequest(router, "POST", "/api/webhook/event", event)

	assert.Equal(t, http.StatusAccepted, w.Code)
	var response map[string]interface{}
	json.Unmarshal(w.Body.Bytes(), &response)
	assert.Equal(t, "event-1", response["event_id"])
	assert.Equal(t, "Event accepted for processing", response["message"])
}

func TestCreateWebhookEvent_InvalidJSON(t *testing.T) {
	publisher := new(mockEventPublisher)
	router := setupTestRouter(publisher)

	req := httptest.NewRequest("POST", "/api/webhook/event", bytes.NewBufferString("invalid json"))
	req.Header.Set("Content-Type", "application/json")
	w := httptest.NewRecorder()
	router.ServeHTTP(w, req)

	assert.Equal(t, http.StatusBadRequest, w.Code)
	var response map[string]interface{}
	json.Unmarshal(w.Body.Bytes(), &response)
	assert.Equal(t, "Invalid request payload", response["error"])
}

func TestCreateWebhookEvent_MissingEventID(t *testing.T) {
	publisher := new(mockEventPublisher)
	router := setupTestRouter(publisher)

	event := map[string]string{
		"event_type": "subscriber.created",
		"account_id": "account-1",
	}
	w := performRequest(router, "POST", "/api/webhook/event", event)

	assert.Equal(t, http.StatusBadRequest, w.Code)
}

func TestCreateWebhookEvent_MissingEventType(t *testing.T) {
	publisher := new(mockEventPublisher)
	router := setupTestRouter(publisher)

	event := map[string]string{
		"event_id":   "event-1",
		"account_id": "account-1",
	}
	w := performRequest(router, "POST", "/api/webhook/event", event)

	assert.Equal(t, http.StatusBadRequest, w.Code)
}

func TestCreateWebhookEvent_MissingAccountID(t *testing.T) {
	publisher := new(mockEventPublisher)
	router := setupTestRouter(publisher)

	event := map[string]string{
		"event_id":   "event-1",
		"event_type": "subscriber.created",
	}
	w := performRequest(router, "POST", "/api/webhook/event", event)

	assert.Equal(t, http.StatusBadRequest, w.Code)
}

func TestCreateWebhookEvent_ProducerFailure(t *testing.T) {
	publisher := new(mockEventPublisher)
	router := setupTestRouter(publisher)

	event := dto.WebhookEventDTO{
		EventID:   "event-1",
		EventType: "subscriber.created",
		AccountID: "account-1",
	}
	publisher.On("Publish", mock.Anything, "", "account-1", mock.AnythingOfType("*dto.WebhookEventDTO")).Return(errors.New("kafka unavailable"))

	w := performRequest(router, "POST", "/api/webhook/event", event)

	assert.Equal(t, http.StatusInternalServerError, w.Code)
	var response map[string]interface{}
	json.Unmarshal(w.Body.Bytes(), &response)
	assert.Equal(t, "Failed to publish event", response["error"])
}
