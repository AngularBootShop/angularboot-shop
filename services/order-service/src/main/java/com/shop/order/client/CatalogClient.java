package com.shop.order.client;

import com.shop.order.dto.ProductDto;

public interface CatalogClient {

  ProductDto getProduct(Long id);

  ProductDto deductStock(Long id, int quantity);

  ProductDto restoreStock(Long id, int quantity);
}
