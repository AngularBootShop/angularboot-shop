package com.shop.catalog.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.shop.catalog.dto.ProductRequest;
import com.shop.catalog.dto.ProductResponse;
import com.shop.catalog.entity.Product;
import com.shop.catalog.exception.InsufficientStockException;
import com.shop.catalog.repository.ProductRepository;
import com.shop.catalog.service.ProductService;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
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
class ProductIntegrationTest {

  @Container
  static PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>("postgres:16-alpine")
          .withDatabaseName("test_catalog_db")
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

  @Autowired private ProductService productService;

  @Autowired private ProductRepository productRepository;

  @Test
  void productLifecycle_andOptimisticLocking_shouldSucceed() {
    // 1. Create a product
    ProductRequest createReq =
        ProductRequest.builder()
            .name("Integration Test Tablet")
            .sku("TAB-INT-999")
            .description("Tablet for integration testing")
            .price(BigDecimal.valueOf(399.99))
            .stock(20)
            .build();

    ProductResponse created = productService.createProduct(createReq);
    assertThat(created.getId()).isNotNull();
    assertThat(created.getVersion()).isEqualTo(0L);

    // 2. Deduct stock and verify version increment
    ProductResponse deducted = productService.deductStock(created.getId(), 5);
    assertThat(deducted.getStock()).isEqualTo(15);

    Product refreshed = productRepository.findById(created.getId()).orElseThrow();
    assertThat(refreshed.getVersion()).isGreaterThan(0L);

    // 3. Compensating restore stock
    ProductResponse restored = productService.restoreStock(created.getId(), 5);
    assertThat(restored.getStock()).isEqualTo(20);

    // 4. Test insufficient stock rejection
    assertThatThrownBy(() -> productService.deductStock(created.getId(), 25))
        .isInstanceOf(InsufficientStockException.class);
  }
}
