package com.shop.catalog.exception;

public class InsufficientStockException extends DomainException {

  public InsufficientStockException(Long productId, int requested, int available) {
    super(
        String.format(
            "Insufficient stock for product ID %d. Requested: %d, Available: %d",
            productId, requested, available),
        "INSUFFICIENT_STOCK");
  }
}
