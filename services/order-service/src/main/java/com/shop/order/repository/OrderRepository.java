package com.shop.order.repository;

import com.shop.order.entity.Order;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

  @EntityGraph(attributePaths = {"items"})
  Optional<Order> findWithItemsById(Long id);

  @EntityGraph(attributePaths = {"items"})
  Optional<Order> findWithItemsByOrderNumber(String orderNumber);

  Page<Order> findByUserId(String userId, Pageable pageable);
}
