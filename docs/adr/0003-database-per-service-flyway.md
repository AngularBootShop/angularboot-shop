# ADR 0003: Database-per-Service Pattern with Flyway Migrations

## Status
Accepted

## Context
A microservices system must maintain loose coupling and autonomous lifecycle management. Sharing a single database schema or database instance across multiple microservices leads to tight coupling, schema evolution deadlocks, and violation of bounded context boundaries.

## Decision
We enforce the **Database-per-Service** pattern:
1. **Isolated PostgreSQL Instances/Databases**:
   - `catalog_db` for `catalog-service`.
   - `order_db` for `order-service`.
   - `keycloak_db` for `keycloak`.
   No service possesses credentials to access another service's database directly.
2. **Schema Evolution via Flyway**:
   - Database migrations are versioned and committed in source control (`src/main/resources/db/migration/V{n}__{description}.sql`).
   - Migrations execute automatically on application startup before Hibernate initializes.
3. **Concurrency Control**:
   - `catalog-service` uses optimistic locking (`@Version` on `products`) to prevent lost updates during concurrent stock modifications.

## Consequences
### Positive
- True data isolation and independent deployability of services.
- Deterministic, reproducible database state across local, test, and production environments.
- Zero risk of cross-service table joins or hidden dependencies.

### Negative / Trade-offs
- Cross-service transactions cannot use 2PC / distributed ACID transactions; data consistency must be managed via Sagas or compensating actions.
