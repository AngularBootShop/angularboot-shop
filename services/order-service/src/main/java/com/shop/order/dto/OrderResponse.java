package com.shop.order.dto;

import com.shop.order.entity.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {

  private Long id;
  private String orderNumber;
  private String userId;
  private OrderStatus status;
  private BigDecimal totalAmount;
  private Instant createdAt;
  private List<OrderItemResponse> items;
}
