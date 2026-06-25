package com.nus.iss.exception;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @Mock
    private MethodArgumentNotValidException methodArgumentNotValidException;

    @Mock
    private BindingResult bindingResult;

    private final GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler();

    @Test
    void handleUserAlreadyExistsExceptionReturnsConflictProblemDetail() {
        ProblemDetail problemDetail = exceptionHandler.handleUserAlreadyExistsException(
                new UserAlreadyExistsException("duplicate")
        );

        assertEquals(HttpStatus.CONFLICT.value(), problemDetail.getStatus());
        assertEquals("Account Conflict", problemDetail.getTitle());
        assertEquals("Unable to complete registration.", problemDetail.getDetail());
    }

    @Test
    void handleBadCredentialsExceptionReturnsUnauthorizedProblemDetail() {
        ProblemDetail problemDetail = exceptionHandler.handleBadCredentialsException(
                new BadCredentialsException("bad credentials")
        );

        assertEquals(HttpStatus.UNAUTHORIZED.value(), problemDetail.getStatus());
        assertEquals("Authentication Failed", problemDetail.getTitle());
        assertEquals("Invalid credentials or request.", problemDetail.getDetail());
    }

    @Test
    void handleValidationExceptionReturnsBadRequestProblemDetailWithDistinctInvalidFields() {
        when(methodArgumentNotValidException.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(
                new FieldError("appUserDTO", "email", "Email should be valid"),
                new FieldError("appUserDTO", "email", "Email cannot be blank"),
                new FieldError("appUserDTO", "password", "Password cannot be blank")
        ));

        ProblemDetail problemDetail = exceptionHandler.handleValidationException(methodArgumentNotValidException);

        assertEquals(HttpStatus.BAD_REQUEST.value(), problemDetail.getStatus());
        assertEquals("Validation Failed", problemDetail.getTitle());
        assertEquals("One or more request fields are invalid.", problemDetail.getDetail());
        assertEquals(List.of("email", "password"), problemDetail.getProperties().get("invalidFields"));
    }

    @Test
    void handleGeneralExceptionReturnsInternalServerErrorProblemDetail() {
        ProblemDetail problemDetail = exceptionHandler.handleGeneralException(new RuntimeException("unexpected"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), problemDetail.getStatus());
        assertEquals("Internal Server Error", problemDetail.getTitle());
        assertEquals("An unexpected error occurred.", problemDetail.getDetail());
    }
}
