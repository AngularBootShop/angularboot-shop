package com.shop.catalog.dto;

import java.math.BigDecimal;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {

  private Long id;
  private String name;
  private String sku;
  private String description;
  private BigDecimal price;
  private Integer stock;
  private Long version;
  private Instant createdAt;
  private Instant updatedAt;
}
