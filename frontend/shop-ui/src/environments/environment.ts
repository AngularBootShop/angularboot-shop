export const environment = {
  production: true,
  apiBaseUrl: 'http://localhost:8080',
  keycloak: {
    issuer: 'http://localhost:8180/realms/shop-realm',
    redirectUri: window.location.origin + '/',
    clientId: 'shop-ui',
    responseType: 'code',
    scope: 'openid profile email',
    requireHttps: false
  }
};
