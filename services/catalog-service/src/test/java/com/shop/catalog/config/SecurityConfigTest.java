package com.shop.catalog.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

class SecurityConfigTest {

  @Test
  void keycloakRoleConverter_shouldExtractRealmAndDirectRoles() {
    SecurityConfig.KeycloakRoleConverter converter = new SecurityConfig.KeycloakRoleConverter();

    Jwt jwt =
        new Jwt(
            "token-value",
            Instant.now(),
            Instant.now().plusSeconds(3600),
            Map.of("alg", "none"),
            Map.of(
                "realm_access", Map.of("roles", List.of("ADMIN", "ROLE_USER")),
                "roles", List.of("SERVICE")));

    Collection<GrantedAuthority> authorities = converter.convert(jwt);

    assertThat(authorities)
        .extracting(GrantedAuthority::getAuthority)
        .containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_USER", "ROLE_SERVICE");
  }

  @Test
  void audienceValidator_whenAudiencePresent_shouldSucceed() {
    SecurityConfig.AudienceValidator validator = new SecurityConfig.AudienceValidator("shop-api");

    Jwt jwt =
        new Jwt(
            "token-value",
            Instant.now(),
            Instant.now().plusSeconds(3600),
            Map.of("alg", "none"),
            Map.of("aud", List.of("shop-api", "account")));

    OAuth2TokenValidatorResult result = validator.validate(jwt);

    assertThat(result.hasErrors()).isFalse();
  }

  @Test
  void audienceValidator_whenAudienceMissing_shouldFail() {
    SecurityConfig.AudienceValidator validator = new SecurityConfig.AudienceValidator("shop-api");

    Jwt jwt =
        new Jwt(
            "token-value",
            Instant.now(),
            Instant.now().plusSeconds(3600),
            Map.of("alg", "none"),
            Map.of("aud", List.of("other-api")));

    OAuth2TokenValidatorResult result = validator.validate(jwt);

    assertThat(result.hasErrors()).isTrue();
    assertThat(result.getErrors()).anyMatch(e -> e.getDescription().contains("shop-api"));
  }
}
