# Shop UI (Angular 19 SPA)

Modern, reactive single page application built with Angular 19, standalone components, Signals, and OnPush change detection.

## Key Features
- **OpenAPI Client Generation**: Strictly generated from checked-in OpenAPI specs (`../../docs/openapi/catalog-service.json` and `order-service.json`). Run `npm run generate:api` to regenerate.
- **Keycloak PKCE Authentication**: Seamless login and silent token refresh via `angular-oauth2-oidc` with Proof Key for Code Exchange (PKCE).
- **Signal-Based Reactive State**: Real-time reactive cart management with local persistence.
- **Design System**: Glassmorphism dark aesthetic, Plus Jakarta Sans typography, micro-interactions, responsive grid.
- **Production Hardening**: Containerized via multi-stage build running on `nginxinc/nginx-unprivileged:alpine` with gzip compression, security headers (CSP, HSTS, X-Frame-Options), and SPA fallback routing.

## Development & Testing
```bash
npm install
npm run build
npm test -- --watch=false --browsers=ChromeHeadless
```
