package com.shop.order.contract;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.shop.order.client.CatalogClient;
import com.shop.order.client.CatalogClientImpl;
import com.shop.order.client.ClientCredentialsTokenManager;
import com.shop.order.dto.ProductDto;
import java.math.BigDecimal;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

class OrderConsumerContractTest {

  private static WireMockServer wireMockServer;
  private CatalogClient catalogClient;
  private ObjectMapper objectMapper;

  @BeforeAll
  static void startWireMock() {
    wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
    wireMockServer.start();
  }

  @AfterAll
  static void stopWireMock() {
    if (wireMockServer != null) {
      wireMockServer.stop();
    }
  }

  @BeforeEach
  void setUp() {
    wireMockServer.resetAll();
    objectMapper = new ObjectMapper();

    ClientCredentialsTokenManager mockTokenManager =
        Mockito.mock(ClientCredentialsTokenManager.class);
    Mockito.when(mockTokenManager.getAccessToken()).thenReturn("contract-test-service-token");

    catalogClient = new CatalogClientImpl(wireMockServer.baseUrl(), mockTokenManager);
  }

  @Test
  void contract_getProductById_shouldMatchSpecification() throws Exception {
    ProductDto expected =
        ProductDto.builder()
            .id(1L)
            .name("Noise-Cancelling Headphones")
            .sku("TECH-HEADPHONE-001")
            .price(BigDecimal.valueOf(249.99))
            .stock(50)
            .version(0L)
            .build();

    wireMockServer.stubFor(
        get(urlEqualTo("/api/v1/products/1"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .withBody(objectMapper.writeValueAsString(expected))));

    ProductDto result = catalogClient.getProduct(1L);

    assertThat(result).isNotNull();
    assertThat(result.getId()).isEqualTo(1L);
    assertThat(result.getSku()).isEqualTo("TECH-HEADPHONE-001");
    assertThat(result.getPrice()).isEqualTo(BigDecimal.valueOf(249.99));
  }

  @Test
  void contract_internalDeductStock_shouldSendServiceTokenAndReturnUpdatedStock() throws Exception {
    ProductDto expected =
        ProductDto.builder()
            .id(1L)
            .name("Noise-Cancelling Headphones")
            .sku("TECH-HEADPHONE-001")
            .price(BigDecimal.valueOf(249.99))
            .stock(48)
            .version(1L)
            .build();

    wireMockServer.stubFor(
        post(urlEqualTo("/api/v1/internal/products/1/deduct-stock"))
            .withHeader(HttpHeaders.AUTHORIZATION, equalTo("Bearer contract-test-service-token"))
            .withRequestBody(matchingJsonPath("$.quantity", equalTo("2")))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .withBody(objectMapper.writeValueAsString(expected))));

    ProductDto result = catalogClient.deductStock(1L, 2);

    assertThat(result).isNotNull();
    assertThat(result.getStock()).isEqualTo(48);
    assertThat(result.getVersion()).isEqualTo(1L);
  }

  @Test
  void contract_internalRestoreStock_shouldSendServiceTokenAndReturnRestoredStock()
      throws Exception {
    ProductDto expected =
        ProductDto.builder()
            .id(1L)
            .name("Noise-Cancelling Headphones")
            .sku("TECH-HEADPHONE-001")
            .price(BigDecimal.valueOf(249.99))
            .stock(50)
            .version(2L)
            .build();

    wireMockServer.stubFor(
        post(urlEqualTo("/api/v1/internal/products/1/restore-stock"))
            .withHeader(HttpHeaders.AUTHORIZATION, equalTo("Bearer contract-test-service-token"))
            .withRequestBody(matchingJsonPath("$.quantity", equalTo("2")))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .withBody(objectMapper.writeValueAsString(expected))));

    ProductDto result = catalogClient.restoreStock(1L, 2);

    assertThat(result).isNotNull();
    assertThat(result.getStock()).isEqualTo(50);
    assertThat(result.getVersion()).isEqualTo(2L);
  }
}
