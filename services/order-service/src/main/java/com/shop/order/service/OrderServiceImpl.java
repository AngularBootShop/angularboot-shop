package com.shop.order.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.order.client.CatalogClient;
import com.shop.order.dto.OrderCreateRequest;
import com.shop.order.dto.OrderItemRequest;
import com.shop.order.dto.OrderResponse;
import com.shop.order.dto.PagedResponse;
import com.shop.order.dto.ProductDto;
import com.shop.order.entity.IdempotencyRecord;
import com.shop.order.entity.IdempotencyStatus;
import com.shop.order.entity.Order;
import com.shop.order.entity.OrderItem;
import com.shop.order.entity.OrderStatus;
import com.shop.order.exception.IdempotencyConflictException;
import com.shop.order.exception.OrderProcessingException;
import com.shop.order.exception.ResourceNotFoundException;
import com.shop.order.mapper.OrderMapper;
import com.shop.order.repository.IdempotencyRecordRepository;
import com.shop.order.repository.OrderRepository;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

  private final OrderRepository orderRepository;
  private final IdempotencyRecordRepository idempotencyRepository;
  private final CatalogClient catalogClient;
  private final OrderMapper orderMapper;
  private final ObjectMapper objectMapper;

  @Override
  @Transactional
  public OrderResponse createOrder(
      String idempotencyKey, String userId, OrderCreateRequest request) {
    log.info(
        "Processing order placement for user: {} with Idempotency-Key: {}", userId, idempotencyKey);

    String requestHash = computeSha256(request);

    // 1. Idempotency Check
    Optional<IdempotencyRecord> existingRecordOpt = idempotencyRepository.findById(idempotencyKey);
    if (existingRecordOpt.isPresent()) {
      IdempotencyRecord existingRecord = existingRecordOpt.get();

      // Hash mismatch check -> 422 Unprocessable Entity
      if (!existingRecord.getRequestHash().equals(requestHash)) {
        log.warn(
            "Idempotency-Key '{}' reused with conflicting payload hash. Existing: {}, New: {}",
            idempotencyKey,
            existingRecord.getRequestHash(),
            requestHash);
        throw IdempotencyConflictException.payloadMismatch();
      }

      // In-flight concurrent request check -> 409 Conflict
      if (existingRecord.getStatus() == IdempotencyStatus.IN_PROGRESS) {
        log.warn("Concurrent duplicate request detected for in-progress key: {}", idempotencyKey);
        throw IdempotencyConflictException.inProgress();
      }

      // Already completed request -> return cached response
      if (existingRecord.getStatus() == IdempotencyStatus.COMPLETED
          && existingRecord.getResponseBody() != null) {
        log.info(
            "Returning cached order response for completed Idempotency-Key: {}", idempotencyKey);
        try {
          return objectMapper.readValue(existingRecord.getResponseBody(), OrderResponse.class);
        } catch (JsonProcessingException e) {
          log.error("Failed to deserialize cached response: {}", e.getMessage());
        }
      }
    }

    // Record initial IN_PROGRESS status
    IdempotencyRecord record =
        IdempotencyRecord.builder()
            .idempotencyKey(idempotencyKey)
            .requestHash(requestHash)
            .status(IdempotencyStatus.IN_PROGRESS)
            .build();
    idempotencyRepository.saveAndFlush(record);

    // 2. Fetch products and calculate total amount
    String orderNumber = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    BigDecimal totalAmount = BigDecimal.ZERO;
    List<OrderItem> orderItems = new ArrayList<>();

    for (OrderItemRequest itemReq : request.getItems()) {
      ProductDto product = catalogClient.getProduct(itemReq.getProductId());
      if (product == null) {
        throw new ResourceNotFoundException(
            "Product with ID " + itemReq.getProductId() + " not found in catalog");
      }

      BigDecimal subtotal = product.getPrice().multiply(BigDecimal.valueOf(itemReq.getQuantity()));
      totalAmount = totalAmount.add(subtotal);

      OrderItem orderItem =
          OrderItem.builder()
              .productId(product.getId())
              .productName(product.getName())
              .unitPrice(product.getPrice())
              .quantity(itemReq.getQuantity())
              .subtotal(subtotal)
              .build();
      orderItems.add(orderItem);
    }

    // 3. Save initial order in PENDING status
    Order order =
        Order.builder()
            .orderNumber(orderNumber)
            .userId(userId)
            .status(OrderStatus.PENDING)
            .totalAmount(totalAmount)
            .build();

    for (OrderItem item : orderItems) {
      order.addItem(item);
    }
    Order savedOrder = orderRepository.saveAndFlush(order);

    // 4. Saga Execution: Synchronous Stock Deduction with Compensation
    List<OrderItemRequest> successfullyDeductedItems = new ArrayList<>();
    try {
      for (OrderItemRequest itemReq : request.getItems()) {
        log.info(
            "Saga step: Deducting stock for product ID: {}, qty: {}",
            itemReq.getProductId(),
            itemReq.getQuantity());
        catalogClient.deductStock(itemReq.getProductId(), itemReq.getQuantity());
        successfullyDeductedItems.add(itemReq);
      }

      // All deductions succeeded -> Confirm order
      savedOrder.setStatus(OrderStatus.CONFIRMED);
      Order confirmedOrder = orderRepository.saveAndFlush(savedOrder);
      OrderResponse response = orderMapper.toResponse(confirmedOrder);

      // Update idempotency record to COMPLETED
      record.setStatus(IdempotencyStatus.COMPLETED);
      record.setResponseStatus(201);
      try {
        record.setResponseBody(objectMapper.writeValueAsString(response));
      } catch (JsonProcessingException e) {
        log.error("Failed to serialize order response: {}", e.getMessage());
      }
      idempotencyRepository.saveAndFlush(record);

      log.info("Order {} successfully confirmed for user: {}", orderNumber, userId);
      return response;

    } catch (Exception ex) {
      log.error(
          "Saga failure during stock deduction for order {}: {}. Initiating compensating transactions...",
          orderNumber,
          ex.getMessage());

      // Compensating action: Restore stock for all already deducted items
      for (OrderItemRequest deducted : successfullyDeductedItems) {
        try {
          log.info(
              "Compensating action: Restoring stock for product ID: {}, qty: {}",
              deducted.getProductId(),
              deducted.getQuantity());
          catalogClient.restoreStock(deducted.getProductId(), deducted.getQuantity());
        } catch (Exception restoreEx) {
          log.error(
              "CRITICAL: Failed to compensate stock restore for product ID {}: {}",
              deducted.getProductId(),
              restoreEx.getMessage());
        }
      }

      // Transition order status to CANCELLED
      savedOrder.setStatus(OrderStatus.CANCELLED);
      orderRepository.saveAndFlush(savedOrder);

      // Mark idempotency record as FAILED
      record.setStatus(IdempotencyStatus.FAILED);
      idempotencyRepository.saveAndFlush(record);

      throw new OrderProcessingException(
          "Order placement failed: " + ex.getMessage(), "ORDER_PLACEMENT_FAILED");
    }
  }

  @Override
  @Transactional(readOnly = true)
  public PagedResponse<OrderResponse> getOrders(String userId, boolean isAdmin, Pageable pageable) {
    log.debug("Fetching orders for userId: {}, isAdmin: {}", userId, isAdmin);
    Page<Order> page;
    if (isAdmin) {
      page = orderRepository.findAll(pageable);
    } else {
      page = orderRepository.findByUserId(userId, pageable);
    }
    return PagedResponse.from(page.map(orderMapper::toResponse));
  }

  @Override
  @Transactional(readOnly = true)
  public OrderResponse getOrderById(Long id, String userId, boolean isAdmin) {
    log.debug("Fetching order ID: {} for userId: {}", id, userId);
    Order order =
        orderRepository
            .findWithItemsById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Order", id));

    if (!isAdmin && !order.getUserId().equals(userId)) {
      log.warn("User {} attempted unauthorized access to order ID {}", userId, id);
      throw new AccessDeniedException("You do not have permission to view this order");
    }

    return orderMapper.toResponse(order);
  }

  private String computeSha256(OrderCreateRequest request) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      String json = objectMapper.writeValueAsString(request);
      byte[] hash = digest.digest(json.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash);
    } catch (NoSuchAlgorithmException | JsonProcessingException e) {
      throw new IllegalStateException("Failed to calculate SHA-256 hash", e);
    }
  }
}
