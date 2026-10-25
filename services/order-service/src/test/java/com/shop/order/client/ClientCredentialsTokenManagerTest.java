package com.shop.order.client;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

class ClientCredentialsTokenManagerTest {

  private static WireMockServer wireMockServer;
  private ClientCredentialsTokenManager tokenManager;

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
    tokenManager =
        new ClientCredentialsTokenManager(
            wireMockServer.baseUrl() + "/token", "test-client", "test-secret");
  }

  @Test
  void getAccessToken_whenSuccess_shouldFetchAndCacheToken() {
    wireMockServer.stubFor(
        post(urlEqualTo("/token"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .withBody("{\"access_token\":\"sample-jwt-token\",\"expires_in\":3600}")));

    String token1 = tokenManager.getAccessToken();
    assertThat(token1).isEqualTo("sample-jwt-token");

    // Second call should return cached token without invoking wiremock again
    String token2 = tokenManager.getAccessToken();
    assertThat(token2).isEqualTo("sample-jwt-token");
    wireMockServer.verify(1, postRequestedFor(urlEqualTo("/token")));
  }

  @Test
  void getAccessToken_whenEndpointFails_shouldReturnFallbackToken() {
    wireMockServer.stubFor(post(urlEqualTo("/token")).willReturn(aResponse().withStatus(500)));

    String token = tokenManager.getAccessToken();
    assertThat(token).isEqualTo("mock-service-token");
  }
}
