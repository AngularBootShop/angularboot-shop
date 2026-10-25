package com.shop.order.service;

import com.shop.order.dto.OrderCreateRequest;
import com.shop.order.dto.OrderResponse;
import com.shop.order.dto.PagedResponse;
import org.springframework.data.domain.Pageable;

public interface OrderService {

  OrderResponse createOrder(String idempotencyKey, String userId, OrderCreateRequest request);

  PagedResponse<OrderResponse> getOrders(String userId, boolean isAdmin, Pageable pageable);

  OrderResponse getOrderById(Long id, String userId, boolean isAdmin);
}
