package com.shop.catalog.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

  @Mock private ProductRepository productRepository;

  private ProductMapper productMapper;
  private ProductService productService;

  @BeforeEach
  void setUp() {
    productMapper = Mappers.getMapper(ProductMapper.class);
    productService = new ProductServiceImpl(productRepository, productMapper);
  }

  @Test
  void getProducts_shouldReturnPagedResponse() {
    Pageable pageable = PageRequest.of(0, 10);
    Product product =
        Product.builder().id(1L).name("Gadget").sku("SKU-1").price(BigDecimal.TEN).stock(5).build();
    Page<Product> page = new PageImpl<>(List.of(product), pageable, 1);

    when(productRepository.searchProducts("Gadget", null, null, pageable)).thenReturn(page);

    PagedResponse<ProductResponse> result =
        productService.getProducts("Gadget", null, null, pageable);

    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getTotalElements()).isEqualTo(1);
    assertThat(result.getContent().get(0).getName()).isEqualTo("Gadget");
  }

  @Test
  void getProductById_whenExists_shouldReturnProduct() {
    Product product =
        Product.builder().id(1L).name("Gadget").sku("SKU-1").price(BigDecimal.TEN).stock(5).build();
    when(productRepository.findById(1L)).thenReturn(Optional.of(product));

    ProductResponse response = productService.getProductById(1L);

    assertThat(response).isNotNull();
    assertThat(response.getId()).isEqualTo(1L);
  }

  @Test
  void getProductById_whenNotFound_shouldThrowException() {
    when(productRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> productService.getProductById(99L))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("Product with identifier 99 was not found");
  }

  @Test
  void getProductBySku_whenExists_shouldReturnProduct() {
    Product product =
        Product.builder().id(1L).name("Gadget").sku("SKU-1").price(BigDecimal.TEN).stock(5).build();
    when(productRepository.findBySku("SKU-1")).thenReturn(Optional.of(product));

    ProductResponse response = productService.getProductBySku("SKU-1");

    assertThat(response.getSku()).isEqualTo("SKU-1");
  }

  @Test
  void createProduct_whenSkuUnique_shouldSaveAndReturn() {
    ProductRequest request =
        ProductRequest.builder()
            .name("Keyboard")
            .sku("KEY-1")
            .price(BigDecimal.valueOf(50))
            .stock(10)
            .build();

    when(productRepository.existsBySku("KEY-1")).thenReturn(false);
    when(productRepository.save(any(Product.class)))
        .thenAnswer(
            invocation -> {
              Product p = invocation.getArgument(0);
              p.setId(10L);
              return p;
            });

    ProductResponse response = productService.createProduct(request);

    assertThat(response.getId()).isEqualTo(10L);
    assertThat(response.getName()).isEqualTo("Keyboard");
  }

  @Test
  void createProduct_whenSkuDuplicate_shouldThrowException() {
    ProductRequest request = ProductRequest.builder().sku("KEY-1").build();
    when(productRepository.existsBySku("KEY-1")).thenReturn(true);

    assertThatThrownBy(() -> productService.createProduct(request))
        .isInstanceOf(DuplicateResourceException.class)
        .hasMessageContaining("already exists");
  }

  @Test
  void updateProduct_whenExistsAndSkuValid_shouldUpdate() {
    Product existing =
        Product.builder().id(1L).name("Old").sku("SKU-1").price(BigDecimal.ONE).stock(1).build();
    ProductRequest request =
        ProductRequest.builder().name("New").sku("SKU-1").price(BigDecimal.TEN).stock(5).build();

    when(productRepository.findById(1L)).thenReturn(Optional.of(existing));
    when(productRepository.existsBySkuAndIdNot("SKU-1", 1L)).thenReturn(false);
    when(productRepository.save(any(Product.class))).thenReturn(existing);

    ProductResponse response = productService.updateProduct(1L, request);

    assertThat(response.getName()).isEqualTo("New");
    assertThat(response.getPrice()).isEqualTo(BigDecimal.TEN);
  }

  @Test
  void deleteProduct_whenExists_shouldDelete() {
    when(productRepository.existsById(1L)).thenReturn(true);

    productService.deleteProduct(1L);

    verify(productRepository).deleteById(1L);
  }

  @Test
  void deductStock_whenSufficient_shouldDeductAndReturn() {
    Product product =
        Product.builder()
            .id(1L)
            .name("Monitor")
            .sku("MON-1")
            .price(BigDecimal.valueOf(100))
            .stock(10)
            .build();
    when(productRepository.findById(1L)).thenReturn(Optional.of(product));
    when(productRepository.saveAndFlush(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

    ProductResponse response = productService.deductStock(1L, 3);

    assertThat(response.getStock()).isEqualTo(7);
  }

  @Test
  void deductStock_whenInsufficient_shouldThrowException() {
    Product product =
        Product.builder()
            .id(1L)
            .name("Monitor")
            .sku("MON-1")
            .price(BigDecimal.valueOf(100))
            .stock(2)
            .build();
    when(productRepository.findById(1L)).thenReturn(Optional.of(product));

    assertThatThrownBy(() -> productService.deductStock(1L, 5))
        .isInstanceOf(InsufficientStockException.class)
        .hasMessageContaining("Requested: 5, Available: 2");
  }

  @Test
  void deductStock_whenOptimisticLockConflict_shouldThrowOptimisticLockingConflictException() {
    Product product =
        Product.builder()
            .id(1L)
            .name("Monitor")
            .sku("MON-1")
            .price(BigDecimal.valueOf(100))
            .stock(10)
            .build();
    when(productRepository.findById(1L)).thenReturn(Optional.of(product));
    when(productRepository.saveAndFlush(any(Product.class)))
        .thenThrow(new OptimisticLockingFailureException("Version conflict"));

    assertThatThrownBy(() -> productService.deductStock(1L, 2))
        .isInstanceOf(OptimisticLockingConflictException.class)
        .hasMessageContaining("Concurrent modification");
  }

  @Test
  void restoreStock_shouldIncreaseStock() {
    Product product =
        Product.builder()
            .id(1L)
            .name("Monitor")
            .sku("MON-1")
            .price(BigDecimal.valueOf(100))
            .stock(7)
            .build();
    when(productRepository.findById(1L)).thenReturn(Optional.of(product));
    when(productRepository.saveAndFlush(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

    ProductResponse response = productService.restoreStock(1L, 3);

    assertThat(response.getStock()).isEqualTo(10);
  }
}
