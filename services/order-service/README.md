# Order Service

The `order-service` handles order placement, line item processing, distributed transaction orchestration (Saga), and order history retrieval.

## Key Architectural Patterns
- **Compensating Saga Orchestrator**:
  - Validates order and saves order in `PENDING` status.
  - Iterates over line items and calls `catalog-service` internal endpoint `/api/v1/internal/products/{id}/deduct-stock` using OAuth2 Client Credentials (`ROLE_SERVICE`).
  - If any stock deduction fails (or network times out), triggers compensating transaction: calls `catalog-service` `/api/v1/internal/products/{id}/restore-stock` for all items deducted so far, and transitions order status to `CANCELLED`.
  - When all deductions succeed, transitions order status to `CONFIRMED`.
- **Request-Hash Idempotency**:
  - Requires `Idempotency-Key` header on `POST /api/v1/orders`.
  - Computes SHA-256 hash of the JSON payload.
  - On repeat request with identical key and identical hash: returns cached 201 response.
  - On key reuse with different payload: returns `422 Unprocessable Entity`.
  - On concurrent request with same key in progress: returns `409 Conflict`.
- **Consumer-Driven Contract Testing**:
  - Contract test `OrderConsumerContractTest` verifies catalog stock deduction contract.

## Running Tests
```bash
mvn test
```
JaCoCo enforces >= 80% line coverage gate.
OpenAPI specification is exported at `docs/openapi/order-service.json`.
