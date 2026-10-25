# ADR 0005: Observability Stack (OpenTelemetry Tracing, Prometheus Metrics, Grafana, and Loki Logs)

## Status
Accepted

## Context
Diagnosing latency anomalies, transient network faults, or security exceptions across distributed microservices requires three telemetry pillars: Metrics, Distributed Traces, and Centralized Logs. Furthermore, these three pillars must be correlated so an engineer investigating a slow request trace in Jaeger or Grafana can jump directly to the exact application logs generated during that specific trace.

## Decision
We implement a unified Cloud-Native observability stack:
1. **Metrics**:
   - Spring Boot Actuator with Micrometer exports Prometheus metrics on `/actuator/prometheus`.
   - Prometheus server scrapes `api-gateway`, `catalog-service`, and `order-service` every 15 seconds.
2. **Distributed Tracing**:
   - OpenTelemetry Spring Boot Starter instruments incoming HTTP requests, WebClient/RestClient calls, and JDBC queries.
   - Traces are exported via standard OTLP (gRPC / HTTP) to **Jaeger**.
   - API Gateway assigns an `X-Correlation-Id` header (and MDC traceId) if not provided by the client and propagates it downstream.
3. **Structured Logging & Log Aggregation**:
   - Services format application logs in structured JSON format including `timestamp`, `level`, `service`, `traceId`, `spanId`, `correlationId`, and `message`.
   - **Grafana Loki** ingests container logs via Promtail / Docker logging.
4. **Grafana Dashboards & Correlation**:
   - Grafana is pre-configured with datasources: Prometheus, Jaeger, and Loki.
   - Loki datasource includes a `derivedField` linking `traceId` directly to the Jaeger trace view.

## Consequences
### Positive
- End-to-end request visibility from Gateway to Database.
- Seamless one-click navigation between metric spikes, traces, and corresponding logs in Grafana.
- Standardized OTLP telemetry avoiding proprietary vendor lock-in.

### Negative / Trade-offs
- Modest memory overhead for running Jaeger, Prometheus, and Loki alongside services in local environments.
