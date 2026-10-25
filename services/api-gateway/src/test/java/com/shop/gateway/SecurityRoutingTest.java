package com.shop.gateway;

import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt;

import com.shop.gateway.config.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@WebFluxTest
@Import({SecurityConfig.class, SecurityRoutingTest.TestEndpointController.class})
@ActiveProfiles("test")
class SecurityRoutingTest {

  @Configuration
  @RestController
  static class TestEndpointController {
    @GetMapping("/actuator/health")
    public Mono<ResponseEntity<String>> health() {
      return Mono.just(ResponseEntity.ok("{\"status\":\"UP\"}"));
    }

    @GetMapping("/api/v1/products")
    public Mono<ResponseEntity<String>> getProducts() {
      return Mono.just(ResponseEntity.ok("[]"));
    }

    @DeleteMapping("/api/v1/products/{id}")
    public Mono<ResponseEntity<Void>> deleteProduct(@PathVariable Long id) {
      return Mono.just(ResponseEntity.noContent().build());
    }

    @GetMapping("/api/v1/orders")
    public Mono<ResponseEntity<String>> getOrders() {
      return Mono.just(ResponseEntity.ok("[]"));
    }

    @GetMapping("/api/v1/internal/products/{id}/deduct-stock")
    public Mono<ResponseEntity<Void>> internalDeduct(@PathVariable Long id) {
      return Mono.just(ResponseEntity.ok().build());
    }
  }

  @Autowired private WebTestClient webTestClient;

  @MockitoBean private ReactiveJwtDecoder reactiveJwtDecoder;

  @Test
  @DisplayName("Actuator health endpoint should be publicly accessible")
  void actuatorShouldBePermitAll() {
    webTestClient.get().uri("/actuator/health").exchange().expectStatus().isOk();
  }

  @Test
  @DisplayName("Public product listing should allow unauthenticated access")
  void publicProductListingPermitAll() {
    webTestClient.get().uri("/api/v1/products").exchange().expectStatus().isOk();
  }

  @Test
  @DisplayName("Internal endpoints should be strictly denied (403) for authenticated users")
  void internalEndpointsShouldBeDeniedForAuthenticatedUsers() {
    webTestClient
        .mutateWith(mockJwt().authorities(new SimpleGrantedAuthority("ROLE_USER")))
        .get()
        .uri("/api/v1/internal/products/1/deduct-stock")
        .exchange()
        .expectStatus()
        .isForbidden();
  }

  @Test
  @DisplayName("Order endpoints should require authentication (401 when anonymous)")
  void orderEndpointsRequireAuth() {
    webTestClient.get().uri("/api/v1/orders").exchange().expectStatus().isUnauthorized();
  }

  @Test
  @DisplayName("Admin role should access product modification")
  void adminCanModifyProducts() {
    webTestClient
        .mutateWith(mockJwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
        .delete()
        .uri("/api/v1/products/1")
        .exchange()
        .expectStatus()
        .isNoContent();
  }

  @Test
  @DisplayName("Regular user cannot modify products (403 Forbidden)")
  void regularUserCannotModifyProducts() {
    webTestClient
        .mutateWith(mockJwt().authorities(new SimpleGrantedAuthority("ROLE_USER")))
        .delete()
        .uri("/api/v1/products/1")
        .exchange()
        .expectStatus()
        .isForbidden();
  }
}
