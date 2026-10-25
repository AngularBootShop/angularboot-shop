package com.shop.catalog.service;

import com.shop.catalog.dto.PagedResponse;
import com.shop.catalog.dto.ProductRequest;
import com.shop.catalog.dto.ProductResponse;
import com.shop.catalog.entity.Product;
import com.shop.catalog.exception.DuplicateResourceException;
import com.shop.catalog.exception.InsufficientStockException;
import com.shop.catalog.exception.OptimisticLockingConflictException;
import com.shop.catalog.exception.ResourceNotFoundException;
import com.shop.catalog.mapper.ProductMapper;
import com.shop.catalog.repository.ProductRepository;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ProductServiceImpl implements ProductService {

  private final ProductRepository productRepository;
  private final ProductMapper productMapper;

  @Override
  @Transactional(readOnly = true)
  public PagedResponse<ProductResponse> getProducts(
      String query, BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable) {
    log.debug(
        "Fetching products with query: '{}', minPrice: {}, maxPrice: {}, page: {}",
        query,
        minPrice,
        maxPrice,
        pageable);
    String cleanQuery = (query != null && !query.isBlank()) ? query.trim() : null;
    Page<Product> page;
    if (cleanQuery == null && minPrice == null && maxPrice == null) {
      page = productRepository.findAll(pageable);
    } else {
      page = productRepository.searchProducts(cleanQuery, minPrice, maxPrice, pageable);
    }
    return PagedResponse.from(page.map(productMapper::toResponse));
  }

  @Override
  @Transactional(readOnly = true)
  public ProductResponse getProductById(Long id) {
    log.debug("Fetching product by ID: {}", id);
    Product product =
        productRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product", id));
    return productMapper.toResponse(product);
  }

  @Override
  @Transactional(readOnly = true)
  public ProductResponse getProductBySku(String sku) {
    log.debug("Fetching product by SKU: {}", sku);
    Product product =
        productRepository
            .findBySku(sku)
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        String.format("Product with SKU '%s' not found", sku)));
    return productMapper.toResponse(product);
  }

  @Override
  public ProductResponse createProduct(ProductRequest request) {
    log.info("Creating new product with SKU: {}", request.getSku());
    if (productRepository.existsBySku(request.getSku())) {
      throw new DuplicateResourceException(
          String.format("Product with SKU '%s' already exists", request.getSku()));
    }

    Product product = productMapper.toEntity(request);
    Product saved = productRepository.save(product);
    log.info("Product created successfully with ID: {}", saved.getId());
    return productMapper.toResponse(saved);
  }

  @Override
  public ProductResponse updateProduct(Long id, ProductRequest request) {
    log.info("Updating product ID: {}", id);
    Product product =
        productRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product", id));

    if (productRepository.existsBySkuAndIdNot(request.getSku(), id)) {
      throw new DuplicateResourceException(
          String.format("Product with SKU '%s' already exists on another item", request.getSku()));
    }

    productMapper.updateEntityFromRequest(request, product);
    Product updated = productRepository.save(product);
    log.info("Product ID: {} updated successfully", updated.getId());
    return productMapper.toResponse(updated);
  }

  @Override
  public void deleteProduct(Long id) {
    log.info("Deleting product ID: {}", id);
    if (!productRepository.existsById(id)) {
      throw new ResourceNotFoundException("Product", id);
    }
    productRepository.deleteById(id);
    log.info("Product ID: {} deleted successfully", id);
  }

  @Override
  public ProductResponse deductStock(Long id, int quantity) {
    log.info("Attempting to deduct {} units for product ID: {}", quantity, id);
    try {
      Product product =
          productRepository
              .findById(id)
              .orElseThrow(() -> new ResourceNotFoundException("Product", id));

      if (product.getStock() < quantity) {
        log.warn(
            "Stock deduction failed: Insufficient stock for product ID {}. Available: {}, Requested: {}",
            id,
            product.getStock(),
            quantity);
        throw new InsufficientStockException(id, quantity, product.getStock());
      }

      product.setStock(product.getStock() - quantity);
      Product saved = productRepository.saveAndFlush(product);
      log.info(
          "Deducted {} units for product ID: {}. Remaining stock: {}",
          quantity,
          id,
          saved.getStock());
      return productMapper.toResponse(saved);
    } catch (OptimisticLockingFailureException ex) {
      log.warn("Optimistic lock conflict when deducting stock for product ID: {}", id);
      throw new OptimisticLockingConflictException(
          String.format("Concurrent modification on product ID: %d. Please retry.", id));
    }
  }

  @Override
  public ProductResponse restoreStock(Long id, int quantity) {
    log.info("Compensating action: Restoring {} units for product ID: {}", quantity, id);
    try {
      Product product =
          productRepository
              .findById(id)
              .orElseThrow(() -> new ResourceNotFoundException("Product", id));

      product.setStock(product.getStock() + quantity);
      Product saved = productRepository.saveAndFlush(product);
      log.info(
          "Restored {} units for product ID: {}. New stock: {}", quantity, id, saved.getStock());
      return productMapper.toResponse(saved);
    } catch (OptimisticLockingFailureException ex) {
      log.warn("Optimistic lock conflict when restoring stock for product ID: {}", id);
      throw new OptimisticLockingConflictException(
          String.format(
              "Concurrent modification on product ID: %d during stock restore. Please retry.", id));
    }
  }
}
