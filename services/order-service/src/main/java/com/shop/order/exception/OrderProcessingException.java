package com.shop.order.exception;

public class OrderProcessingException extends DomainException {

  public OrderProcessingException(String message) {
    super(message, "ORDER_PROCESSING_FAILED");
  }

  public OrderProcessingException(String message, String errorCode) {
    super(message, errorCode);
  }
}
