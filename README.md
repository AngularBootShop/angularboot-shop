# Shop Microservices Architecture (Production-Grade)

A complete, production-grade microservice e-commerce system built to Senior Staff and Cloud Architect engineering standards. Minimal domain scope, maximal architectural rigor.

---

## 📚 Complete System & Component Documentation

| Document | Focus & Contents |
| :--- | :--- |
| **[System Architecture Documentation](docs/SYSTEM_DOCUMENTATION.md)** | Full platform topology, zero-trust security perimeter, distributed saga transactions, request-hash idempotency, observability pipeline, and Kubernetes/Compose deployments with Mermaid diagrams. |
| **[Backend Microservices Documentation](docs/BACKEND_DOCUMENTATION.md)** | Exhaustive technical guide for `api-gateway`, `catalog-service`, and `order-service`, complete Database ERD diagrams, 11 API endpoints with RFC 7807 problem details, and full test matrices. |
| **[Frontend UI Documentation](docs/FRONTEND_DOCUMENTATION.md)** | Deep dive into `shop-ui` (Angular 19), Angular Signals reactive state management, Keycloak PKCE authentication, OpenAPI code generation, component architecture, and Playwright E2E suites. |
| **[Architecture Decision Records (ADRs)](docs/adr/)** | 7 foundational architectural decision records covering Gateway, Keycloak PKCE, Flyway, Saga, Observability, Rate Limiting, and Idempotency. |

---

## 1. System Architecture

```mermaid
flowchart TB
    subgraph Clients["Edge & Clients"]
        SPA["Angular 19 SPA (shop-ui)\n:4200 (Nginx Non-Root)"]
        Browser["End Users & Admins"]
        Browser --> SPA
    end

    subgraph IAM["Identity & Access Management"]
        KC["Keycloak 26.1 (PKCE & OIDC)\n:8180 (shop-realm)"]
        KC_DB[("PostgreSQL 16 (keycloak_db)\n:5434")]
        KC --> KC_DB
    end

    subgraph Edge["Edge Routing & Distributed Rate Limiting"]
        GW["API Gateway (Spring Cloud Gateway Reactive)\n:8080"]
        REDIS[("Redis 7 (Rate Limiting Token Bucket)\n:6379")]
        GW <--> REDIS
    end

    subgraph Microservices["Backend Services Layer"]
        CATALOG["catalog-service\n:8081 (Spring Boot 3.4.3 / Java 21)"]
        CATALOG_DB[("PostgreSQL 16 (catalog_db)\n:5432 (Flyway)")]
        CATALOG --> CATALOG_DB

        ORDER["order-service\n:8082 (Spring Boot 3.4.3 / Java 21)"]
        ORDER_DB[("PostgreSQL 16 (order_db)\n:5433 (Flyway)")]
        ORDER --> ORDER_DB
    end

    subgraph Observability["Full Observability & Tracing"]
        PROM["Prometheus\n:9090"]
        JAEGER["Jaeger Tracing (OTLP)\n:16686 / :4318"]
        LOKI["Grafana Loki\n:3100"]
        PROMTAIL["Promtail\n(Docker Logs)"]
        GRAFANA["Grafana Dashboards\n:3000"]
        
        CATALOG -.->|OTLP Traces| JAEGER
        ORDER -.->|OTLP Traces| JAEGER
        GW -.->|OTLP Traces| JAEGER
        PROM -.->|Scrape /metrics| CATALOG & ORDER & GW
        PROMTAIL --> LOKI
        GRAFANA --> PROM & JAEGER & LOKI
    end

    SPA -->|OAuth2 PKCE Login| KC
    SPA -->|API Requests + JWT + X-Correlation-Id| GW

    GW -->|Route /api/v1/products/**| CATALOG
    GW -->|Route /api/v1/orders/**| ORDER
    GW -.->|BLOCK /api/v1/internal/** (403)| X[Blocked]

    ORDER -->|Saga: OAuth2 Client Credentials + ROLE_SERVICE\n/api/v1/internal/products/{id}/deduct-stock\n/restore-stock| CATALOG
```

---

## 2. Key Architectural Guarantees

