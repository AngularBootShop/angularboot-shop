package com.shop.order.controller;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.order.config.SecurityConfig;
import com.shop.order.dto.*;
import com.shop.order.entity.OrderStatus;
import com.shop.order.service.OrderService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(OrderController.class)
@Import(SecurityConfig.class)
class OrderControllerTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private OrderService orderService;

  @MockitoBean private JwtDecoder jwtDecoder;

  @Test
  void createOrder_missingIdempotencyKey_shouldReturnBadRequestProblemDetail() throws Exception {
    OrderCreateRequest request = new OrderCreateRequest(List.of(new OrderItemRequest(1L, 2)));

    mockMvc
        .perform(
            post("/api/v1/orders")
                .with(
                    jwt()
                        .jwt(
                            j ->
                                j.claim("aud", List.of("shop-api"))
                                    .claim("preferred_username", "testuser"))
                        .authorities(new SimpleGrantedAuthority("ROLE_USER")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode", is("MISSING_HEADER")));
  }

  @Test
  void createOrder_withValidKeyAndAuth_shouldReturnCreated() throws Exception {
    OrderCreateRequest request = new OrderCreateRequest(List.of(new OrderItemRequest(1L, 2)));
    OrderResponse response =
        OrderResponse.builder()
            .id(100L)
            .orderNumber("ORD-TEST-001")
            .userId("testuser")
            .status(OrderStatus.CONFIRMED)
            .totalAmount(BigDecimal.valueOf(100))
            .createdAt(Instant.now())
            .items(
                List.of(
                    OrderItemResponse.builder()
                        .id(1L)
                        .productId(1L)
                        .productName("Keyboard")
                        .unitPrice(BigDecimal.valueOf(50))
                        .quantity(2)
                        .subtotal(BigDecimal.valueOf(100))
                        .build()))
            .build();

    when(orderService.createOrder(eq("key-12345"), eq("testuser"), any(OrderCreateRequest.class)))
        .thenReturn(response);

    mockMvc
        .perform(
            post("/api/v1/orders")
                .header("Idempotency-Key", "key-12345")
                .with(
                    jwt()
                        .jwt(
                            j ->
                                j.claim("aud", List.of("shop-api"))
                                    .claim("preferred_username", "testuser"))
                        .authorities(new SimpleGrantedAuthority("ROLE_USER")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id", is(100)))
        .andExpect(jsonPath("$.orderNumber", is("ORD-TEST-001")))
        .andExpect(jsonPath("$.status", is("CONFIRMED")));
  }

  @Test
  void getOrders_withUserAuth_shouldReturnPagedOrders() throws Exception {
    PagedResponse<OrderResponse> pagedResponse =
        PagedResponse.<OrderResponse>builder()
            .content(
                List.of(
                    OrderResponse.builder()
                        .id(1L)
                        .orderNumber("O1")
                        .status(OrderStatus.CONFIRMED)
                        .totalAmount(BigDecimal.TEN)
                        .build()))
            .totalElements(1)
            .totalPages(1)
            .pageNumber(0)
            .pageSize(10)
            .isLast(true)
            .build();

    when(orderService.getOrders(eq("testuser"), eq(false), any())).thenReturn(pagedResponse);

    mockMvc
        .perform(
            get("/api/v1/orders")
                .with(
                    jwt()
                        .jwt(
                            j ->
                                j.claim("aud", List.of("shop-api"))
                                    .claim("preferred_username", "testuser"))
                        .authorities(new SimpleGrantedAuthority("ROLE_USER"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].orderNumber", is("O1")));
  }

  @Test
  void getOrderById_withUserAuth_shouldReturnOrder() throws Exception {
    OrderResponse response =
        OrderResponse.builder()
            .id(1L)
            .orderNumber("O1")
            .status(OrderStatus.CONFIRMED)
            .totalAmount(BigDecimal.TEN)
            .build();
    when(orderService.getOrderById(eq(1L), eq("testuser"), eq(false))).thenReturn(response);

    mockMvc
        .perform(
            get("/api/v1/orders/1")
                .with(
                    jwt()
                        .jwt(
                            j ->
                                j.claim("aud", List.of("shop-api"))
                                    .claim("preferred_username", "testuser"))
                        .authorities(new SimpleGrantedAuthority("ROLE_USER"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.orderNumber", is("O1")));
  }
}
