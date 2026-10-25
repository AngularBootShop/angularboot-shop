package com.shop.catalog.controller;

import com.shop.catalog.dto.ProductResponse;
import com.shop.catalog.dto.StockDeductRequest;
import com.shop.catalog.dto.StockRestoreRequest;
import com.shop.catalog.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/internal/products")
@RequiredArgsConstructor
@Slf4j
@Tag(
    name = "Internal Inventory Management",
    description = "Service-to-service endpoints restricted to microservices with ROLE_SERVICE")
public class InternalStockController {

  private final ProductService productService;

  @PostMapping("/{id}/deduct-stock")
  @PreAuthorize("hasRole('SERVICE')")
  @SecurityRequirement(name = "bearerAuth")
  @Operation(summary = "Internal: Deduct inventory for product (Requires ROLE_SERVICE)")
  public ResponseEntity<ProductResponse> deductStock(
      @PathVariable Long id, @Valid @RequestBody StockDeductRequest request) {
    log.info(
        "INTERNAL: Deduct stock request for product ID: {}, quantity: {}",
        id,
        request.getQuantity());
    return ResponseEntity.ok(productService.deductStock(id, request.getQuantity()));
  }

  @PostMapping("/{id}/restore-stock")
  @PreAuthorize("hasRole('SERVICE')")
  @SecurityRequirement(name = "bearerAuth")
  @Operation(summary = "Internal: Compensating action to restore stock (Requires ROLE_SERVICE)")
  public ResponseEntity<ProductResponse> restoreStock(
      @PathVariable Long id, @Valid @RequestBody StockRestoreRequest request) {
    log.info(
        "INTERNAL: Restore stock compensating request for product ID: {}, quantity: {}",
        id,
        request.getQuantity());
    return ResponseEntity.ok(productService.restoreStock(id, request.getQuantity()));
  }
}
