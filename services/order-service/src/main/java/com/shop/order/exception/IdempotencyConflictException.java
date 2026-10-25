package com.shop.order.exception;

import org.springframework.http.HttpStatus;

public class IdempotencyConflictException extends DomainException {

  private final HttpStatus httpStatus;

  public IdempotencyConflictException(String message, HttpStatus httpStatus, String errorCode) {
    super(message, errorCode);
    this.httpStatus = httpStatus;
  }

  public static IdempotencyConflictException payloadMismatch() {
    return new IdempotencyConflictException(
        "Idempotency-Key reused with conflicting request payload",
        HttpStatus.UNPROCESSABLE_ENTITY,
        "IDEMPOTENCY_PAYLOAD_MISMATCH");
  }

  public static IdempotencyConflictException inProgress() {
    return new IdempotencyConflictException(
        "An order creation request is currently in progress for this Idempotency-Key. Please retry shortly.",
        HttpStatus.CONFLICT,
        "IDEMPOTENCY_IN_PROGRESS");
  }

  public HttpStatus getHttpStatus() {
    return httpStatus;
  }
}
