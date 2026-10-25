# ADR 0006: Redis-Backed Distributed Rate Limiting at API Gateway

## Status
Accepted

## Context
Exposing an API Gateway without rate limiting invites denial-of-service, abusive scraping, and cascading thread exhaustion across downstream microservices. In-memory rate limiting (such as standard Bucket4j in a single process) fails in a clustered environment when multiple Gateway replicas run behind an Ingress controller or load balancer, because token buckets are not shared.

## Decision
We implement distributed rate limiting at the API Gateway using **Redis**:
1. **Spring Cloud Gateway RedisRateLimiter**:
   - Gateway integrates `spring-boot-starter-data-redis-reactive` and `RedisRateLimiter`.
   - Utilizes the token-bucket algorithm executed atomically via Redis Lua scripts.
2. **Key Resolution**:
   - Authenticated requests resolve token bucket key by User Principal (`sub` or username from JWT).
   - Unauthenticated requests resolve token bucket key by Client Remote IP (`X-Forwarded-For` or remote address).
3. **Configuration**:
   - `replenishRate`: 10 requests per second.
   - `burstCapacity`: 20 requests.
   - `requestedTokens`: 1 token per request.
   - Excess requests immediately receive `429 Too Many Requests` with RFC 7807 problem details.
4. **Infrastructure**:
   - Redis 7-alpine is provisioned in Docker Compose and Kubernetes manifests.

## Consequences
### Positive
- Rate limits remain accurate and consistent across horizontally scaled Gateway instances.
- Zero impact on downstream services when excessive traffic is shed at the edge.

### Negative / Trade-offs
- Adds a runtime dependency on Redis for the API Gateway.
