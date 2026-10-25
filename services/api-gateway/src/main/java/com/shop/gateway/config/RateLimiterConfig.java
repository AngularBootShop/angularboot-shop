package com.shop.gateway.config;

import java.net.InetSocketAddress;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

@Configuration
public class RateLimiterConfig {

  @Bean
  @Primary
  public KeyResolver userOrIpKeyResolver() {
    return exchange ->
        exchange
            .getPrincipal()
            .flatMap(
                p -> {
                  String name = p != null ? p.getName() : null;
                  if (name != null && !name.isBlank()) {
                    return Mono.just(name);
                  }
                  return Mono.empty();
                })
            .switchIfEmpty(
                Mono.defer(
                    () -> {
                      String forwardedFor =
                          exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
                      if (forwardedFor != null && !forwardedFor.isBlank()) {
                        return Mono.just(forwardedFor.split(",")[0].trim());
                      }
                      InetSocketAddress remoteAddress = exchange.getRequest().getRemoteAddress();
                      if (remoteAddress != null && remoteAddress.getAddress() != null) {
                        return Mono.just(remoteAddress.getAddress().getHostAddress());
                      }
                      return Mono.just("anonymous");
                    }));
  }

  @Bean
  @org.springframework.boot.autoconfigure.condition.ConditionalOnBean(
      org.springframework.data.redis.core.ReactiveStringRedisTemplate.class)
  public RedisRateLimiter redisRateLimiter() {
    // replenishRate: 20 tokens/sec, burstCapacity: 40 tokens
    return new RedisRateLimiter(20, 40, 1);
  }
}
