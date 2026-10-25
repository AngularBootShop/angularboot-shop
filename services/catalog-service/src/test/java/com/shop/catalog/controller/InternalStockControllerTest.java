package com.shop.catalog.controller;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.catalog.config.SecurityConfig;
import com.shop.catalog.dto.ProductResponse;
import com.shop.catalog.dto.StockDeductRequest;
import com.shop.catalog.dto.StockRestoreRequest;
import com.shop.catalog.service.ProductService;
import java.math.BigDecimal;
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

@WebMvcTest(InternalStockController.class)
@Import(SecurityConfig.class)
class InternalStockControllerTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private ProductService productService;

  @MockitoBean private JwtDecoder jwtDecoder;

  @Test
  void deductStock_withServiceRole_shouldSucceed() throws Exception {
    StockDeductRequest request = new StockDeductRequest(2);
    ProductResponse response =
        ProductResponse.builder()
            .id(1L)
            .name("Keyboard")
            .sku("KEY-1")
            .price(BigDecimal.valueOf(50))
            .stock(8)
            .build();

    when(productService.deductStock(eq(1L), eq(2))).thenReturn(response);

    mockMvc
        .perform(
            post("/api/v1/internal/products/1/deduct-stock")
                .with(
                    jwt()
                        .jwt(j -> j.claim("aud", List.of("shop-api")))
                        .authorities(new SimpleGrantedAuthority("ROLE_SERVICE")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.stock", is(8)));
  }

  @Test
  void deductStock_withUserRole_shouldReturnForbidden() throws Exception {
    StockDeductRequest request = new StockDeductRequest(2);

    mockMvc
        .perform(
            post("/api/v1/internal/products/1/deduct-stock")
                .with(
                    jwt()
                        .jwt(j -> j.claim("aud", List.of("shop-api")))
                        .authorities(new SimpleGrantedAuthority("ROLE_USER")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isForbidden());
  }

  @Test
  void deductStock_withAdminRole_shouldReturnForbidden() throws Exception {
    StockDeductRequest request = new StockDeductRequest(2);

    mockMvc
        .perform(
            post("/api/v1/internal/products/1/deduct-stock")
                .with(
                    jwt()
                        .jwt(j -> j.claim("aud", List.of("shop-api")))
                        .authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isForbidden());
  }

  @Test
  void deductStock_withoutAuth_shouldReturnUnauthorized() throws Exception {
    StockDeductRequest request = new StockDeductRequest(2);

    mockMvc
        .perform(
            post("/api/v1/internal/products/1/deduct-stock")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void restoreStock_withServiceRole_shouldSucceed() throws Exception {
    StockRestoreRequest request = new StockRestoreRequest(2);
    ProductResponse response =
        ProductResponse.builder()
            .id(1L)
            .name("Keyboard")
            .sku("KEY-1")
            .price(BigDecimal.valueOf(50))
            .stock(10)
            .build();

    when(productService.restoreStock(eq(1L), eq(2))).thenReturn(response);

    mockMvc
        .perform(
            post("/api/v1/internal/products/1/restore-stock")
                .with(
                    jwt()
                        .jwt(j -> j.claim("aud", List.of("shop-api")))
                        .authorities(new SimpleGrantedAuthority("ROLE_SERVICE")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.stock", is(10)));
  }
}
