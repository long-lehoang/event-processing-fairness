package com.event.processing.notifier.client;

import com.event.processing.notifier.domain.dto.BaseEventDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebhookRestClientTest {

    @Mock
    private RestClient restClient;

    @Mock
    private RestClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private RestClient.RequestBodySpec requestBodySpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    private WebhookRestClient webhookClient;

    private static final String WEBHOOK_URL = "http://test-url.com";
    private static final String SUCCESS_RESPONSE = "Success";
    private static final String ERROR_MESSAGE = "Connection refused";

    @BeforeEach
    void setUp() {
        webhookClient = new WebhookRestClient(restClient);
    }

    @Test
    void sendWebhook_WhenSuccessful_ShouldReturnTrue() {
        // Arrange
        BaseEventDTO payload = new BaseEventDTO();
        ResponseEntity<String> response = new ResponseEntity<>(SUCCESS_RESPONSE, HttpStatus.OK);

        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(BaseEventDTO.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toEntity(String.class)).thenReturn(response);

        // Act
        boolean result = webhookClient.sendWebhook(WEBHOOK_URL, payload);

        // Assert
        assertTrue(result);
        verify(restClient).post();
        verify(requestBodyUriSpec).uri(WEBHOOK_URL);
        verify(requestBodySpec).body(payload);
    }

    @Test
    void sendWebhook_WhenServerError_ShouldReturnFalse() {
        // Arrange
        BaseEventDTO payload = new BaseEventDTO();
        ResponseEntity<String> response = new ResponseEntity<>(SUCCESS_RESPONSE, HttpStatus.INTERNAL_SERVER_ERROR);

        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(BaseEventDTO.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toEntity(String.class)).thenReturn(response);

        // Act
        boolean result = webhookClient.sendWebhook(WEBHOOK_URL, payload);

        // Assert
        assertFalse(result);
        verify(restClient).post();
        verify(requestBodyUriSpec).uri(WEBHOOK_URL);
        verify(requestBodySpec).body(payload);
    }

    @Test
    void sendWebhook_WhenExceptionOccurs_ShouldThrowException() {
        // Arrange
        BaseEventDTO payload = new BaseEventDTO();

        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(BaseEventDTO.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toEntity(String.class)).thenThrow(new RuntimeException(ERROR_MESSAGE));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> webhookClient.sendWebhook(WEBHOOK_URL, payload));
        assertEquals(ERROR_MESSAGE, exception.getMessage());
        verify(restClient).post();
        verify(requestBodyUriSpec).uri(WEBHOOK_URL);
        verify(requestBodySpec).body(payload);
    }
}