| Capability | Implementation Mechanism |
| :--- | :--- |
| **Saga Distributed Transaction** | `order-service` validates and sets order `PENDING`. Executes stock deduction across items via REST client. If any step fails, executes compensating stock restoration (`restore-stock`) and sets status `CANCELLED`. |
| **Service-to-Service Security** | Inter-service calls use OAuth2 Client Credentials (`order-service-client`) granting `ROLE_SERVICE`. Endpoints `/api/v1/internal/**` require `ROLE_SERVICE` and strictly reject end-user tokens with `403 Forbidden`. The API Gateway blocks `/api/v1/internal/**`. |
| **Request-Hash Idempotency** | Orders require `Idempotency-Key` header. SHA-256 hash of payload is computed. Repeat requests with identical hash return cached 201; payload mismatch returns `422 Unprocessable Entity`; concurrent requests return `409 Conflict`. |
| **Distributed Rate Limiting** | Redis-backed token bucket (`RequestRateLimiter`) at API Gateway. Resolves keys per authenticated user or IP address. Exceeding burst capacity returns `429 Too Many Requests`. |
| **Observability Correlation** | Traces flow via OpenTelemetry to Jaeger. Logs are formatted as structured JSON with `traceId` and `spanId`. Promtail ships logs to Loki. Grafana links trace-to-log and log-to-trace seamlessly. |
| **OpenAPI Contract Generation** | Checked-in OpenAPI 3.1 specifications (`docs/openapi/`) are the source of truth. Angular TypeScript clients are generated via `openapi-generator-cli`. |
| **Zero Real Secrets in Manifests** | Kubernetes manifests use placeholders only (`deploy/k8s/secrets.yaml`). Container security uses non-root users, read-only root filesystems, and strict NetworkPolicies. |

---

## 3. Port & Service Mapping

| Component | Port | Description |
| :--- | :--- | :--- |
| **API Gateway** | `8080` | Entry point for all external HTTP traffic |
| **Catalog Service** | `8081` | Product management & optimistic-locking inventory |
| **Order Service** | `8082` | Order management & Saga orchestrator |
| **Shop UI** | `4200` | Angular 19 SPA (Nginx unprivileged) |
| **Keycloak IAM** | `8180` | Identity provider with PKCE & client credentials |
| **PostgreSQL (Catalog)** | `5432` | Catalog microservice database |
| **PostgreSQL (Order)** | `5433` | Order microservice database |
| **PostgreSQL (Keycloak)** | `5434` | Dedicated Keycloak database |
| **Redis** | `6379` | Distributed token bucket rate limiter |
| **Grafana** | `3000` | Observability dashboards & trace-to-log exploration |
| **Prometheus** | `9090` | Metrics scraper & time series store |
| **Jaeger UI** | `16686` | Distributed trace search and waterfall visualization |
| **Loki** | `3100` | Log aggregation engine |

---

## 4. Default Credentials & Test Users

| Service / Role | Username | Password | Purpose |
| :--- | :--- | :--- | :--- |
| **Keycloak Realm Admin** | `admin` | `admin_password` | Keycloak Master Console |
| **Shop Admin User** | `admin` | `admin123` | Roles: `ROLE_ADMIN`, `ROLE_USER` |
| **Shop Customer User** | `customer` | `customer123` | Roles: `ROLE_USER` |
| **Service Client** | `order-service-client` | `order-service-secret` | Role: `ROLE_SERVICE` (Inter-service) |
| **Grafana Admin** | `admin` | `admin_password` | Monitoring dashboard |
| **Database (Catalog)** | `catalog_user` | `catalog_password` | Database: `catalog_db` |
| **Database (Order)** | `order_user` | `order_password` | Database: `order_db` |
| **Database (Keycloak)** | `keycloak` | `keycloak_db_password` | Database: `keycloak_db` |

---

## 5. Quick Start (Docker Compose)

### 5.1 Prerequisites
- Docker Engine 24+ & Docker Compose v2.20+
- Java 21 & Maven 3.9+ (for local builds)
- Node.js 22+ (for Angular UI development)

### 5.2 Build & Start Stack
```bash
# 1. Package backend services
mvn clean package -DskipTests

# 2. Build and launch entire stack
docker compose -f deploy/docker-compose/docker-compose.yml up -d --build

# 3. Check health status across all containers
docker compose -f deploy/docker-compose/docker-compose.yml ps
```

### 5.3 Access Web Application
- **Shop UI**: Open [http://localhost:4200](http://localhost:4200) in your browser.
- **Grafana**: Open [http://localhost:3000](http://localhost:3000) (User: `admin`, Password: `admin_password`).
- **Jaeger Traces**: Open [http://localhost:16686](http://localhost:16686).
- **Keycloak Console**: Open [http://localhost:8180](http://localhost:8180) (User: `admin`, Password: `admin_password`).

---

## 6. Verification & Automated Test Commands

```bash
# Run all backend unit, contract & integration tests with JaCoCo verification
mvn test

# Run Angular unit tests in headless Chrome
cd frontend/shop-ui && npm test -- --watch=false --browsers=ChromeHeadless

# Regenerate Angular API client from OpenAPI specs
cd frontend/shop-ui && npm run generate:api
```
