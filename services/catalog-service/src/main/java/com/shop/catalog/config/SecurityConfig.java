package com.shop.catalog.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

  @Value(
      "${spring.security.oauth2.resourceserver.jwt.jwk-set-uri:http://localhost:8180/realms/shop-realm/protocol/openid-connect/certs}")
  private String jwkSetUri;

  @Value("${app.security.expected-audience:shop-api}")
  private String expectedAudience;

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtDecoder jwtDecoder)
      throws Exception {
    http.csrf(AbstractHttpConfigurer::disable)
        .cors(AbstractHttpConfigurer::disable)
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .headers(
            headers ->
                headers
                    .frameOptions(frame -> frame.deny())
                    .xssProtection(xss -> xss.disable())
                    .contentTypeOptions(content -> {})
                    .referrerPolicy(
                        referrer ->
                            referrer.policy(
                                ReferrerPolicyHeaderWriter.ReferrerPolicy
                                    .STRICT_ORIGIN_WHEN_CROSS_ORIGIN)))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers("/actuator/**")
                    .permitAll()
                    .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/v1/products/**")
                    .permitAll()
                    .requestMatchers("/api/v1/internal/**")
                    .hasRole("SERVICE")
                    .requestMatchers(HttpMethod.POST, "/api/v1/products/**")
                    .hasRole("ADMIN")
                    .requestMatchers(HttpMethod.PUT, "/api/v1/products/**")
                    .hasRole("ADMIN")
                    .requestMatchers(HttpMethod.DELETE, "/api/v1/products/**")
                    .hasRole("ADMIN")
                    .anyRequest()
                    .authenticated())
        .oauth2ResourceServer(
            oauth2 ->
                oauth2.jwt(
                    jwt ->
                        jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())
                            .decoder(jwtDecoder)));

    return http.build();
  }

  @Bean
  @ConditionalOnMissingBean(JwtDecoder.class)
  public JwtDecoder jwtDecoder() {
    NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();

    OAuth2TokenValidator<Jwt> timestampValidator = new JwtTimestampValidator();
    OAuth2TokenValidator<Jwt> issuerValidator =
        new JwtClaimValidator<String>(
            "iss", iss -> iss != null && iss.endsWith("/realms/shop-realm"));
    OAuth2TokenValidator<Jwt> audienceValidator = new AudienceValidator(expectedAudience);
    OAuth2TokenValidator<Jwt> combinedValidator =
        new DelegatingOAuth2TokenValidator<>(
            timestampValidator, issuerValidator, audienceValidator);

    jwtDecoder.setJwtValidator(combinedValidator);
    return jwtDecoder;
  }

  @Bean
  public JwtAuthenticationConverter jwtAuthenticationConverter() {
    JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(new KeycloakRoleConverter());
    return converter;
  }

  public static class KeycloakRoleConverter
      implements Converter<Jwt, Collection<GrantedAuthority>> {
    @Override
    @SuppressWarnings("unchecked")
    public Collection<GrantedAuthority> convert(Jwt jwt) {
      List<GrantedAuthority> authorities = new ArrayList<>();

      // 1. Extract from realm_access.roles
      Map<String, Object> realmAccess = jwt.getClaim("realm_access");
      if (realmAccess != null && realmAccess.containsKey("roles")) {
        List<String> roles = (List<String>) realmAccess.get("roles");
        roles.forEach(
            role -> {
              String authority = role.startsWith("ROLE_") ? role : "ROLE_" + role;
              authorities.add(new SimpleGrantedAuthority(authority));
            });
      }

      // 2. Extract from direct "roles" claim if present
      List<String> directRoles = jwt.getClaimAsStringList("roles");
      if (directRoles != null) {
        directRoles.forEach(
            role -> {
              String authority = role.startsWith("ROLE_") ? role : "ROLE_" + role;
              authorities.add(new SimpleGrantedAuthority(authority));
            });
      }

      return authorities;
    }
  }

  public static class AudienceValidator implements OAuth2TokenValidator<Jwt> {
    private final String audience;

    public AudienceValidator(String audience) {
      this.audience = audience;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
      List<String> audiences = jwt.getAudience();
      if (audiences != null && audiences.contains(audience)) {
        return OAuth2TokenValidatorResult.success();
      }
      OAuth2Error error =
          new OAuth2Error(
              "invalid_token",
              String.format("The required audience '%s' is missing in token claims", audience),
              null);
      return OAuth2TokenValidatorResult.failure(error);
    }
  }
}
