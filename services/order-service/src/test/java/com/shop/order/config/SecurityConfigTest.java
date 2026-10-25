package com.shop.order.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

class SecurityConfigTest {

  @Test
  void keycloakRoleConverter_shouldExtractRoles() {
    SecurityConfig.KeycloakRoleConverter converter = new SecurityConfig.KeycloakRoleConverter();

    Jwt jwt =
        new Jwt(
            "token-val",
            Instant.now(),
            Instant.now().plusSeconds(3600),
            Map.of("alg", "none"),
            Map.of(
                "realm_access", Map.of("roles", List.of("USER")),
                "roles", List.of("ADMIN")));

    Collection<GrantedAuthority> authorities = converter.convert(jwt);

    assertThat(authorities)
        .extracting(GrantedAuthority::getAuthority)
        .containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
  }

  @Test
  void audienceValidator_shouldValidateAudience() {
    SecurityConfig.AudienceValidator validator = new SecurityConfig.AudienceValidator("shop-api");

    Jwt validJwt =
        new Jwt(
            "v",
            Instant.now(),
            Instant.now().plusSeconds(3600),
            Map.of("a", "b"),
            Map.of("aud", List.of("shop-api")));
    Jwt invalidJwt =
        new Jwt(
            "v",
            Instant.now(),
            Instant.now().plusSeconds(3600),
            Map.of("a", "b"),
            Map.of("aud", List.of("other-api")));

    assertThat(validator.validate(validJwt).hasErrors()).isFalse();
    assertThat(validator.validate(invalidJwt).hasErrors()).isTrue();
  }
}
