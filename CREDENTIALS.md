# Platform Credentials & Access Guide

This document contains all URLs, credentials, tokens, and database access details for logging into dashboards, services, and administrative interfaces across the **Shop.io** platform.

---

## 1. Web Dashboards & Portals

| Portal / Dashboard | URL | Username | Password | Role / Permissions |
| :--- | :--- | :--- | :--- | :--- |
| **Shop Angular Frontend** | [http://localhost:4200](http://localhost:4200) | `customer` | `user123` | End-user customer: Browse catalog, add to cart, place orders, view order history. |
| **Shop Angular Frontend** | [http://localhost:4200](http://localhost:4200) | `admin` | `admin123` | Administrator: Create/edit products, manage inventory, place orders. |
| **Keycloak Admin Console** | [http://localhost:8180/admin](http://localhost:8180/admin) | `admin` | `admin_password` | Master realm administrator: Manage `shop-realm`, users, clients, roles, tokens. |
| **Grafana Observability** | [http://localhost:3000](http://localhost:3000) | `admin` | `admin_password` | Observability admin: View pre-configured dashboards, Loki logs, Prometheus metrics, Jaeger traces. |
| **Jaeger Tracing UI** | [http://localhost:16686](http://localhost:16686) | *(No auth)* | *(No auth)* | Distributed trace explorer: Inspect end-to-end request spans, latency waterfalls. |
| **Prometheus Metrics UI** | [http://localhost:9090](http://localhost:9090) | *(No auth)* | *(No auth)* | Metrics explorer: PromQL query console, scrape target health checks (`/targets`). |
| **API Gateway Ingress** | [http://localhost:8080](http://localhost:8080) | *(Bearer Token)*| *(Bearer Token)*| Edge router with distributed Redis rate limiting and JWT validation. |

---

## 2. Machine-to-Machine (M2M) Service Accounts

For automated inter-service communication via OAuth2 Client Credentials flow (granting `ROLE_SERVICE`):

| Parameter | Value |
| :--- | :--- |
| **Token Endpoint** | `http://localhost:8180/realms/shop-realm/protocol/openid-connect/token` |
| **Client ID** | `order-service-client` |
| **Client Secret** | `order-client-super-secret` |
| **Grant Type** | `client_credentials` |
| **Assigned Roles** | `ROLE_SERVICE` |
| **Target Endpoints** | `POST http://localhost:8081/api/v1/internal/**` (Internal Stock Deduct / Restore) |

### Curl Command to Generate Service Token:
```bash
curl -s -X POST http://localhost:8180/realms/shop-realm/protocol/openid-connect/token \
  -d "client_id=order-service-client" \
  -d "client_secret=order-client-super-secret" \
  -d "grant_type=client_credentials" | jq -r .access_token
```

---

## 3. Database Connections (PostgreSQL 16)

Connect using `psql`, DBeaver, pgAdmin, DataGrip, or VS Code Database Client:

| Database Instance | Host Port | Database Name | Username | Password | JDBC Connection String |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Catalog Service DB** | `5432` | `catalog_db` | `catalog_user` | `catalog_password` | `jdbc:postgresql://localhost:5432/catalog_db` |
| **Order Service DB** | `5433` | `order_db` | `order_user` | `order_password` | `jdbc:postgresql://localhost:5433/order_db` |
| **Keycloak DB** | `5434` | `keycloak_db` | `keycloak` | `keycloak_db_password` | `jdbc:postgresql://localhost:5434/keycloak_db` |

### Terminal Quick Connect:
```bash
# Connect to Catalog Database
PGPASSWORD=catalog_password psql -h localhost -p 5432 -U catalog_user -d catalog_db

# Connect to Order Database
PGPASSWORD=order_password psql -h localhost -p 5433 -U order_user -d order_db

# Connect to Keycloak Database
PGPASSWORD=keycloak_db_password psql -h localhost -p 5434 -U keycloak -d keycloak_db
```

---

## 4. Redis Cache & Rate Limiting

| Parameter | Value |
| :--- | :--- |
| **Host / Port** | `localhost:6379` |
| **Authentication** | None (Default open in isolated network) |
| **CLI Access** | `docker exec -it shop-redis redis-cli` |
| **Check Rate Limiter Keys** | `KEYS request_rate_limiter*` |

---

## 5. Token Generation Quick Reference (cURL)

### Acquire Customer Token:
```bash
curl -s -X POST http://localhost:8180/realms/shop-realm/protocol/openid-connect/token \
  -d "client_id=shop-ui" \
  -d "grant_type=password" \
  -d "username=customer" \
  -d "password=user123" | jq
```

### Acquire Admin Token:
```bash
curl -s -X POST http://localhost:8180/realms/shop-realm/protocol/openid-connect/token \
  -d "client_id=shop-ui" \
  -d "grant_type=password" \
  -d "username=admin" \
  -d "password=admin123" | jq
```
