package com.shop.catalog.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.shop.catalog.dto.ProductRequest;
import com.shop.catalog.dto.ProductResponse;
import com.shop.catalog.entity.Product;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class ProductMapperTest {

  private ProductMapper mapper;

  @BeforeEach
  void setUp() {
    mapper = Mappers.getMapper(ProductMapper.class);
  }

  @Test
  void toResponse_shouldMapAllFieldsCorrectly() {
    Product product =
        Product.builder()
            .id(1L)
            .name("Test Product")
            .sku("SKU-100")
            .description("Test Description")
            .price(BigDecimal.valueOf(99.99))
            .stock(20)
            .version(0L)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

    ProductResponse response = mapper.toResponse(product);

    assertThat(response).isNotNull();
    assertThat(response.getId()).isEqualTo(1L);
    assertThat(response.getName()).isEqualTo("Test Product");
    assertThat(response.getSku()).isEqualTo("SKU-100");
    assertThat(response.getPrice()).isEqualTo(BigDecimal.valueOf(99.99));
    assertThat(response.getStock()).isEqualTo(20);
  }

  @Test
  void toEntity_shouldMapRequestFields() {
    ProductRequest request =
        ProductRequest.builder()
            .name("New Item")
            .sku("SKU-200")
            .description("Desc")
            .price(BigDecimal.valueOf(14.50))
            .stock(10)
            .build();

    Product entity = mapper.toEntity(request);

    assertThat(entity).isNotNull();
    assertThat(entity.getName()).isEqualTo("New Item");
    assertThat(entity.getSku()).isEqualTo("SKU-200");
    assertThat(entity.getPrice()).isEqualTo(BigDecimal.valueOf(14.50));
    assertThat(entity.getStock()).isEqualTo(10);
  }

  @Test
  void updateEntityFromRequest_shouldUpdateFields() {
    Product product =
        Product.builder()
            .id(5L)
            .name("Old Name")
            .sku("SKU-OLD")
            .price(BigDecimal.TEN)
            .stock(5)
            .build();

    ProductRequest request =
        ProductRequest.builder()
            .name("Updated Name")
            .sku("SKU-NEW")
            .price(BigDecimal.valueOf(25.00))
            .stock(15)
            .build();

    mapper.updateEntityFromRequest(request, product);

    assertThat(product.getId()).isEqualTo(5L);
    assertThat(product.getName()).isEqualTo("Updated Name");
    assertThat(product.getSku()).isEqualTo("SKU-NEW");
    assertThat(product.getPrice()).isEqualTo(BigDecimal.valueOf(25.00));
    assertThat(product.getStock()).isEqualTo(15);
  }

  @Test
  void toResponseList_shouldMapAllItems() {
    Product p1 =
        Product.builder().id(1L).name("P1").sku("S1").price(BigDecimal.ONE).stock(1).build();
    Product p2 =
        Product.builder().id(2L).name("P2").sku("S2").price(BigDecimal.TEN).stock(2).build();

    List<ProductResponse> responses = mapper.toResponseList(List.of(p1, p2));

    assertThat(responses).hasSize(2);
    assertThat(responses.get(0).getName()).isEqualTo("P1");
    assertThat(responses.get(1).getName()).isEqualTo("P2");
  }
}
