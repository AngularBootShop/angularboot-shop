package com.shop.order.controller;

import com.shop.order.exception.IdempotencyConflictException;
import com.shop.order.exception.OrderProcessingException;
import com.shop.order.exception.ResourceNotFoundException;
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
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

  @ExceptionHandler(IdempotencyConflictException.class)
  public ProblemDetail handleIdempotencyConflict(IdempotencyConflictException ex) {
    log.warn("Idempotency conflict [{}]: {}", ex.getHttpStatus(), ex.getMessage());
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(ex.getHttpStatus(), ex.getMessage());
    problem.setTitle("Idempotency Conflict");
    problem.setType(URI.create("https://api.shop.local/errors/idempotency-conflict"));
    problem.setProperty("errorCode", ex.getErrorCode());
    problem.setProperty("timestamp", Instant.now());
    return problem;
  }

  @ExceptionHandler(OrderProcessingException.class)
  public ProblemDetail handleOrderProcessing(OrderProcessingException ex) {
    log.warn("Order processing failure: {}", ex.getMessage());
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    problem.setTitle("Order Processing Failed");
    problem.setType(URI.create("https://api.shop.local/errors/order-processing-failed"));
    problem.setProperty("errorCode", ex.getErrorCode());
    problem.setProperty("timestamp", Instant.now());
    return problem;
  }

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

  @ExceptionHandler(MissingRequestHeaderException.class)
  public ProblemDetail handleMissingHeader(MissingRequestHeaderException ex) {
    log.warn("Missing required request header: {}", ex.getHeaderName());
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            String.format("Required request header '%s' is not present", ex.getHeaderName()));
    problem.setTitle("Missing Required Header");
    problem.setType(URI.create("https://api.shop.local/errors/missing-header"));
    problem.setProperty("errorCode", "MISSING_HEADER");
    problem.setProperty("timestamp", Instant.now());
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
