# ADR 0002: OAuth2 / OIDC Authentication with Keycloak and PKCE

## Status
Accepted

## Context
Securing a distributed microservice system with a single-page application requires industry-standard identity and access management (IAM). Storing user credentials or managing session cookies across microservices introduces high security risk, complexity, and violates statelessness.

## Decision
We adopt **OAuth2.0 / OpenID Connect (OIDC)** with **Keycloak 26.x**:
1. **Frontend Authentication**: Angular SPA utilizes the **Authorization Code Flow with Proof Key for Code Exchange (PKCE)** via `angular-oauth2-oidc`. Client secret is never exposed in browser code.
2. **Stateless JWT Verification**: Services (`api-gateway`, `catalog-service`, `order-service`) act as OAuth2 Resource Servers validating cryptographically signed RS256 JWTs against Keycloak's JWKS endpoint.
3. **Audience Validation**: Services strictly validate that the token `aud` claim contains `shop-api`.
4. **Service-to-Service Authorization**: Inter-service communication (such as Order Service calling Catalog Service internal inventory endpoints) uses **OAuth2 Client Credentials Flow** with client `order-service-client` and role `ROLE_SERVICE`. End-user tokens cannot invoke `/api/v1/internal/**`.
5. **Persistence**: Keycloak runs in production mode backed by a dedicated PostgreSQL database (`keycloak_db`).

## Consequences
### Positive
- Strict separation of authentication from application logic.
- Robust role-based access control (`ROLE_ADMIN`, `ROLE_USER`, `ROLE_SERVICE`).
- Revocation and token lifecycle managed centrally without restarting services.

### Negative / Trade-offs
- Keycloak is an additional infrastructure component requiring memory and health check coordination during startup.
