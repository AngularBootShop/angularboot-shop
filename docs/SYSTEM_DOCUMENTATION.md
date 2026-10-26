# SHOP.IO — Enterprise Microservices System Documentation

This document provides a comprehensive, production-grade system specification for the **SHOP.IO** platform. It details the overarching architectural principles, network topography, zero-trust security model, distributed transaction orchestration, high-availability data persistence, observability fabric, and containerized deployment infrastructure.

---

## 1. High-Level System Architecture

The platform is designed around a decoupled, database-per-service microservice pattern fronted by an API Gateway and authenticated via an OpenID Connect (OIDC) identity provider.

```mermaid
flowchart TB
    subgraph ClientTier["Client Tier & Edge"]
        SPA["Angular 19 SPA (shop-ui)\n:4200 (Nginx Non-Root)"]
        Browser["End Users & System Administrators"]
        Browser --> SPA
    end

    subgraph IdentityTier["Identity & Access Management (IAM)"]
        KC["Keycloak 26.1 (PKCE & OIDC)\n:8180 (shop-realm)"]
        KC_DB[("PostgreSQL 16\nkeycloak_db :5434")]
        KC --> KC_DB
    end

    subgraph GatewayTier["Edge Reverse Proxy & Rate Limiting"]
        GW["Spring Cloud Gateway Reactive\n:8080"]
        REDIS[("Redis 7 Cluster / Single-Node\n:6379 (Token Bucket State)")]
        GW <-->|Sliding Window Rate Limit| REDIS
    end

    subgraph ServiceTier["Core Microservices Layer (Zero-Trust Internal Network)"]
        CATALOG["catalog-service\n:8081 (Spring Boot 3.4.3 / Java 21)"]
        CATALOG_DB[("PostgreSQL 16\ncatalog_db :5432 (Flyway)")]
        CATALOG --> CATALOG_DB

        ORDER["order-service\n:8082 (Spring Boot 3.4.3 / Java 21)"]
        ORDER_DB[("PostgreSQL 16\norder_db :5433 (Flyway)")]
        ORDER --> ORDER_DB
    end

    subgraph ObservabilityTier["Unified Observability Fabric"]
        PROM["Prometheus\n:9090"]
        JAEGER["Jaeger All-In-One (OTLP)\n:16686 / :4318"]
        LOKI["Grafana Loki\n:3100 (Log Aggregator)"]
        PROMTAIL["Promtail Agent\n(Container Log Shipper)"]
        GRAFANA["Grafana Dashboards\n:3000"]
        
        CATALOG -.->|OTLP Traces| JAEGER
        ORDER -.->|OTLP Traces| JAEGER
        GW -.->|OTLP Traces| JAEGER
        PROM -.->|Scrape /actuator/prometheus| CATALOG & ORDER & GW
        PROMTAIL --> LOKI
        GRAFANA --> PROM & JAEGER & LOKI
    end

    SPA -->|"1. OAuth2 PKCE Authorization Code Flow"| KC
    SPA -->|"2. HTTP Requests + JWT + X-Correlation-Id"| GW

    GW -->|"3. Route /api/v1/products/**"| CATALOG
    GW -->|"4. Route /api/v1/orders/**"| ORDER
    GW -.->|"5. Prohibited Route: /api/v1/internal/**"| BLOCKED["HTTP 403 Forbidden Edge Block"]

    ORDER -->|"6. Distributed Saga: Client Credentials M2M (ROLE_SERVICE)"| CATALOG
```

---

## 2. Global Port & Network Mapping

