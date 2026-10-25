package com.shop.order.client;

import com.shop.order.dto.ProductDto;
import com.shop.order.dto.StockDeductDto;
import com.shop.order.dto.StockRestoreDto;
import com.shop.order.exception.OrderProcessingException;
import com.shop.order.exception.ResourceNotFoundException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

@Component
@Slf4j
public class CatalogClientImpl implements CatalogClient {

  private final RestClient restClient;
  private final ClientCredentialsTokenManager tokenManager;

  public CatalogClientImpl(
      @Value("${app.catalog-service.url:http://localhost:8081}") String catalogUrl,
      ClientCredentialsTokenManager tokenManager) {
    this.tokenManager = tokenManager;
    this.restClient =
        RestClient.builder()
            .baseUrl(catalogUrl)
            .defaultStatusHandler(
                HttpStatusCode::is4xxClientError,
                (request, response) -> {
                  String errorBody = new String(response.getBody().readAllBytes());
                  log.warn(
                      "Catalog client 4xx response [{}] : {}", response.getStatusCode(), errorBody);
                  if (response.getStatusCode().value() == 404) {
                    throw new ResourceNotFoundException(
                        "Product in Catalog Service not found: " + errorBody);
                  }
                  throw new OrderProcessingException(
                      "Catalog stock deduction rejected: " + errorBody, "STOCK_DEDUCTION_FAILED");
                })
            .defaultStatusHandler(
                HttpStatusCode::is5xxServerError,
                (request, response) -> {
                  String errorBody = new String(response.getBody().readAllBytes());
                  log.error(
                      "Catalog client 5xx response [{}] : {}", response.getStatusCode(), errorBody);
                  throw new HttpServerErrorException(
                      response.getStatusCode(), "Catalog service server error: " + errorBody);
                })
            .build();
  }

  @Override
  @CircuitBreaker(name = "catalogService")
  @Retry(name = "catalogService")
  public ProductDto getProduct(Long id) {
    log.debug("Calling Catalog Service GET /api/v1/products/{}", id);
    return restClient
        .get()
        .uri("/api/v1/products/{id}", id)
        .accept(MediaType.APPLICATION_JSON)
        .retrieve()
        .body(ProductDto.class);
  }

  @Override
  @CircuitBreaker(name = "catalogService")
  @Retry(name = "catalogService")
  public ProductDto deductStock(Long id, int quantity) {
    log.info(
        "Calling Catalog Service POST /api/v1/internal/products/{}/deduct-stock with quantity: {}",
        id,
        quantity);
    String token = tokenManager.getAccessToken();
    return restClient
        .post()
        .uri("/api/v1/internal/products/{id}/deduct-stock", id)
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .body(new StockDeductDto(quantity))
        .retrieve()
        .body(ProductDto.class);
  }

  @Override
  @CircuitBreaker(name = "catalogService")
  @Retry(name = "catalogService")
  public ProductDto restoreStock(Long id, int quantity) {
    log.info(
        "Calling Catalog Service POST /api/v1/internal/products/{}/restore-stock with quantity: {}",
        id,
        quantity);
    String token = tokenManager.getAccessToken();
    return restClient
        .post()
        .uri("/api/v1/internal/products/{id}/restore-stock", id)
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .body(new StockRestoreDto(quantity))
        .retrieve()
        .body(ProductDto.class);
  }
}
