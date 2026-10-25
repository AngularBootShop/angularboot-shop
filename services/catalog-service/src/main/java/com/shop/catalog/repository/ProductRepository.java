package com.shop.catalog.repository;

import com.shop.catalog.entity.Product;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository
    extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

  Optional<Product> findBySku(String sku);

  boolean existsBySku(String sku);

  boolean existsBySkuAndIdNot(String sku, Long id);

  @Query(
      "SELECT p FROM Product p WHERE "
          + "(CAST(:query AS string) IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', CAST(:query AS string), '%')) OR LOWER(p.sku) LIKE LOWER(CONCAT('%', CAST(:query AS string), '%'))) AND "
          + "(CAST(:minPrice AS big_decimal) IS NULL OR p.price >= :minPrice) AND "
          + "(CAST(:maxPrice AS big_decimal) IS NULL OR p.price <= :maxPrice)")
  Page<Product> searchProducts(
      @Param("query") String query,
      @Param("minPrice") java.math.BigDecimal minPrice,
      @Param("maxPrice") java.math.BigDecimal maxPrice,
      Pageable pageable);
}
