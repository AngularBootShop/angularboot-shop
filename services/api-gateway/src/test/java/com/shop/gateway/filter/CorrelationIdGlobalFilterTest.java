package com.shop.gateway.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;

class CorrelationIdGlobalFilterTest {

  private CorrelationIdGlobalFilter filter;
  private GatewayFilterChain chain;

  @BeforeEach
  void setUp() {
    filter = new CorrelationIdGlobalFilter();
    chain = mock(GatewayFilterChain.class);
    when(chain.filter(any(ServerWebExchange.class)))
        .thenAnswer(
            invocation -> {
              ServerWebExchange swe = invocation.getArgument(0);
              return swe.getResponse().setComplete();
            });
  }

  @Test
  @DisplayName("Should generate new correlation ID when header is missing and commit to response")
  void shouldGenerateNewCorrelationIdWhenMissing() {
    MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/products").build();
    MockServerWebExchange exchange = MockServerWebExchange.from(request);

    filter.filter(exchange, chain).block();

    ArgumentCaptor<ServerWebExchange> captor = ArgumentCaptor.forClass(ServerWebExchange.class);
    verify(chain).filter(captor.capture());

    ServerWebExchange capturedExchange = captor.getValue();
    String reqCorrelationId =
        capturedExchange
            .getRequest()
            .getHeaders()
            .getFirst(CorrelationIdGlobalFilter.CORRELATION_ID_HEADER);

    assertThat(reqCorrelationId).isNotNull().isNotBlank();

    String respCorrelationId =
        capturedExchange
            .getResponse()
            .getHeaders()
            .getFirst(CorrelationIdGlobalFilter.CORRELATION_ID_HEADER);

    assertThat(respCorrelationId).isEqualTo(reqCorrelationId);
  }

  @Test
  @DisplayName(
      "Should preserve existing correlation ID when header is present and commit to response")
  void shouldPreserveExistingCorrelationId() {
    String existingId = "client-trace-12345";
    MockServerHttpRequest request =
        MockServerHttpRequest.get("/api/v1/products")
            .header(CorrelationIdGlobalFilter.CORRELATION_ID_HEADER, existingId)
            .build();
    MockServerWebExchange exchange = MockServerWebExchange.from(request);

    filter.filter(exchange, chain).block();

    ArgumentCaptor<ServerWebExchange> captor = ArgumentCaptor.forClass(ServerWebExchange.class);
    verify(chain).filter(captor.capture());

    ServerWebExchange capturedExchange = captor.getValue();
    String reqCorrelationId =
        capturedExchange
            .getRequest()
            .getHeaders()
            .getFirst(CorrelationIdGlobalFilter.CORRELATION_ID_HEADER);

    assertThat(reqCorrelationId).isEqualTo(existingId);

    String respCorrelationId =
        capturedExchange
            .getResponse()
            .getHeaders()
            .getFirst(CorrelationIdGlobalFilter.CORRELATION_ID_HEADER);

    assertThat(respCorrelationId).isEqualTo(existingId);
  }

  @Test
  @DisplayName("Should return HIGHEST_PRECEDENCE for order")
  void shouldReturnHighestPrecedenceOrder() {
    assertThat(filter.getOrder()).isEqualTo(Ordered.HIGHEST_PRECEDENCE);
  }
}
