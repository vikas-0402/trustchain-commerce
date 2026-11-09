# TrustChain Commerce

Blockchain-backed, event-driven order and payment platform
(Java 21, Spring Boot 4.1, PostgreSQL, Kafka, Redis, Solidity).

**Status: Phase 1 (foundation).** Only the Order and Inventory services exist, each with its own
PostgreSQL database. They do not talk to each other yet. Kafka and Redis are configured in Docker Compose (they start
only when you run it) but no application code uses them yet. Payment, saga, smart contracts and the audit service are **planned**,
not implemented. See [docs/](docs/) for what is implemented versus planned.

## Prerequisites

JDK 21, Maven 3.9+, Docker (for the database and for the integration tests).

## Build and test

Build everything and run all tests:

    mvn clean verify

Build a single service (plus the `common` module it depends on):

    mvn -pl services/order-service -am clean package
    mvn -pl services/inventory-service -am clean package

Unit and web-layer tests need no Docker. The `*IntegrationTest` classes start a throwaway PostgreSQL
container with Testcontainers and are skipped automatically when Docker is not available.

## Run

Start the infrastructure, then run the packaged jars:

    docker compose up -d postgres redis kafka

    java -jar services/order-service/target/order-service-0.1.0-SNAPSHOT.jar
    java -jar services/inventory-service/target/inventory-service-0.1.0-SNAPSHOT.jar

Or run everything in Docker (builds the images):

    docker compose up --build

Note: do not use `mvn -am spring-boot:run`. `-am` also selects the `common` library module, which
has no main class, so the goal fails there. Package first and run the jar, or run the application
class from your IDE.

## Ports

| Component         | Port |
|-------------------|------|
| order-service     | 8081 |
| inventory-service | 8082 |
| PostgreSQL        | 5432 |
| Redis             | 6379 |
| Kafka             | 9092 |

Swagger UI: http://localhost:8081/swagger-ui.html and http://localhost:8082/swagger-ui.html

## Try it

    curl http://localhost:8082/products

    curl -X POST http://localhost:8081/orders \
      -H "Content-Type: application/json" \
      -d '{"customerId":"cust-1","items":[{"productId":"11111111-1111-1111-1111-111111111111","quantity":1,"unitPrice":899.00}]}'

    curl http://localhost:8081/orders

Check Postgres:

    docker exec -it tc-postgres psql -U trustchain -d orderdb -c "select * from orders;"

## Phase 1 limitations (intentional)

- `unitPrice` is supplied by the client and is **not** validated against inventory. This is a
  temporary simplification until the Order service integrates with the Inventory service.
- Creating an order does **not** check or reserve stock. Orders are stored as `PENDING` and stay `PENDING`.
- No authentication, no payments, no events, no blockchain.

## Local credentials

The `trustchain/trustchain` Postgres credentials in `docker-compose.yml` and `application.yml` are
**development-only** defaults. Copy `.env.example` to `.env` to override them locally. `.env`, key
files and keystores are git-ignored. Never commit real secrets or wallet private keys.
