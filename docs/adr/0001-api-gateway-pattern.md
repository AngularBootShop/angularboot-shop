# ADR 0001: API Gateway Pattern using Spring Cloud Gateway Reactive

## Status
Accepted

## Context
The Shop microservice architecture comprises independent microservices (`catalog-service`, `order-service`) and a single-page application (`shop-ui`). Exposing individual microservice endpoints directly to external clients causes numerous operational and security challenges:
1. Increased client complexity due to orchestrating multiple endpoints and tracking port/host locations.
2. Inconsistent cross-cutting concerns (CORS, distributed rate limiting, SSL termination, and request correlation).
3. Security surface exposure: internal endpoints (such as inter-service inventory deduction and stock restoration) could be accessed by unauthorized external callers if exposed directly.

## Decision
We implement a single entry point using **Spring Cloud Gateway** (Reactive / Netty-based):
1. **Unified Routing**: Gateway routes client traffic `/api/v1/products/**` to `catalog-service` and `/api/v1/orders/**` to `order-service`.
2. **Internal Service Shielding**: Internal service-to-service endpoints (`/api/v1/internal/**`) are deliberately omitted from gateway routes, ensuring they cannot be reached from outside the private container network.
3. **Cross-Cutting Concerns**: Gateway enforces global CORS, correlation ID generation (`X-Correlation-Id`), distributed rate limiting (via Redis), and entry-point JWT token validation with audience validation (`aud: shop-api`).

## Consequences
### Positive
- Client communicates only with a single domain and port (`http://localhost:8080`).
- Consistent observability and correlation ID propagation from edge to downstream services.
- Protection of internal microservice endpoints.

### Negative / Trade-offs
- Single point of failure at the edge; requires multi-instance deployment and HPA / health probing in production.
- Netty non-blocking programming model requires reactive configuration for custom filters.
