package com.shop.order.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.shop.order.client.CatalogClient;
import com.shop.order.dto.OrderCreateRequest;
import com.shop.order.dto.OrderItemRequest;
import com.shop.order.dto.OrderResponse;
import com.shop.order.dto.ProductDto;
import com.shop.order.entity.OrderStatus;
import com.shop.order.repository.IdempotencyRecordRepository;
import com.shop.order.repository.OrderRepository;
import com.shop.order.service.OrderService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class OrderIntegrationTest {

  @Container
  static PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>("postgres:16-alpine")
          .withDatabaseName("test_order_db")
          .withUsername("test_user")
          .withPassword("test_pass");

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
    registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    registry.add("spring.flyway.enabled", () -> "true");
    registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
  }

  @MockitoBean private JwtDecoder jwtDecoder;

  @MockitoBean private CatalogClient catalogClient;

  @Autowired private OrderService orderService;

  @Autowired private OrderRepository orderRepository;

  @Autowired private IdempotencyRecordRepository idempotencyRepository;

  @Test
  void createOrder_andPersistInDatabase_shouldSucceed() {
    String key = "integ-key-001";
    String userId = "integ-user";
    OrderCreateRequest request = new OrderCreateRequest(List.of(new OrderItemRequest(1L, 3)));

    ProductDto product =
        ProductDto.builder()
            .id(1L)
            .name("Integration Mechanical Keyboard")
            .price(BigDecimal.valueOf(120.00))
            .stock(50)
            .build();

    Mockito.when(catalogClient.getProduct(1L)).thenReturn(product);
    Mockito.when(catalogClient.deductStock(1L, 3)).thenReturn(product);

    OrderResponse created = orderService.createOrder(key, userId, request);

    assertThat(created.getId()).isNotNull();
    assertThat(created.getOrderNumber()).startsWith("ORD-");
    assertThat(created.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    assertThat(created.getTotalAmount()).isEqualTo(BigDecimal.valueOf(360.00));
    assertThat(created.getItems()).hasSize(1);

    // Verify database persistence
    assertThat(orderRepository.existsById(created.getId())).isTrue();
    assertThat(idempotencyRepository.existsById(key)).isTrue();
  }
}
