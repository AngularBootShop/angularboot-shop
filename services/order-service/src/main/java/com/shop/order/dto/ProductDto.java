package com.shop.order.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductDto {

  private Long id;
  private String name;
  private String sku;
  private String description;
  private BigDecimal price;
  private Integer stock;
  private Long version;
}
