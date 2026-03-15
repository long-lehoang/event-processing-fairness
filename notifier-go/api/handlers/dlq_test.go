package handlers

import (
	"bytes"
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"testing"

	"github.com/gin-gonic/gin"
	"github.com/stretchr/testify/assert"
)

func setupDLQRouter() *gin.Engine {
	gin.SetMode(gin.TestMode)
	router := gin.New()
	router.GET("/api/dlq/events", GetDLQEvents)
	router.POST("/api/dlq/replay", ReplayDLQEvents)
	return router
}

func TestGetDLQEvents_ReturnsEmptyArray(t *testing.T) {
	router := setupDLQRouter()

	req := httptest.NewRequest("GET", "/api/dlq/events", nil)
	w := httptest.NewRecorder()
	router.ServeHTTP(w, req)

	assert.Equal(t, http.StatusOK, w.Code)
	var response map[string]interface{}
	json.Unmarshal(w.Body.Bytes(), &response)
	events, ok := response["events"].([]interface{})
	assert.True(t, ok)
	assert.Empty(t, events)
}

func TestReplayDLQEvents_Success(t *testing.T) {
	router := setupDLQRouter()

	body := `{"event_ids": ["id1", "id2"]}`
	req := httptest.NewRequest("POST", "/api/dlq/replay", bytes.NewBufferString(body))
	req.Header.Set("Content-Type", "application/json")
	w := httptest.NewRecorder()
	router.ServeHTTP(w, req)

	assert.Equal(t, http.StatusAccepted, w.Code)
	var response map[string]interface{}
	json.Unmarshal(w.Body.Bytes(), &response)
	assert.Equal(t, float64(2), response["count"])
}

func TestReplayDLQEvents_InvalidJSON(t *testing.T) {
	router := setupDLQRouter()

	req := httptest.NewRequest("POST", "/api/dlq/replay", bytes.NewBufferString("invalid"))
	req.Header.Set("Content-Type", "application/json")
	w := httptest.NewRecorder()
	router.ServeHTTP(w, req)

	assert.Equal(t, http.StatusBadRequest, w.Code)
}

func TestReplayDLQEvents_EmptyEventIDs(t *testing.T) {
	router := setupDLQRouter()

	body := `{"event_ids": []}`
	req := httptest.NewRequest("POST", "/api/dlq/replay", bytes.NewBufferString(body))
	req.Header.Set("Content-Type", "application/json")
	w := httptest.NewRecorder()
	router.ServeHTTP(w, req)

	assert.Equal(t, http.StatusBadRequest, w.Code)
}
