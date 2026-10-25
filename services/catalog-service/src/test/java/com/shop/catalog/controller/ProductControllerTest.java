package com.shop.catalog.controller;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.catalog.config.SecurityConfig;
import com.shop.catalog.dto.PagedResponse;
import com.shop.catalog.dto.ProductRequest;
import com.shop.catalog.dto.ProductResponse;
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

@WebMvcTest(ProductController.class)
@Import(SecurityConfig.class)
class ProductControllerTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private ProductService productService;

  @MockitoBean private JwtDecoder jwtDecoder;

  @Test
  void getProducts_isPubliclyAccessible() throws Exception {
    PagedResponse<ProductResponse> pagedResponse =
        PagedResponse.<ProductResponse>builder()
            .content(
                List.of(
                    ProductResponse.builder()
                        .id(1L)
                        .name("Gadget")
                        .price(BigDecimal.TEN)
                        .stock(5)
                        .build()))
            .totalElements(1)
            .totalPages(1)
            .pageNumber(0)
            .pageSize(10)
            .isLast(true)
            .build();

    when(productService.getProducts(any(), any(), any(), any())).thenReturn(pagedResponse);

    mockMvc
        .perform(get("/api/v1/products"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].name", is("Gadget")));
  }

  @Test
  void getProductById_isPubliclyAccessible() throws Exception {
    ProductResponse response =
        ProductResponse.builder().id(1L).name("Gadget").price(BigDecimal.TEN).stock(5).build();
    when(productService.getProductById(1L)).thenReturn(response);

    mockMvc
        .perform(get("/api/v1/products/1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name", is("Gadget")));
  }

  @Test
  void createProduct_withAdminRole_shouldCreateProduct() throws Exception {
    ProductRequest request =
        ProductRequest.builder()
            .name("Ultra Phone")
            .sku("PHONE-001")
            .description("Latest smartphone")
            .price(BigDecimal.valueOf(899.99))
            .stock(15)
            .build();

    ProductResponse response =
        ProductResponse.builder()
            .id(10L)
            .name("Ultra Phone")
            .sku("PHONE-001")
            .price(BigDecimal.valueOf(899.99))
            .stock(15)
            .build();

    when(productService.createProduct(any(ProductRequest.class))).thenReturn(response);

    mockMvc
        .perform(
            post("/api/v1/products")
                .with(
                    jwt()
                        .jwt(j -> j.claim("aud", List.of("shop-api")))
                        .authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id", is(10)))
        .andExpect(jsonPath("$.name", is("Ultra Phone")));
  }

  @Test
  void createProduct_withUserRole_shouldReturnForbidden() throws Exception {
    ProductRequest request =
        ProductRequest.builder()
            .name("Ultra Phone")
            .sku("PHONE-001")
            .price(BigDecimal.valueOf(899.99))
            .stock(15)
            .build();

    mockMvc
        .perform(
            post("/api/v1/products")
                .with(
                    jwt()
                        .jwt(j -> j.claim("aud", List.of("shop-api")))
                        .authorities(new SimpleGrantedAuthority("ROLE_USER")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isForbidden());
  }

  @Test
  void createProduct_withoutAuth_shouldReturnUnauthorized() throws Exception {
    ProductRequest request =
        ProductRequest.builder()
            .name("Ultra Phone")
            .sku("PHONE-001")
            .price(BigDecimal.valueOf(899.99))
            .stock(15)
            .build();

    mockMvc
        .perform(
            post("/api/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void createProduct_withValidationErrors_shouldReturnBadRequestProblemDetail() throws Exception {
    ProductRequest invalidRequest =
        ProductRequest.builder()
            .name("") // Blank name
            .sku("") // Blank sku
            .price(BigDecimal.valueOf(-5.0)) // Negative price
            .stock(-1) // Negative stock
            .build();

    mockMvc
        .perform(
            post("/api/v1/products")
                .with(
                    jwt()
                        .jwt(j -> j.claim("aud", List.of("shop-api")))
                        .authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.title", is("Constraint Violation")))
        .andExpect(jsonPath("$.errorCode", is("VALIDATION_FAILED")))
        .andExpect(jsonPath("$.fieldErrors.name").exists())
        .andExpect(jsonPath("$.fieldErrors.price").exists());
  }

  @Test
  void updateProduct_withAdminRole_shouldUpdate() throws Exception {
    ProductRequest request =
        ProductRequest.builder()
            .name("Updated Phone")
            .sku("PHONE-001")
            .price(BigDecimal.valueOf(799.99))
            .stock(10)
            .build();

    ProductResponse response =
        ProductResponse.builder()
            .id(1L)
            .name("Updated Phone")
            .sku("PHONE-001")
            .price(BigDecimal.valueOf(799.99))
            .stock(10)
            .build();

    when(productService.updateProduct(eq(1L), any(ProductRequest.class))).thenReturn(response);

    mockMvc
        .perform(
            put("/api/v1/products/1")
                .with(
                    jwt()
                        .jwt(j -> j.claim("aud", List.of("shop-api")))
                        .authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name", is("Updated Phone")));
  }

  @Test
  void deleteProduct_withAdminRole_shouldReturnNoContent() throws Exception {
    mockMvc
        .perform(
            delete("/api/v1/products/1")
                .with(
                    jwt()
                        .jwt(j -> j.claim("aud", List.of("shop-api")))
                        .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
        .andExpect(status().isNoContent());
  }
}