| Service Name | Container Name | Host Port | Internal Port | Protocol | Purpose / Access Policy |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **API Gateway** | `api-gateway` | `8080` | `8080` | HTTP/1.1 | Edge entry point. Exposes public and authenticated customer/admin APIs. |
| **Catalog Service** | `catalog-service` | `8081` | `8081` | HTTP/1.1 | Product CRUD, optimistic concurrency stock management, internal M2M endpoints. |
| **Order Service** | `order-service` | `8082` | `8082` | HTTP/1.1 | Saga orchestrator, idempotent order state machine. |
| **Shop UI** | `shop-ui` | `4200` | `4200` | HTTP/1.1 | Angular 19 SPA running on hardened, unprivileged Nginx. |
| **Keycloak IAM** | `shop-keycloak` | `8180` | `8180` | HTTP/1.1 | OIDC Provider issuing signed RS256 JWT tokens with `shop-api` audience. |
| **Catalog Database** | `postgres-catalog`| `5432` | `5432` | PostgreSQL | Dedicated database for `catalog_db`. |
| **Order Database** | `postgres-order` | `5433` | `5432` | PostgreSQL | Dedicated database for `order_db`. |
| **Keycloak Database**| `postgres-keycloak`| `5434`| `5432` | PostgreSQL | Dedicated database for Keycloak identity schemas (`keycloak_db`). |
| **Redis** | `shop-redis` | `6379` | `6379` | RESP | Distributed rate limiting token bucket cache. |
| **Grafana** | `shop-grafana` | `3000` | `3000` | HTTP/1.1 | Unified dashboard for metrics, trace waterfalls, and log search. |
| **Prometheus** | `shop-prometheus` | `9090` | `9090` | HTTP/1.1 | Scrapes JVM, Spring Actuator, and host metrics. |
| **Jaeger UI** | `shop-jaeger` | `16686` | `16686` | HTTP/1.1 | Distributed trace search, span timeline visualization. |
| **Jaeger OTLP HTTP** | `shop-jaeger` | `4318` | `4318` | HTTP/1.1 | OTLP HTTP receiver for Spring Boot trace exporters. |
| **Loki** | `shop-loki` | `3100` | `3100` | HTTP/1.1 | Log storage engine queryable via LogQL. |

---

## 3. Zero-Trust Security Architecture

The platform enforces a multi-tier security perimeter combining edge blocking, public key cryptographic token verification, and machine-to-machine (M2M) role segregation.

```mermaid
sequenceDiagram
    autonumber
    actor User as Customer / Admin
    participant SPA as Angular SPA (shop-ui)
    participant KC as Keycloak (:8180)
    participant GW as API Gateway (:8080)
    participant ORD as Order Service (:8082)
    participant CAT as Catalog Service (:8081)

    Note over User,KC: Tier 1: User Authentication via PKCE
    User->>SPA: Click "Sign In"
    SPA->>KC: Redirect /auth (Code Challenge + PKCE)
    KC->>User: Render Keycloak Login Form
    User->>KC: Submit Credentials
    KC->>SPA: Redirect /callback?code=AUTH_CODE
    SPA->>KC: POST /token (Code + Code Verifier)
    KC-->>SPA: Return JWT Access Token (Audience: shop-api, Roles: ROLE_USER / ROLE_ADMIN)

    Note over SPA,GW: Tier 2: Edge Routing & Token Validation
    SPA->>GW: POST /api/v1/orders (Bearer JWT + Idempotency-Key)
    GW->>GW: Validate JWT Signature (JWK Certs) & Audience (shop-api)
    GW->>GW: Evaluate Redis Rate Limiter (Token Bucket)
    GW->>ORD: Proxy to http://order-service:8082/api/v1/orders

    Note over ORD,CAT: Tier 3: Zero-Trust M2M Call via Client Credentials
    ORD->>KC: POST /token (grant_type=client_credentials, client_id=order-service-client)
    KC-->>ORD: Return Service JWT (Audience: shop-api, Roles: ROLE_SERVICE)
    ORD->>CAT: POST /api/v1/internal/products/{id}/deduct-stock (Bearer SERVICE_TOKEN)
    CAT->>CAT: Verify ROLE_SERVICE (End-user tokens rejected with 403 Forbidden)
    CAT-->>ORD: Stock Deducted Successfully (HTTP 200 OK)
    ORD-->>GW: Order Created (HTTP 201 Created)
    GW-->>SPA: Return OrderResponse + Location Header
```

### Security Controls & Attack Surface Reduction
1. **Edge Route Blacklisting**:
   - The Gateway contains an explicit matching rule blocking `/api/v1/internal/**` from external ingress:
   ```yaml
   - id: internal-block-catalog
     uri: no://op
     predicates:
       - Path=/api/v1/internal/**
     filters:
       - SetStatus=403
       - SetResponseHeader=X-Blocked-Reason, Internal endpoints prohibited at edge
   ```
