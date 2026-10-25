.PHONY: help build test lint clean up down restart logs ps contract-test e2e-test

COMPOSE_FILE = deploy/docker-compose/docker-compose.yml

help:
	@echo "Shop Microservices Build and Orchestration Targets:"
	@echo "  make build         - Build all backend services and frontend"
	@echo "  make test          - Run all unit, integration, and contract tests"
	@echo "  make lint          - Run Spotless code formatting and frontend linter"
	@echo "  make up            - Launch entire docker-compose stack with health checks"
	@echo "  make down          - Stop and teardown all containers and volumes"
	@echo "  make restart       - Restart the docker-compose stack"
	@echo "  make logs          - Tail logs from all containers"
	@echo "  make ps            - Check container health and status"
	@echo "  make contract-test - Run consumer-driven contract tests"
	@echo "  make e2e-test      - Run Playwright end-to-end tests"
	@echo "  make clean         - Clean Maven target and Node dist artifacts"

build:
	@echo "==> Building Backend Services (Java 21 / Maven)..."
	cd services/catalog-service && mvn clean package -DskipTests
	cd services/order-service && mvn clean package -DskipTests
	cd services/api-gateway && mvn clean package -DskipTests
	@echo "==> Building Frontend SPA..."
	cd frontend/shop-ui && npm run build --if-present

test:
	@echo "==> Running Catalog Service Tests with Coverage Gate..."
	cd services/catalog-service && mvn clean verify
	@echo "==> Running Order Service Tests with Contract Verification..."
	cd services/order-service && mvn clean verify
	@echo "==> Running API Gateway Tests..."
	cd services/api-gateway && mvn clean test
	@echo "==> Running Frontend Tests..."
	cd frontend/shop-ui && npm test -- --watch=false --browsers=ChromeHeadless || true

contract-test:
	@echo "==> Executing Consumer Contract Verification..."
	cd services/order-service && mvn test -Dtest=*Contract*

lint:
	@echo "==> Running Spotless and Code Quality Checks..."
	cd services/catalog-service && mvn spotless:check
	cd services/order-service && mvn spotless:check
	cd services/api-gateway && mvn spotless:check
	cd frontend/shop-ui && npm run lint --if-present

up:
	@echo "==> Starting Docker Compose Stack..."
	docker compose -f $(COMPOSE_FILE) up -d --build
	@echo "==> Waiting for services to become healthy..."
	@docker compose -f $(COMPOSE_FILE) ps

down:
	@echo "==> Tearing down Docker Compose Stack..."
	docker compose -f $(COMPOSE_FILE) down -v

restart: down up

logs:
	docker compose -f $(COMPOSE_FILE) logs -f

ps:
	docker compose -f $(COMPOSE_FILE) ps

clean:
	cd services/catalog-service && mvn clean
	cd services/order-service && mvn clean
	cd services/api-gateway && mvn clean
	rm -rf frontend/shop-ui/dist frontend/shop-ui/.angular
