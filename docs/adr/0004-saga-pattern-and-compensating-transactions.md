# ADR 0004: Synchronous REST with Resilience4j and Compensating Saga Pattern

## Status
Accepted

## Context
When an order is submitted to `order-service`, product availability must be confirmed and stock deducted in `catalog-service`.
If an order references multiple products and a downstream stock deduction fails midway (e.g. insufficient inventory on item 2, network timeout, or optimistic lock exhaustion), the system must not leave inventory in a deducted, leaked state.
While an event-driven architecture using Kafka/RabbitMQ is standard for large-scale async choreography, introducing message brokers for a minimal service domain increases operational complexity substantially.

## Decision
We implement a **Synchronous Orchestrated Saga with Compensating Transactions**:
1. **Initial Order Creation**: The order is persisted in `order_db` with status `PENDING`.
2. **Synchronous Inventory Call with Resilience**:
   - `order-service` calls `catalog-service` internal stock deduction endpoint using Spring `RestClient` protected by **Resilience4j** (timeout: 3s, retry with exponential backoff for transient 5xx/IO faults, circuit breaker).
3. **Compensating Action on Partial Failure**:
   - If stock deduction fails for any item (or a network partition occurs), `order-service` executes compensating calls (`POST /api/v1/internal/products/{id}/restore-stock`) for all line items that were already successfully deducted.
   - The order status in `order_db` transitions from `PENDING` to `CANCELLED`.
   - The client receives an RFC 7807 `ProblemDetail` (e.g. 422 Unprocessable Entity or 400 Bad Request) detailing the failure.
4. **Success Path**:
   - Upon successful deduction of all items, order status transitions from `PENDING` to `CONFIRMED`.

## Future Evolution
Should the domain scale to include payment providers and shipment dispatchers, this synchronous saga can transition to an asynchronous message-driven Choreographed Saga via Apache Kafka or RabbitMQ with an outbox pattern without changing the domain semantics.

## Consequences
### Positive
- Strict eventual consistency guarantee: zero stock leakage under partial failures.
- Direct synchronous feedback to the frontend user while checking out.
- Minimal operational footprint without requiring a Kafka/Zookeeper cluster for local runs.

### Negative / Trade-offs
- If the Order Service crashes during compensation before completing stock restore, a background reconciliation job or dead-letter mechanism would be required in high-scale environments.
