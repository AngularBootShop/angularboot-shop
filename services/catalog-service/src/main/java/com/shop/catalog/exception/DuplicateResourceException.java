package com.shop.catalog.exception;

public class DuplicateResourceException extends DomainException {

  public DuplicateResourceException(String message) {
    super(message, "DUPLICATE_RESOURCE");
  }
}
