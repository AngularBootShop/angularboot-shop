package com.shop.catalog.service;

import com.shop.catalog.dto.PagedResponse;
import com.shop.catalog.dto.ProductRequest;
import com.shop.catalog.dto.ProductResponse;
import java.math.BigDecimal;
import org.springframework.data.domain.Pageable;

public interface ProductService {

  PagedResponse<ProductResponse> getProducts(
      String query, BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);

  ProductResponse getProductById(Long id);

  ProductResponse getProductBySku(String sku);

  ProductResponse createProduct(ProductRequest request);

  ProductResponse updateProduct(Long id, ProductRequest request);

  void deleteProduct(Long id);

  ProductResponse deductStock(Long id, int quantity);

  ProductResponse restoreStock(Long id, int quantity);
}
