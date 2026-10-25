package com.shop.catalog.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.shop.catalog.exception.DuplicateResourceException;
import com.shop.catalog.exception.InsufficientStockException;
import com.shop.catalog.exception.OptimisticLockingConflictException;
import com.shop.catalog.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;

class GlobalExceptionHandlerTest {

  private GlobalExceptionHandler exceptionHandler;

  @BeforeEach
  void setUp() {
    exceptionHandler = new GlobalExceptionHandler();
  }

  @Test
  void handleResourceNotFound_shouldReturn404() {
    ResourceNotFoundException ex = new ResourceNotFoundException("Product", 10L);
    ProblemDetail problem = exceptionHandler.handleResourceNotFound(ex);

    assertThat(problem.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
    assertThat(problem.getTitle()).isEqualTo("Resource Not Found");
    assertThat(problem.getProperties()).containsEntry("errorCode", "RESOURCE_NOT_FOUND");
  }

  @Test
  void handleInsufficientStock_shouldReturn422() {
    InsufficientStockException ex = new InsufficientStockException(1L, 10, 5);
    ProblemDetail problem = exceptionHandler.handleInsufficientStock(ex);

    assertThat(problem.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY.value());
    assertThat(problem.getTitle()).isEqualTo("Insufficient Stock");
    assertThat(problem.getProperties()).containsEntry("errorCode", "INSUFFICIENT_STOCK");
  }

  @Test
  void handleDuplicateResource_shouldReturn409() {
    DuplicateResourceException ex = new DuplicateResourceException("Product already exists");
    ProblemDetail problem = exceptionHandler.handleDuplicateResource(ex);

    assertThat(problem.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
    assertThat(problem.getTitle()).isEqualTo("Duplicate Resource Conflict");
    assertThat(problem.getProperties()).containsEntry("errorCode", "DUPLICATE_RESOURCE");
  }

  @Test
  void handleOptimisticLockConflict_shouldReturn409() {
    OptimisticLockingConflictException ex =
        new OptimisticLockingConflictException("Version mismatch");
    ProblemDetail problem = exceptionHandler.handleOptimisticLockConflict(ex);

    assertThat(problem.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
    assertThat(problem.getTitle()).isEqualTo("Concurrent Modification Conflict");
    assertThat(problem.getProperties()).containsEntry("errorCode", "OPTIMISTIC_LOCK_CONFLICT");
  }

  @Test
  void handleAccessDenied_shouldReturn403() {
    AccessDeniedException ex = new AccessDeniedException("Access denied");
    ProblemDetail problem = exceptionHandler.handleAccessDenied(ex);

    assertThat(problem.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
    assertThat(problem.getTitle()).isEqualTo("Forbidden");
    assertThat(problem.getProperties()).containsEntry("errorCode", "ACCESS_DENIED");
  }

  @Test
  void handleAuthentication_shouldReturn401() {
    BadCredentialsException ex = new BadCredentialsException("Bad token");
    ProblemDetail problem = exceptionHandler.handleAuthentication(ex);

    assertThat(problem.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
    assertThat(problem.getTitle()).isEqualTo("Unauthorized");
    assertThat(problem.getProperties()).containsEntry("errorCode", "UNAUTHORIZED");
  }

  @Test
  void handleGenericException_shouldReturn500() {
    RuntimeException ex = new RuntimeException("Unexpected database failure");
    ProblemDetail problem = exceptionHandler.handleGenericException(ex);

    assertThat(problem.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
    assertThat(problem.getTitle()).isEqualTo("Internal Server Error");
    assertThat(problem.getProperties()).containsEntry("errorCode", "INTERNAL_SERVER_ERROR");
  }
}
