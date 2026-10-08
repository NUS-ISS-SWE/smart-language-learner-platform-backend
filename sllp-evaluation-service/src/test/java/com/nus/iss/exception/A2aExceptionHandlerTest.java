package com.nus.iss.exception;

import com.nus.iss.dto.A2aResponseDTO;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class A2aExceptionHandlerTest {

    private final A2aExceptionHandler handler = new A2aExceptionHandler();

    @Test
    void unauthorizedReturns401WithoutBody() {
        ResponseEntity<Void> response = handler.unauthorized();

        assertEquals(401, response.getStatusCode().value());
        assertNull(response.getBody());
    }

    @Test
    void invalidRequestPreservesRequestIdAndReturnsJsonRpcError() {
        IllegalArgumentException cause = new IllegalArgumentException("bad payload");
        A2aInvalidRequestException error = new A2aInvalidRequestException(
                "request-1", "invalid", cause);

        ResponseEntity<A2aResponseDTO> response = handler.invalidRequest(error);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("request-1", response.getBody().getId());
        assertEquals(-32602, response.getBody().getError().getCode());
        assertSame(cause, error.getCause());
    }

    @Test
    void unreadableJsonGetsGeneratedRequestId() {
        HttpMessageNotReadableException error = org.mockito.Mockito.mock(HttpMessageNotReadableException.class);

        ResponseEntity<A2aResponseDTO> response = handler.invalidRequest(error);

        assertNotNull(response.getBody().getId());
        assertTrue(!response.getBody().getId().isBlank());
        assertEquals("invalid A2A grading request", response.getBody().getError().getMessage());
    }

    @Test
    void executionFailuresReturn500WithAndWithoutCause() {
        A2aExecutionException withoutCause = new A2aExecutionException("request-1", "failed");
        A2aExecutionException withCause = new A2aExecutionException(
                "request-2", "failed", new IllegalStateException("model unavailable"));

        ResponseEntity<A2aResponseDTO> first = handler.executionFailure(withoutCause);
        ResponseEntity<A2aResponseDTO> second = handler.executionFailure(withCause);

        assertEquals(500, first.getStatusCode().value());
        assertEquals("request-1", first.getBody().getId());
        assertEquals(500, second.getStatusCode().value());
        assertEquals("request-2", second.getBody().getId());
        assertEquals(-32603, second.getBody().getError().getCode());
    }

    @Test
    void unexpectedFailureReturnsSanitized500() {
        ResponseEntity<A2aResponseDTO> response =
                handler.unexpectedFailure(new RuntimeException("sensitive detail"));

        assertEquals(500, response.getStatusCode().value());
        assertNotNull(response.getBody().getId());
        assertEquals("grading execution failed", response.getBody().getError().getMessage());
    }
}
