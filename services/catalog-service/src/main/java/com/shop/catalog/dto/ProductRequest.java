package com.shop.catalog.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductRequest {

  @NotBlank(message = "Product name is required")
  @Size(max = 255, message = "Product name cannot exceed 255 characters")
  private String name;

  @NotBlank(message = "Product SKU is required")
  @Size(max = 64, message = "Product SKU cannot exceed 64 characters")
  private String sku;

  private String description;

  @NotNull(message = "Price is required")
  @DecimalMin(value = "0.0", inclusive = true, message = "Price must be non-negative")
  private BigDecimal price;

  @NotNull(message = "Stock is required")
  @Min(value = 0, message = "Stock must be non-negative")
  private Integer stock;
}
