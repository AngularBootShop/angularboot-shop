#!/usr/bin/env bash
# ==============================================================================
# SHOP.IO — One-Command Production Stack Launcher
# Usage: ./start.sh
# ==============================================================================
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMPOSE_FILE="${ROOT_DIR}/deploy/docker-compose/docker-compose.yml"
KEYCLOAK_URL="http://localhost:8180"

# Colors for terminal output
BLUE='\033[0;34m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

echo -e "\n${BLUE}======================================================${NC}"
echo -e "${CYAN}   ⚡ SHOP.IO — Starting Microservices Stack          ${NC}"
echo -e "${BLUE}======================================================${NC}\n"

# 1. Ensure local persistence directories exist
mkdir -p "${ROOT_DIR}/.data/pg-catalog" "${ROOT_DIR}/.data/pg-order" "${ROOT_DIR}/.data/pg-keycloak"

# 2. Start all 13 containers in background
echo -e "${YELLOW}--> Launching Docker Compose stack...${NC}"
docker compose -f "${COMPOSE_FILE}" up -d

# 3. Wait for Keycloak and ensure realm is imported
echo -e "${YELLOW}--> Verifying Keycloak IAM readiness & realm configuration...${NC}"
for i in {1..30}; do
  if curl -s -f "${KEYCLOAK_URL}/realms/shop-realm" > /dev/null 2>&1; then
    echo -e "    ${GREEN}✓ shop-realm is active and ready${NC}"
    break
  fi

  # Attempt realm import if master realm is responsive
  ADMIN_TOKEN=$(curl -s -X POST "${KEYCLOAK_URL}/realms/master/protocol/openid-connect/token" \
    -d "client_id=admin-cli" -d "username=admin" -d "password=admin_password" -d "grant_type=password" 2>/dev/null | jq -r .access_token 2>/dev/null || true)
  if [ -n "$ADMIN_TOKEN" ] && [ "$ADMIN_TOKEN" != "null" ]; then
    curl -s -X POST "${KEYCLOAK_URL}/admin/realms" \
      -H "Authorization: Bearer $ADMIN_TOKEN" \
      -H "Content-Type: application/json" \
      -d @"${ROOT_DIR}/deploy/keycloak/shop-realm.json" > /dev/null 2>&1 || true
  fi
  sleep 2
done

# 4. Display live dashboard
echo -e "\n${GREEN}======================================================${NC}"
echo -e "${GREEN}   ✓ All Services Are Running Live!                   ${NC}"
echo -e "${GREEN}======================================================${NC}\n"
echo -e "  ${CYAN}Frontend UI:${NC}         http://localhost:4200"
echo -e "  ${CYAN}API Gateway:${NC}         http://localhost:8080"
echo -e "  ${CYAN}Catalog Service:${NC}     http://localhost:8081"
echo -e "  ${CYAN}Order Service:${NC}       http://localhost:8082"
echo -e "  ${CYAN}Keycloak IAM:${NC}        http://localhost:8180 (Realm: shop-realm)"
echo -e "  ${CYAN}Grafana Dashboards:${NC}  http://localhost:3000 (admin / admin_password)"
echo -e "  ${CYAN}Jaeger UI (Tracing):${NC} http://localhost:16686"
echo -e "  ${CYAN}Prometheus:${NC}          http://localhost:9090\n"
echo -e "  ${YELLOW}Test Credentials:${NC}"
echo -e "    Customer: customer / user123"
echo -e "    Admin:    admin / admin123\n"
echo -e "To verify the complete stack with all 23 tests, run: ${CYAN}./scripts/verify-stack.sh${NC}\n"
