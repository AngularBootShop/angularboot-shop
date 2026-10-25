package com.shop.order.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.shop.order.exception.IdempotencyConflictException;
import com.shop.order.exception.OrderProcessingException;
import com.shop.order.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MissingRequestHeaderException;

class GlobalExceptionHandlerTest {

  private GlobalExceptionHandler exceptionHandler;

  @BeforeEach
  void setUp() {
    exceptionHandler = new GlobalExceptionHandler();
  }

  @Test
  void handleIdempotencyConflict_payloadMismatch_shouldReturn422() {
    IdempotencyConflictException ex = IdempotencyConflictException.payloadMismatch();
    ProblemDetail problem = exceptionHandler.handleIdempotencyConflict(ex);

    assertThat(problem.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY.value());
    assertThat(problem.getTitle()).isEqualTo("Idempotency Conflict");
    assertThat(problem.getProperties()).containsEntry("errorCode", "IDEMPOTENCY_PAYLOAD_MISMATCH");
  }

  @Test
  void handleIdempotencyConflict_inProgress_shouldReturn409() {
    IdempotencyConflictException ex = IdempotencyConflictException.inProgress();
    ProblemDetail problem = exceptionHandler.handleIdempotencyConflict(ex);

    assertThat(problem.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
    assertThat(problem.getTitle()).isEqualTo("Idempotency Conflict");
    assertThat(problem.getProperties()).containsEntry("errorCode", "IDEMPOTENCY_IN_PROGRESS");
  }

  @Test
  void handleOrderProcessing_shouldReturn422() {
    OrderProcessingException ex = new OrderProcessingException("Out of stock", "STOCK_FAILED");
    ProblemDetail problem = exceptionHandler.handleOrderProcessing(ex);

    assertThat(problem.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY.value());
    assertThat(problem.getTitle()).isEqualTo("Order Processing Failed");
    assertThat(problem.getProperties()).containsEntry("errorCode", "STOCK_FAILED");
  }

  @Test
  void handleResourceNotFound_shouldReturn404() {
    ResourceNotFoundException ex = new ResourceNotFoundException("Order", 5L);
    ProblemDetail problem = exceptionHandler.handleResourceNotFound(ex);

    assertThat(problem.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
    assertThat(problem.getTitle()).isEqualTo("Resource Not Found");
    assertThat(problem.getProperties()).containsEntry("errorCode", "RESOURCE_NOT_FOUND");
  }

  @Test
  void handleMissingHeader_shouldReturn400() {
    MissingRequestHeaderException ex = new MissingRequestHeaderException("Idempotency-Key", null);
    ProblemDetail problem = exceptionHandler.handleMissingHeader(ex);

    assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    assertThat(problem.getTitle()).isEqualTo("Missing Required Header");
    assertThat(problem.getProperties()).containsEntry("errorCode", "MISSING_HEADER");
  }

  @Test
  void handleAccessDenied_shouldReturn403() {
    AccessDeniedException ex = new AccessDeniedException("Forbidden access");
    ProblemDetail problem = exceptionHandler.handleAccessDenied(ex);

    assertThat(problem.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
    assertThat(problem.getTitle()).isEqualTo("Forbidden");
    assertThat(problem.getProperties()).containsEntry("errorCode", "ACCESS_DENIED");
  }

  @Test
  void handleAuthentication_shouldReturn401() {
    BadCredentialsException ex = new BadCredentialsException("Auth error");
    ProblemDetail problem = exceptionHandler.handleAuthentication(ex);

    assertThat(problem.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
    assertThat(problem.getTitle()).isEqualTo("Unauthorized");
    assertThat(problem.getProperties()).containsEntry("errorCode", "UNAUTHORIZED");
  }

  @Test
  void handleGenericException_shouldReturn500() {
    RuntimeException ex = new RuntimeException("Generic crash");
    ProblemDetail problem = exceptionHandler.handleGenericException(ex);

    assertThat(problem.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
    assertThat(problem.getTitle()).isEqualTo("Internal Server Error");
    assertThat(problem.getProperties()).containsEntry("errorCode", "INTERNAL_SERVER_ERROR");
  }
}
