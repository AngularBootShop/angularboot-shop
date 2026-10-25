package com.shop.gateway.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

class KeycloakJwtAuthenticationConverterTest {

  private final SecurityConfig.KeycloakJwtAuthenticationConverter converter =
      new SecurityConfig.KeycloakJwtAuthenticationConverter();

  @Test
  @DisplayName("Should extract realm roles into Spring Security authorities with ROLE_ prefix")
  void shouldExtractRealmRoles() {
    Jwt jwt =
        Jwt.withTokenValue("mock-token")
            .header("alg", "RS256")
            .subject("user-uuid-123")
            .claim("preferred_username", "admin_user")
            .claim("realm_access", Map.of("roles", List.of("admin", "customer")))
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(300))
            .build();

    var authToken = converter.convert(jwt).block();

    assertThat(authToken).isNotNull();
    assertThat(authToken.getName()).isEqualTo("admin_user");
    assertThat(authToken.getAuthorities())
        .extracting(GrantedAuthority::getAuthority)
        .containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_CUSTOMER");
  }

  @Test
  @DisplayName("Should handle missing realm_access gracefully")
  void shouldHandleMissingRealmAccess() {
    Jwt jwt =
        Jwt.withTokenValue("mock-token")
            .header("alg", "RS256")
            .subject("user-uuid-123")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(300))
            .build();

    var authToken = converter.convert(jwt).block();

    assertThat(authToken).isNotNull();
    assertThat(authToken.getName()).isEqualTo("user-uuid-123");
    assertThat(authToken.getAuthorities()).isEmpty();
  }
}
