package com.shop.gateway.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.InetSocketAddress;
import java.security.Principal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.test.StepVerifier;

class RateLimiterConfigTest {

  private RateLimiterConfig rateLimiterConfig;
  private KeyResolver keyResolver;

  @BeforeEach
  void setUp() {
    rateLimiterConfig = new RateLimiterConfig();
    keyResolver = rateLimiterConfig.userOrIpKeyResolver();
  }

  @Test
  @DisplayName("Should resolve authenticated user principal name")
  void shouldResolveAuthenticatedPrincipalName() {
    MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/orders").build();
    MockServerWebExchange baseExchange = MockServerWebExchange.from(request);
    org.springframework.web.server.ServerWebExchange exchange =
        baseExchange
            .mutate()
            .principal(reactor.core.publisher.Mono.just((Principal) () -> "test-user"))
            .build();

    StepVerifier.create(keyResolver.resolve(exchange)).expectNext("test-user").verifyComplete();
  }

  @Test
  @DisplayName("Should resolve X-Forwarded-For header when unauthenticated")
  void shouldResolveForwardedForHeaderWhenUnauthenticated() {
    MockServerHttpRequest request =
        MockServerHttpRequest.get("/api/v1/products")
            .header("X-Forwarded-For", "203.0.113.195, 70.41.3.18")
            .build();
    MockServerWebExchange exchange = MockServerWebExchange.from(request);

    StepVerifier.create(keyResolver.resolve(exchange)).expectNext("203.0.113.195").verifyComplete();
  }

  @Test
  @DisplayName("Should resolve remote host address when unauthenticated without headers")
  void shouldResolveRemoteHostAddress() {
    MockServerHttpRequest request =
        MockServerHttpRequest.get("/api/v1/products")
            .remoteAddress(new InetSocketAddress("198.51.100.42", 54321))
            .build();
    MockServerWebExchange exchange = MockServerWebExchange.from(request);

    StepVerifier.create(keyResolver.resolve(exchange)).expectNext("198.51.100.42").verifyComplete();
  }

  @Test
  @DisplayName("Should configure RedisRateLimiter bean")
  void shouldConfigureRedisRateLimiter() {
    var limiter = rateLimiterConfig.redisRateLimiter();
    assertThat(limiter).isNotNull();
  }
}
