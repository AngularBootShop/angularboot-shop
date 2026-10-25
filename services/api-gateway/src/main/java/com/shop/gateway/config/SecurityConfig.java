package com.shop.gateway.config;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.core.publisher.Mono;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

  @Value(
      "${spring.security.oauth2.resourceserver.jwt.issuer-uri:http://localhost:8180/realms/shop-realm}")
  private String issuerUri;

  @Value(
      "${spring.security.oauth2.resourceserver.jwt.jwk-set-uri:http://localhost:8180/realms/shop-realm/protocol/openid-connect/certs}")
  private String jwkSetUri;

  @Bean
  public SecurityWebFilterChain securityWebFilterChain(
      ServerHttpSecurity http, KeycloakJwtAuthenticationConverter jwtAuthenticationConverter) {
    return http.csrf(ServerHttpSecurity.CsrfSpec::disable)
        .authorizeExchange(
            exchanges ->
                exchanges
                    .pathMatchers("/actuator/**")
                    .permitAll()
                    .pathMatchers(HttpMethod.OPTIONS)
                    .permitAll()
                    // Public catalog view
                    .pathMatchers(HttpMethod.GET, "/api/v1/products/**")
                    .permitAll()
                    // Internal endpoints strictly denied on gateway
                    .pathMatchers("/api/v1/internal/**")
                    .denyAll()
                    // Catalog administrative modifications
                    .pathMatchers(HttpMethod.POST, "/api/v1/products/**")
                    .hasRole("ADMIN")
                    .pathMatchers(HttpMethod.PUT, "/api/v1/products/**")
                    .hasRole("ADMIN")
                    .pathMatchers(HttpMethod.DELETE, "/api/v1/products/**")
                    .hasRole("ADMIN")
                    // Orders require authentication
                    .pathMatchers("/api/v1/orders/**")
                    .authenticated()
                    .anyExchange()
                    .authenticated())
        .oauth2ResourceServer(
            oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)))
        .build();
  }

  @Bean
  @ConditionalOnMissingBean(ReactiveJwtDecoder.class)
  public ReactiveJwtDecoder reactiveJwtDecoder() {
    NimbusReactiveJwtDecoder jwtDecoder = NimbusReactiveJwtDecoder.withJwkSetUri(jwkSetUri).build();

    OAuth2TokenValidator<Jwt> timestampValidator = new JwtTimestampValidator();
    OAuth2TokenValidator<Jwt> issuerValidator =
        new JwtClaimValidator<String>(
            "iss", iss -> iss != null && iss.endsWith("/realms/shop-realm"));
    OAuth2TokenValidator<Jwt> audienceValidator =
        new JwtClaimValidator<List<String>>(
            "aud", aud -> aud != null && (aud.contains("shop-api") || aud.contains("account")));

    jwtDecoder.setJwtValidator(
        new DelegatingOAuth2TokenValidator<>(
            timestampValidator, issuerValidator, audienceValidator));
    return jwtDecoder;
  }

  @Bean
  public KeycloakJwtAuthenticationConverter jwtAuthenticationConverter() {
    return new KeycloakJwtAuthenticationConverter();
  }

  public static class KeycloakJwtAuthenticationConverter
      implements Converter<Jwt, Mono<AbstractAuthenticationToken>> {

    @Override
    public Mono<AbstractAuthenticationToken> convert(Jwt jwt) {
      Collection<GrantedAuthority> authorities = extractRealmRoles(jwt);
      String principalClaimName =
          jwt.getClaimAsString("preferred_username") != null
              ? jwt.getClaimAsString("preferred_username")
              : (jwt.getSubject() != null ? jwt.getSubject() : "shop-user");
      return Mono.just(new JwtAuthenticationToken(jwt, authorities, principalClaimName));
    }

    @SuppressWarnings("unchecked")
    private Collection<GrantedAuthority> extractRealmRoles(Jwt jwt) {
      java.util.List<GrantedAuthority> authorities = new java.util.ArrayList<>();

      // 1. Extract from realm_access.roles
      Map<String, Object> realmAccess = jwt.getClaim("realm_access");
      if (realmAccess != null && realmAccess.containsKey("roles")) {
        Collection<String> roles = (Collection<String>) realmAccess.get("roles");
        roles.forEach(
            role -> {
              String authority = role.startsWith("ROLE_") ? role : "ROLE_" + role.toUpperCase();
              authorities.add(new SimpleGrantedAuthority(authority));
            });
      }

      // 2. Extract from direct "roles" claim if present
      List<String> directRoles = jwt.getClaimAsStringList("roles");
      if (directRoles != null) {
        directRoles.forEach(
            role -> {
              String authority = role.startsWith("ROLE_") ? role : "ROLE_" + role.toUpperCase();
              authorities.add(new SimpleGrantedAuthority(authority));
            });
      }

      return authorities;
    }
  }
}
