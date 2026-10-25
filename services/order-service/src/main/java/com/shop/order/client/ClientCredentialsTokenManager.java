package com.shop.order.client;

import java.time.Instant;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Component
@Slf4j
public class ClientCredentialsTokenManager {

  private final RestClient restClient;
  private final String tokenUri;
  private final String clientId;
  private final String clientSecret;

  private String cachedToken;
  private Instant tokenExpiry = Instant.MIN;

  public ClientCredentialsTokenManager(
      @Value(
              "${app.keycloak.token-uri:http://localhost:8180/realms/shop-realm/protocol/openid-connect/token}")
          String tokenUri,
      @Value("${app.keycloak.client-id:order-service-client}") String clientId,
      @Value("${app.keycloak.client-secret:order-client-super-secret}") String clientSecret) {
    this.restClient = RestClient.builder().build();
    this.tokenUri = tokenUri;
    this.clientId = clientId;
    this.clientSecret = clientSecret;
  }

  public synchronized String getAccessToken() {
    if (cachedToken != null && Instant.now().isBefore(tokenExpiry.minusSeconds(30))) {
      return cachedToken;
    }

    log.debug("Fetching new OAuth2 client credentials token from: {}", tokenUri);
    try {
      MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
      body.add("grant_type", "client_credentials");
      body.add("client_id", clientId);
      body.add("client_secret", clientSecret);

      @SuppressWarnings("unchecked")
      Map<String, Object> response =
          restClient
              .post()
              .uri(tokenUri)
              .contentType(MediaType.APPLICATION_FORM_URLENCODED)
              .body(body)
              .retrieve()
              .body(Map.class);

      if (response != null && response.containsKey("access_token")) {
        this.cachedToken = (String) response.get("access_token");
        int expiresIn = ((Number) response.getOrDefault("expires_in", 300)).intValue();
        this.tokenExpiry = Instant.now().plusSeconds(expiresIn);
        log.info("Obtained client credentials token successfully (expires in {}s)", expiresIn);
        return this.cachedToken;
      }
    } catch (Exception ex) {
      log.warn(
          "Failed to obtain client credentials token from Keycloak: {}. Falling back to cached or empty token for local/test.",
          ex.getMessage());
    }

    return cachedToken != null ? cachedToken : "mock-service-token";
  }
}
