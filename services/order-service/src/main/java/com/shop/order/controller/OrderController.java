package com.shop.order.controller;

import com.shop.order.dto.OrderCreateRequest;
import com.shop.order.dto.OrderResponse;
import com.shop.order.dto.PagedResponse;
import com.shop.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Slf4j
@Tag(
    name = "Order Management",
    description = "Endpoints for creating and retrieving orders with idempotency guarantees")
public class OrderController {

  private final OrderService orderService;

  @PostMapping
  @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
  @SecurityRequirement(name = "bearerAuth")
  @Operation(summary = "Submit a new order with Idempotency-Key")
  public ResponseEntity<OrderResponse> createOrder(
      @RequestHeader("Idempotency-Key") String idempotencyKey,
      @Valid @RequestBody OrderCreateRequest request,
      Authentication authentication) {
    String userId = extractUserId(authentication);
    log.info("POST /api/v1/orders - User: {}, Idempotency-Key: {}", userId, idempotencyKey);
    OrderResponse created = orderService.createOrder(idempotencyKey, userId, request);
    URI location = URI.create(String.format("/api/v1/orders/%d", created.getId()));
    return ResponseEntity.created(location).body(created);
  }

  @GetMapping
  @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
  @SecurityRequirement(name = "bearerAuth")
  @Operation(summary = "List orders (filtered by current user or all if Admin)")
  public ResponseEntity<PagedResponse<OrderResponse>> getOrders(
      Authentication authentication,
      @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
          Pageable pageable) {
    String userId = extractUserId(authentication);
    boolean isAdmin = hasRole(authentication, "ROLE_ADMIN");
    log.debug("GET /api/v1/orders - User: {}, isAdmin: {}", userId, isAdmin);
    return ResponseEntity.ok(orderService.getOrders(userId, isAdmin, pageable));
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
  @SecurityRequirement(name = "bearerAuth")
  @Operation(summary = "Get order by ID")
  public ResponseEntity<OrderResponse> getOrderById(
      @PathVariable Long id, Authentication authentication) {
    String userId = extractUserId(authentication);
    boolean isAdmin = hasRole(authentication, "ROLE_ADMIN");
    log.debug("GET /api/v1/orders/{} - User: {}", id, userId);
    return ResponseEntity.ok(orderService.getOrderById(id, userId, isAdmin));
  }

  private String extractUserId(Authentication authentication) {
    if (authentication == null) {
      return "anonymous";
    }
    if (authentication.getPrincipal() instanceof Jwt jwt) {
      String preferredUsername = jwt.getClaimAsString("preferred_username");
      if (preferredUsername != null && !preferredUsername.isBlank()) {
        return preferredUsername;
      }
      return jwt.getSubject() != null ? jwt.getSubject() : "customer-user";
    }
    return authentication.getName() != null ? authentication.getName() : "customer-user";
  }

  private boolean hasRole(Authentication authentication, String role) {
    if (authentication == null) {
      return false;
    }
    return authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(role));
  }
}
