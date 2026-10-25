# ADR 0007: Request-Hash Idempotency for Order Placement

## Status
Accepted

## Context
Network timeouts, mobile client connection drops, and accidental double-clicks frequently trigger duplicate order submissions.
Blindly accepting repeated `POST /api/v1/orders` requests creates duplicate orders, redundant billing, and depleted stock.
Conversely, naively caching a response solely based on an `Idempotency-Key` header without verifying the request body allows malicious or buggy clients to accidentally reuse a previously generated key with a different shopping cart, receiving a false confirmation.

## Decision
We implement a robust **Request-Hash Idempotency Pattern** in `order-service`:
1. **Header Requirement**: `POST /api/v1/orders` requires an `Idempotency-Key` header (UUID or client-generated string).
2. **Payload Hashing**:
   - The service calculates a deterministic cryptographic hash (SHA-256) of the normalized request payload.
3. **Database-Enforced State & Concurrency Control**:
   - An `idempotency_records` table holds:
     - `idempotency_key` (VARCHAR PK / UNIQUE constraint).
     - `request_hash` (CHAR(64) NOT NULL).
     - `status` (`IN_PROGRESS`, `COMPLETED`, `FAILED`).
     - `response_status` (INTEGER).
     - `response_body` (TEXT).
     - `created_at` (TIMESTAMP).
4. **Execution Flow**:
   - **First Arrival**: Insert row with `status = IN_PROGRESS` and `request_hash`.
   - **Concurrent Duplicate**: If a second request arrives while the first is `IN_PROGRESS`, database unique constraint or atomic select returns `409 Conflict` (indicating request is currently being processed).
   - **Mismatched Payload on Reuse**: If an existing key is presented with a different payload hash, the service immediately aborts and returns `422 Unprocessable Entity` with message: "Idempotency-Key reused with conflicting request payload."
   - **Completed Request Reuse**: If an existing key with identical hash has `status = COMPLETED`, the cached HTTP response (`201 Created` and serialized order JSON) is returned directly without re-executing order creation or stock deduction.

## Consequences
### Positive
- Guarantees exactly-once semantics for orders.
- Prevents security anomalies from key collisions or payload tampering.
- Resilient to frontend retries during transient network interruptions.

### Negative / Trade-offs
- Slight storage overhead for storing idempotency records and response payloads (can be pruned via periodic cleanup TTL).
