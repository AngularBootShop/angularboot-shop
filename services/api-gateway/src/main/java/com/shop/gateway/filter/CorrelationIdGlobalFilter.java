package com.shop.gateway.filter;

import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class CorrelationIdGlobalFilter implements GlobalFilter, Ordered {

  public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    ServerHttpRequest request = exchange.getRequest();
    String correlationId = request.getHeaders().getFirst(CORRELATION_ID_HEADER);

    if (correlationId == null || correlationId.isBlank()) {
      correlationId = UUID.randomUUID().toString();
      log.debug("Generated new correlation ID: {}", correlationId);
    } else {
      log.debug("Found existing correlation ID: {}", correlationId);
    }

    final String finalCorrelationId = correlationId;

    // Mutate downstream request with correlation ID
    ServerHttpRequest mutatedRequest =
        request.mutate().header(CORRELATION_ID_HEADER, finalCorrelationId).build();

    // Mutate exchange with mutated request
    ServerWebExchange mutatedExchange = exchange.mutate().request(mutatedRequest).build();

    // Add correlation ID to outgoing response headers
    mutatedExchange
        .getResponse()
        .beforeCommit(
            () -> {
              mutatedExchange
                  .getResponse()
                  .getHeaders()
                  .set(CORRELATION_ID_HEADER, finalCorrelationId);
              return Mono.empty();
            });

    return chain.filter(mutatedExchange);
  }

  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE;
  }
}
