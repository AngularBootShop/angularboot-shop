package com.shop.catalog.exception;

public class OptimisticLockingConflictException extends DomainException {

  public OptimisticLockingConflictException(String message) {
    super(message, "OPTIMISTIC_LOCK_CONFLICT");
  }
}
