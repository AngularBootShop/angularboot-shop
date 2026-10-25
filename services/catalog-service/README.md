# Catalog Service

The `catalog-service` manages product entities, inventory stock levels, and category listings.

## Key Capabilities
- **Optimistic Locking**: Uses `@Version private Long version` to ensure concurrent stock modifications cannot corrupt stock values.
- **Service-to-Service Security**: Endpoints under `/api/v1/internal/**` (e.g. `/deduct-stock`, `/restore-stock`) strictly require `ROLE_SERVICE` and are blocked for end-user tokens (403 Forbidden).
- **Public & Admin Endpoints**:
  - `GET /api/v1/products`: Publicly accessible with keyword filtering, price range, stock availability, and pagination.
  - `POST /api/v1/products`: Restricted to `ROLE_ADMIN`.
  - `PUT /api/v1/products/{id}`: Restricted to `ROLE_ADMIN`.
  - `DELETE /api/v1/products/{id}`: Restricted to `ROLE_ADMIN`.
- **Flyway Database Migrations**: Located in `src/main/resources/db/migration`.
- **RFC 7807 Problem Details**: Consistent error formatting for 400, 404, 409, 422, 500 errors.

## Running Tests
```bash
mvn test
```
JaCoCo enforces >= 80% line coverage gate.
OpenAPI specification is exported at `docs/openapi/catalog-service.json`.
