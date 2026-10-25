package com.shop.catalog.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockDeductRequest {

  @NotNull(message = "Quantity is required")
  @Min(value = 1, message = "Deduct quantity must be at least 1")
  private Integer quantity;
}