2. **Cryptographic Audience Validation**:
   - Every microservice parses the JWT `aud` claim and rejects tokens unless `shop-api` is present, thwarting cross-realm replay attacks.
3. **Internal Endpoint Role Gates**:
   - `InternalStockController` is gated with `@PreAuthorize("hasRole('SERVICE')")`. Even if a valid Admin or Customer JWT reaches this endpoint, Spring Security enforces an immediate `403 Forbidden`.

---

## 4. Distributed Saga Pattern (Compensating Transactions)

To maintain data consistency across distributed database boundaries without blocking two-phase commits (2PC), `order-service` acts as a **Saga Orchestrator** using compensating transactions.

```mermaid
sequenceDiagram
    autonumber
    actor Client as Customer
    participant ORD as Order Service
    participant ODB as order_db (Postgres)
    participant CAT as Catalog Service
    participant CDB as catalog_db (Postgres)

    Client->>ORD: POST /api/v1/orders (Items: [Prod A (qty: 1), Prod B (qty: 99999)])
    ORD->>ODB: Persist Order record with status = 'PENDING'
    
    Note over ORD,CAT: Step 1: Deduct Stock for Product A
    ORD->>CAT: POST /api/v1/internal/products/A/deduct-stock {"quantity": 1}
    CAT->>CDB: UPDATE products SET stock = stock - 1, version = version + 1
    CDB-->>CAT: Success (Stock 50 -> 49)
    CAT-->>ORD: HTTP 200 OK (ProductResponse)

    Note over ORD,CAT: Step 2: Deduct Stock for Product B (Insufficient Inventory)
    ORD->>CAT: POST /api/v1/internal/products/B/deduct-stock {"quantity": 99999}
    CAT->>CDB: Check stock < 99999
    CAT-->>ORD: HTTP 422 Unprocessable Entity ("INSUFFICIENT_STOCK")

    Note over ORD,CAT: Saga Compensation: Rollback Product A Stock
    ORD->>ORD: Trigger Compensation Handler
    ORD->>CAT: POST /api/v1/internal/products/A/restore-stock {"quantity": 1}
    CAT->>CDB: UPDATE products SET stock = stock + 1, version = version + 1
    CDB-->>CAT: Stock Restored (49 -> 50)
    CAT-->>ORD: HTTP 200 OK

    ORD->>ODB: UPDATE orders SET status = 'CANCELLED' WHERE id = :orderId
    ORD-->>Client: HTTP 422 Unprocessable Entity (RFC 7807 Problem Details)
```

---

## 5. Request-Hash Idempotency Engine

To guarantee zero accidental duplicate charges or duplicate stock deductions under network packet retries, `order-service` implements an enterprise request-hash idempotency barrier.

```mermaid
flowchart TD
    Req[Incoming POST /api/v1/orders\nHeaders: Idempotency-Key\nPayload: JSON Body] --> CheckHeader{Idempotency-Key\nHeader Present?}
    CheckHeader -- No --> Err400[Return 400 Bad Request\n'Missing Idempotency-Key']
    CheckHeader -- Yes --> HashCalc[Compute SHA-256 Checksum\nof Raw Request Body]
    HashCalc --> LookupDB[(Lookup idempotency_records\nby Key)]
    
    LookupDB --> Exists{Record\nFound?}
    Exists -- No --> InsertProgress[INSERT idempotency_records\nstatus = 'IN_PROGRESS'\nhash = SHA256\ncreated_at = NOW]
    InsertProgress --> ExecSaga[Execute Order Saga Orchestration]
    ExecSaga --> Success{Saga Result}
    Success -- Success --> UpdateCompleted[UPDATE idempotency_records\nstatus = 'COMPLETED'\nresponse_status = 201\nresponse_body = JSON]
    UpdateCompleted --> Return201[Return HTTP 201 Created]
    Success -- Failure --> UpdateFailed[UPDATE idempotency_records\nstatus = 'FAILED'\nresponse_status = 422\nresponse_body = ProblemDetails]
    UpdateFailed --> Return422[Return HTTP 422 Unprocessable Entity]

    Exists -- Yes --> CheckHash{Stored Hash ==\nCalculated Hash?}
    CheckHash -- No --> Err422[Return 422 Unprocessable Entity\n'Payload checksum mismatch for idempotency key']
    CheckHash -- Yes --> CheckStatus{Stored Status}
    CheckStatus -- IN_PROGRESS --> Err409[Return 409 Conflict\n'Concurrent request currently in progress']
    CheckStatus -- COMPLETED --> CachedResponse[Return Stored Response Status & Body\nHTTP 201 Cached Replay]
    CheckStatus -- FAILED --> CachedFailure[Return Stored Error Status & Body\nHTTP 422 Cached Replay]
```

