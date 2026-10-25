package com.shop.catalog.controller;

import com.shop.catalog.exception.DuplicateResourceException;
import com.shop.catalog.exception.InsufficientStockException;
import com.shop.catalog.exception.OptimisticLockingConflictException;
import com.shop.catalog.exception.ResourceNotFoundException;
import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

  @ExceptionHandler(ResourceNotFoundException.class)
  public ProblemDetail handleResourceNotFound(ResourceNotFoundException ex) {
    log.warn("Resource not found: {}", ex.getMessage());
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    problem.setTitle("Resource Not Found");
    problem.setType(URI.create("https://api.shop.local/errors/not-found"));
    problem.setProperty("errorCode", ex.getErrorCode());
    problem.setProperty("timestamp", Instant.now());
    return problem;
  }

  @ExceptionHandler(InsufficientStockException.class)
  public ProblemDetail handleInsufficientStock(InsufficientStockException ex) {
    log.warn("Insufficient stock error: {}", ex.getMessage());
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    problem.setTitle("Insufficient Stock");
    problem.setType(URI.create("https://api.shop.local/errors/insufficient-stock"));
    problem.setProperty("errorCode", ex.getErrorCode());
    problem.setProperty("timestamp", Instant.now());
    return problem;
  }

  @ExceptionHandler(DuplicateResourceException.class)
  public ProblemDetail handleDuplicateResource(DuplicateResourceException ex) {
    log.warn("Duplicate resource: {}", ex.getMessage());
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    problem.setTitle("Duplicate Resource Conflict");
    problem.setType(URI.create("https://api.shop.local/errors/duplicate-resource"));
    problem.setProperty("errorCode", ex.getErrorCode());
    problem.setProperty("timestamp", Instant.now());
    return problem;
  }

  @ExceptionHandler(OptimisticLockingConflictException.class)
  public ProblemDetail handleOptimisticLockConflict(OptimisticLockingConflictException ex) {
    log.warn("Optimistic lock conflict: {}", ex.getMessage());
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    problem.setTitle("Concurrent Modification Conflict");
    problem.setType(URI.create("https://api.shop.local/errors/optimistic-lock-conflict"));
    problem.setProperty("errorCode", ex.getErrorCode());
    problem.setProperty("timestamp", Instant.now());
    return problem;
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ProblemDetail handleValidationErrors(MethodArgumentNotValidException ex) {
    log.warn("Validation failed for request: {}", ex.getMessage());
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST, "Validation failed for one or more fields");
    problem.setTitle("Constraint Violation");
    problem.setType(URI.create("https://api.shop.local/errors/validation-error"));
    problem.setProperty("errorCode", "VALIDATION_FAILED");
    problem.setProperty("timestamp", Instant.now());

    Map<String, String> errors = new HashMap<>();
    for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
      errors.put(fieldError.getField(), fieldError.getDefaultMessage());
    }
    problem.setProperty("fieldErrors", errors);
    return problem;
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ProblemDetail handleAccessDenied(AccessDeniedException ex) {
    log.warn("Access denied: {}", ex.getMessage());
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(
            HttpStatus.FORBIDDEN, "Access is denied for this resource");
    problem.setTitle("Forbidden");
    problem.setType(URI.create("https://api.shop.local/errors/forbidden"));
    problem.setProperty("errorCode", "ACCESS_DENIED");
    problem.setProperty("timestamp", Instant.now());
    return problem;
  }

  @ExceptionHandler(AuthenticationException.class)
  public ProblemDetail handleAuthentication(AuthenticationException ex) {
    log.warn("Authentication failed: {}", ex.getMessage());
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(
            HttpStatus.UNAUTHORIZED, "Full authentication is required");
    problem.setTitle("Unauthorized");
    problem.setType(URI.create("https://api.shop.local/errors/unauthorized"));
    problem.setProperty("errorCode", "UNAUTHORIZED");
    problem.setProperty("timestamp", Instant.now());
    return problem;
  }

  @ExceptionHandler(Exception.class)
  public ProblemDetail handleGenericException(Exception ex) {
    log.error("Unhandled server exception: ", ex);
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(
            HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
    problem.setTitle("Internal Server Error");
    problem.setType(URI.create("https://api.shop.local/errors/internal-error"));
    problem.setProperty("errorCode", "INTERNAL_SERVER_ERROR");
    problem.setProperty("timestamp", Instant.now());
    return problem;
  }
}
