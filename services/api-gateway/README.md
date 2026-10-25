# API Gateway

The `api-gateway` acts as the single entry point for all external client traffic into the Shop microservices stack. Built on Spring Cloud Gateway Reactive and Project Reactor.

## Key Features
- **Redis-Backed Distributed Rate Limiting**: Token-bucket rate limiter (`RequestRateLimiter`) backed by Redis. Resolves rate limit key per authenticated user principal or client IP address. Exceeding limits returns `429 Too Many Requests`.
- **Zero-Trust Internal Endpoint Isolation**: Route `/api/v1/internal/**` is explicitly blocked at the Gateway level returning `403 Forbidden` / `404 Not Found`. Internal service-to-service endpoints are strictly never reachable from the gateway.
- **Correlation ID Tracking**: `CorrelationIdGlobalFilter` inspects incoming `X-Correlation-Id` header, generates a UUID if absent, injects it downstream, and echoes it back in response headers.
- **OAuth2 Token Audience Validation**: Validates Keycloak JWT and ensures the token contains `shop-api` audience.

## Running Tests
```bash
mvn test
```
JaCoCo enforces >= 80% line coverage gate.