---

## 6. Distributed Observability & Correlation

The platform provides end-to-end telemetry across asynchronous boundaries.

```mermaid
flowchart LR
    subgraph TracingPipeline["Tracing Flow"]
        ClientReq[HTTP Request] -->|"X-Correlation-Id / traceparent"| GWTrace[API Gateway Span]
        GWTrace --> CatalogSpan[Catalog Service Span]
        GWTrace --> OrderSpan[Order Service Span]
        OrderSpan -->|"M2M HTTP Client"| InternalCatalogSpan[Internal Catalog Span]
        CatalogSpan & OrderSpan & GWTrace & InternalCatalogSpan -.->|OTLP Export :4318| JaegerCollector[Jaeger Collector]
    end

    subgraph LoggingPipeline["Log Aggregation Flow"]
        AppLogs[JSON Structured Logs\ntraceId, spanId, correlationId] --> Promtail[Promtail Agent]
        Promtail --> Loki[Loki Storage :3100]
    end

    subgraph MetricsPipeline["Metrics Flow"]
        ActuatorMicrometer["/actuator/prometheus\nJVM, HikariCP, Netty, HTTP"] --> Prometheus["Prometheus Server :9090"]
    end

    JaegerCollector & Loki & Prometheus --> Grafana["Grafana Unified Dashboard :3000"]
```

### Log Format Specification
All microservices emit structured single-line JSON logs via Logstash Logback encoder:
```json
{
  "@timestamp": "2026-10-25T21:14:10.905Z",
  "@version": "1",
  "message": "POST /api/v1/orders - User: customer, Idempotency-Key: order-verify-1792962850",
  "logger_name": "com.shop.order.controller.OrderController",
  "thread_name": "http-nio-8082-exec-1",
  "level": "INFO",
  "level_value": 20000,
  "appName": "order-service",
  "traceId": "045607856528409dfd12d7125e992f51",
  "spanId": "bad8c109925da4ab",
  "correlationId": "0cb7726a-46df-4e13-8fd2-9bf09ed1174d",
  "service": "order-service"
}
```

---

## 7. Deployment Architectures

### 7.1 Docker Compose (Local & Integration Testing)
The local development stack orchestrates 13 isolated containers via `deploy/docker-compose/docker-compose.yml`:
- **Mount Hardening**: Databases use host volumes; ephemeral services (`redis`, `prometheus`, `loki`, `grafana`, `keycloak`) utilize memory `tmpfs` mounts to isolate write operations from disk space bottlenecks.
- **Bootstrapping**: Automatic database initialization via Flyway migrations; realm import via Keycloak Admin REST API.

### 7.2 Kubernetes Infrastructure (`deploy/k8s`)
The production deployment manifests provide enterprise-grade hardening:
- **Namespaces**: Dedicated `shop` namespace.
- **Security Contexts**:
  - `runAsNonRoot: true`
  - `runAsUser: 10001`
  - `readOnlyRootFilesystem: true`
  - `allowPrivilegeEscalation: false`
  - `capabilities: drop: ["ALL"]`
- **Resilience & Scaling**:
  - Horizontal Pod Autoscalers (`HPA`) scaling on 75% CPU / 80% Memory thresholds.
  - PodDisruptionBudgets (`PDB`) guaranteeing `minAvailable: 1`.
  - Liveness (`/actuator/health/liveness`) and Readiness (`/actuator/health/readiness`) probes with explicit `startPeriodSeconds`.
- **Zero Real Secrets**: All manifests checked into git utilize structured placeholder values (`deploy/k8s/secrets.yaml`). Secrets are injected via SealedSecrets or Vault in production CI/CD.
