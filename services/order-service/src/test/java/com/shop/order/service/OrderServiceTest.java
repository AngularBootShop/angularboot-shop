package com.shop.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.order.client.CatalogClient;
import com.shop.order.dto.*;
import com.shop.order.entity.*;
import com.shop.order.exception.IdempotencyConflictException;
import com.shop.order.exception.OrderProcessingException;
import com.shop.order.mapper.OrderMapper;
import com.shop.order.repository.IdempotencyRecordRepository;
import com.shop.order.repository.OrderRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

  @Mock private OrderRepository orderRepository;

  @Mock private IdempotencyRecordRepository idempotencyRepository;

  @Mock private CatalogClient catalogClient;

  private OrderMapper orderMapper;
  private ObjectMapper objectMapper;
  private OrderService orderService;

  @BeforeEach
  void setUp() {
    orderMapper = Mappers.getMapper(OrderMapper.class);
    objectMapper = new ObjectMapper();
    orderService =
        new OrderServiceImpl(
            orderRepository, idempotencyRepository, catalogClient, orderMapper, objectMapper);
  }

  @Test
  void createOrder_happyPath_shouldConfirmOrderAndCompleteIdempotency() {
    String key = "idemp-001";
    String userId = "user-123";
    OrderCreateRequest request =
        new OrderCreateRequest(List.of(new OrderItemRequest(1L, 2), new OrderItemRequest(2L, 1)));

    ProductDto p1 =
        ProductDto.builder()
            .id(1L)
            .name("Headphones")
            .price(BigDecimal.valueOf(100))
            .stock(10)
            .build();
    ProductDto p2 =
        ProductDto.builder().id(2L).name("Mouse").price(BigDecimal.valueOf(50)).stock(5).build();

    when(idempotencyRepository.findById(key)).thenReturn(Optional.empty());
    when(catalogClient.getProduct(1L)).thenReturn(p1);
    when(catalogClient.getProduct(2L)).thenReturn(p2);
    when(orderRepository.saveAndFlush(any(Order.class)))
        .thenAnswer(
            inv -> {
              Order o = inv.getArgument(0);
              o.setId(10L);
              return o;
            });

    OrderResponse response = orderService.createOrder(key, userId, request);

    assertThat(response).isNotNull();
    assertThat(response.getId()).isEqualTo(10L);
    assertThat(response.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    assertThat(response.getTotalAmount()).isEqualTo(BigDecimal.valueOf(250));

    // Verify stock deduction occurred for both products
    verify(catalogClient).deductStock(1L, 2);
    verify(catalogClient).deductStock(2L, 1);
    // Verify no compensation calls
    verify(catalogClient, never()).restoreStock(anyLong(), anyInt());

    // Verify idempotency record saved as COMPLETED
    verify(idempotencyRepository, atLeastOnce())
        .saveAndFlush(
            argThat(
                r ->
                    r.getIdempotencyKey().equals(key)
                        && r.getStatus() == IdempotencyStatus.COMPLETED));
  }

  @Test
  void createOrder_partialFailureSaga_shouldRestoreStockAndCancelOrder() {
    String key = "idemp-fail-001";
    String userId = "user-123";
    OrderCreateRequest request =
        new OrderCreateRequest(List.of(new OrderItemRequest(1L, 2), new OrderItemRequest(2L, 5)));

    ProductDto p1 =
        ProductDto.builder()
            .id(1L)
            .name("Keyboard")
            .price(BigDecimal.valueOf(80))
            .stock(10)
            .build();
    ProductDto p2 =
        ProductDto.builder().id(2L).name("Monitor").price(BigDecimal.valueOf(300)).stock(2).build();

    when(idempotencyRepository.findById(key)).thenReturn(Optional.empty());
    when(catalogClient.getProduct(1L)).thenReturn(p1);
    when(catalogClient.getProduct(2L)).thenReturn(p2);
    when(orderRepository.saveAndFlush(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

    // Item 1 succeeds, Item 2 fails
    when(catalogClient.deductStock(1L, 2)).thenReturn(p1);
    when(catalogClient.deductStock(2L, 5)).thenThrow(new OrderProcessingException("Out of stock"));

    assertThatThrownBy(() -> orderService.createOrder(key, userId, request))
        .isInstanceOf(OrderProcessingException.class)
        .hasMessageContaining("Order placement failed");

    // Verify compensating stock restore was executed for item 1
    verify(catalogClient).restoreStock(1L, 2);
    // Item 2 never succeeded deduction, so restore should not be called for item 2
    verify(catalogClient, never()).restoreStock(2L, 5);

    // Verify order saved as CANCELLED
    verify(orderRepository, atLeastOnce())
        .saveAndFlush(argThat(o -> o.getStatus() == OrderStatus.CANCELLED));

    // Verify idempotency record marked as FAILED
    verify(idempotencyRepository, atLeastOnce())
        .saveAndFlush(argThat(r -> r.getStatus() == IdempotencyStatus.FAILED));
  }

  @Test
  void createOrder_whenKeyReusedWithIdenticalPayload_shouldReturnCachedResponse() throws Exception {
    String key = "idemp-cached-001";
    String userId = "user-123";
    OrderCreateRequest request = new OrderCreateRequest(List.of(new OrderItemRequest(1L, 1)));

    OrderResponse cachedResponse =
        OrderResponse.builder()
            .id(99L)
            .orderNumber("ORD-CACHED")
            .userId(userId)
            .status(OrderStatus.CONFIRMED)
            .totalAmount(BigDecimal.TEN)
            .build();

    // Calculate actual hash for request
    java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
    byte[] hashBytes =
        md.digest(
            objectMapper
                .writeValueAsString(request)
                .getBytes(java.nio.charset.StandardCharsets.UTF_8));
    String actualHash = java.util.HexFormat.of().formatHex(hashBytes);

    IdempotencyRecord cachedRecord =
        IdempotencyRecord.builder()
            .idempotencyKey(key)
            .requestHash(actualHash)
            .status(IdempotencyStatus.COMPLETED)
            .responseStatus(201)
            .responseBody(objectMapper.writeValueAsString(cachedResponse))
            .build();

    when(idempotencyRepository.findById(key)).thenReturn(Optional.of(cachedRecord));

    OrderResponse result = orderService.createOrder(key, userId, request);

    assertThat(result.getId()).isEqualTo(99L);
    assertThat(result.getOrderNumber()).isEqualTo("ORD-CACHED");

    // Verify no database or catalog calls were made
    verify(catalogClient, never()).getProduct(anyLong());
    verify(catalogClient, never()).deductStock(anyLong(), anyInt());
    verify(orderRepository, never()).saveAndFlush(any());
  }

  @Test
  void createOrder_whenKeyReusedWithMismatchedPayload_shouldThrow422() {
    String key = "idemp-mismatch";
    String userId = "user-123";
    OrderCreateRequest request = new OrderCreateRequest(List.of(new OrderItemRequest(1L, 2)));

    IdempotencyRecord existing =
        IdempotencyRecord.builder()
            .idempotencyKey(key)
            .requestHash("different-sha256-hash-value-00000000000000000000000000000000000000000")
            .status(IdempotencyStatus.COMPLETED)
            .build();

    when(idempotencyRepository.findById(key)).thenReturn(Optional.of(existing));

    assertThatThrownBy(() -> orderService.createOrder(key, userId, request))
        .isInstanceOf(IdempotencyConflictException.class)
        .satisfies(
            ex -> {
              IdempotencyConflictException ice = (IdempotencyConflictException) ex;
              assertThat(ice.getHttpStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
              assertThat(ice.getErrorCode()).isEqualTo("IDEMPOTENCY_PAYLOAD_MISMATCH");
            });
  }

  @Test
  void createOrder_whenConcurrentDuplicateInProgress_shouldThrow409() throws Exception {
    String key = "idemp-inprogress";
    String userId = "user-123";
    OrderCreateRequest request = new OrderCreateRequest(List.of(new OrderItemRequest(1L, 2)));

    java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
    byte[] hashBytes =
        md.digest(
            objectMapper
                .writeValueAsString(request)
                .getBytes(java.nio.charset.StandardCharsets.UTF_8));
    String actualHash = java.util.HexFormat.of().formatHex(hashBytes);

    IdempotencyRecord inProgressRecord =
        IdempotencyRecord.builder()
            .idempotencyKey(key)
            .requestHash(actualHash)
            .status(IdempotencyStatus.IN_PROGRESS)
            .build();

    when(idempotencyRepository.findById(key)).thenReturn(Optional.of(inProgressRecord));

    assertThatThrownBy(() -> orderService.createOrder(key, userId, request))
        .isInstanceOf(IdempotencyConflictException.class)
        .satisfies(
            ex -> {
              IdempotencyConflictException ice = (IdempotencyConflictException) ex;
              assertThat(ice.getHttpStatus()).isEqualTo(HttpStatus.CONFLICT);
              assertThat(ice.getErrorCode()).isEqualTo("IDEMPOTENCY_IN_PROGRESS");
            });
  }

  @Test
  void getOrders_forAdmin_shouldReturnAllOrders() {
    Pageable pageable = PageRequest.of(0, 10);
    Order o =
        Order.builder()
            .id(1L)
            .orderNumber("O1")
            .userId("cust-1")
            .status(OrderStatus.CONFIRMED)
            .totalAmount(BigDecimal.TEN)
            .build();
    Page<Order> page = new PageImpl<>(List.of(o), pageable, 1);

    when(orderRepository.findAll(pageable)).thenReturn(page);

    PagedResponse<OrderResponse> result = orderService.getOrders("admin", true, pageable);

    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getContent().get(0).getOrderNumber()).isEqualTo("O1");
  }

  @Test
  void getOrders_forRegularUser_shouldFilterByUserId() {
    Pageable pageable = PageRequest.of(0, 10);
    Order o =
        Order.builder()
            .id(1L)
            .orderNumber("O1")
            .userId("cust-1")
            .status(OrderStatus.CONFIRMED)
            .totalAmount(BigDecimal.TEN)
            .build();
    Page<Order> page = new PageImpl<>(List.of(o), pageable, 1);

    when(orderRepository.findByUserId("cust-1", pageable)).thenReturn(page);

    PagedResponse<OrderResponse> result = orderService.getOrders("cust-1", false, pageable);

    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getContent().get(0).getUserId()).isEqualTo("cust-1");
  }

  @Test
  void getOrderById_whenUserOwnsOrder_shouldReturnOrder() {
    Order o =
        Order.builder()
            .id(1L)
            .orderNumber("O1")
            .userId("cust-1")
            .status(OrderStatus.CONFIRMED)
            .totalAmount(BigDecimal.TEN)
            .build();
    when(orderRepository.findWithItemsById(1L)).thenReturn(Optional.of(o));

    OrderResponse response = orderService.getOrderById(1L, "cust-1", false);

    assertThat(response).isNotNull();
    assertThat(response.getId()).isEqualTo(1L);
  }

  @Test
  void getOrderById_whenUserDoesNotOwnOrder_shouldThrowAccessDenied() {
    Order o =
        Order.builder()
            .id(1L)
            .orderNumber("O1")
            .userId("other-user")
            .status(OrderStatus.CONFIRMED)
            .totalAmount(BigDecimal.TEN)
            .build();
    when(orderRepository.findWithItemsById(1L)).thenReturn(Optional.of(o));

    assertThatThrownBy(() -> orderService.getOrderById(1L, "cust-1", false))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessageContaining("You do not have permission");
  }
}
