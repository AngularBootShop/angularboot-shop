package com.shop.catalog.controller;

import com.shop.catalog.dto.PagedResponse;
import com.shop.catalog.dto.ProductRequest;
import com.shop.catalog.dto.ProductResponse;
import com.shop.catalog.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Product Catalog", description = "Endpoints for browsing and managing products")
public class ProductController {

  private final ProductService productService;

  @GetMapping
  @Operation(summary = "Browse products with pagination, sorting, and filtering")
  public ResponseEntity<PagedResponse<ProductResponse>> getProducts(
      @RequestParam(required = false) String query,
      @RequestParam(required = false) BigDecimal minPrice,
      @RequestParam(required = false) BigDecimal maxPrice,
      @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
          Pageable pageable) {
    log.debug(
        "GET /api/v1/products - query: '{}', minPrice: {}, maxPrice: {}",
        query,
        minPrice,
        maxPrice);
    return ResponseEntity.ok(productService.getProducts(query, minPrice, maxPrice, pageable));
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get a single product by ID")
  public ResponseEntity<ProductResponse> getProductById(@PathVariable Long id) {
    log.debug("GET /api/v1/products/{}", id);
    return ResponseEntity.ok(productService.getProductById(id));
  }

  @GetMapping("/sku/{sku}")
  @Operation(summary = "Get a single product by SKU")
  public ResponseEntity<ProductResponse> getProductBySku(@PathVariable String sku) {
    log.debug("GET /api/v1/products/sku/{}", sku);
    return ResponseEntity.ok(productService.getProductBySku(sku));
  }

  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  @SecurityRequirement(name = "bearerAuth")
  @Operation(summary = "Create a new product (Admin only)")
  public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody ProductRequest request) {
    log.info("POST /api/v1/products - SKU: {}", request.getSku());
    ProductResponse created = productService.createProduct(request);
    URI location = URI.create(String.format("/api/v1/products/%d", created.getId()));
    return ResponseEntity.created(location).body(created);
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  @SecurityRequirement(name = "bearerAuth")
  @Operation(summary = "Update an existing product (Admin only)")
  public ResponseEntity<ProductResponse> updateProduct(
      @PathVariable Long id, @Valid @RequestBody ProductRequest request) {
    log.info("PUT /api/v1/products/{}", id);
    return ResponseEntity.ok(productService.updateProduct(id, request));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  @SecurityRequirement(name = "bearerAuth")
  @Operation(summary = "Delete a product by ID (Admin only)")
  public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
    log.info("DELETE /api/v1/products/{}", id);
    productService.deleteProduct(id);
    return ResponseEntity.noContent().build();
  }
}
