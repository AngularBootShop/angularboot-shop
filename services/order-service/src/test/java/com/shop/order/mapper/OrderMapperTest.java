package com.shop.order.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.shop.order.dto.OrderItemResponse;
import com.shop.order.dto.OrderResponse;
import com.shop.order.entity.Order;
import com.shop.order.entity.OrderItem;
import com.shop.order.entity.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class OrderMapperTest {

  private OrderMapper mapper;

  @BeforeEach
  void setUp() {
    mapper = Mappers.getMapper(OrderMapper.class);
  }

  @Test
  void toResponse_shouldMapOrderAndItems() {
    OrderItem item =
        OrderItem.builder()
            .id(1L)
            .productId(10L)
            .productName("Keyboard")
            .unitPrice(BigDecimal.valueOf(50))
            .quantity(2)
            .subtotal(BigDecimal.valueOf(100))
            .build();

    Order order =
        Order.builder()
            .id(100L)
            .orderNumber("ORD-12345")
            .userId("customer-1")
            .status(OrderStatus.CONFIRMED)
            .totalAmount(BigDecimal.valueOf(100))
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
    order.addItem(item);

    OrderResponse response = mapper.toResponse(order);

    assertThat(response).isNotNull();
    assertThat(response.getId()).isEqualTo(100L);
    assertThat(response.getOrderNumber()).isEqualTo("ORD-12345");
    assertThat(response.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    assertThat(response.getItems()).hasSize(1);
    assertThat(response.getItems().get(0).getProductName()).isEqualTo("Keyboard");
  }

  @Test
  void toResponseList_shouldMapAllOrders() {
    Order o1 =
        Order.builder()
            .id(1L)
            .orderNumber("O1")
            .userId("U1")
            .status(OrderStatus.CONFIRMED)
            .totalAmount(BigDecimal.TEN)
            .build();
    Order o2 =
        Order.builder()
            .id(2L)
            .orderNumber("O2")
            .userId("U2")
            .status(OrderStatus.PENDING)
            .totalAmount(BigDecimal.valueOf(20))
            .build();

    List<OrderResponse> list = mapper.toResponseList(List.of(o1, o2));

    assertThat(list).hasSize(2);
    assertThat(list.get(0).getOrderNumber()).isEqualTo("O1");
    assertThat(list.get(1).getOrderNumber()).isEqualTo("O2");
  }

  @Test
  void toItemResponse_shouldMapAllFields() {
    OrderItem item =
        OrderItem.builder()
            .id(5L)
            .productId(20L)
            .productName("Mouse")
            .unitPrice(BigDecimal.valueOf(30))
            .quantity(1)
            .subtotal(BigDecimal.valueOf(30))
            .build();

    OrderItemResponse response = mapper.toItemResponse(item);

    assertThat(response).isNotNull();
    assertThat(response.getProductId()).isEqualTo(20L);
    assertThat(response.getProductName()).isEqualTo("Mouse");
  }
}
