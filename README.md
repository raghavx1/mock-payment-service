# Mock Payment Authorization API

A containerized Spring Boot REST API that simulates a payment gateway's authorization flow (similar to Stripe or Adyen). I built this primarily to demonstrate how to handle **idempotency** in a distributed system to prevent double-charging customers during network retries or concurrent requests.

## Tech Stack
* **Java 17 & Spring Boot 3** (REST API, Data JPA)
* **PostgreSQL** (Source of truth, transactional storage)
* **Redis** (Fast-read idempotency caching layer)
* **Docker & Docker Compose** (Containerization & orchestration)

## System Architecture: The Two-Tier Idempotency Shield
When a client (frontend or another microservice) sends a payment request, it must include an `Idempotency-Key` header. 

1. **Layer 1 (Redis):** The service first checks Redis. If the key exists, it instantly returns the cached receipt in < 2ms without hitting the database or running the payment logic twice.
2. **Layer 2 (PostgreSQL Fallback):** If Redis is down, times out, or evicts the key, the service gracefully degrades. It queries Postgres for the idempotency key. If found, it returns the existing record. If not, it processes a new payment.
3. **Database Constraint:** A unique constraint on the `idempotency_key` column in Postgres acts as the final safety net against race conditions.

## Architecture Decisions & Trade-offs

* **Explicit Serialization over Framework Defaults:** Spring Data's default `GenericJackson2JsonRedisSerializer` relies heavily on reflection. This introduces silent failure modes when deserializing into modern Java 17 immutable `record` types, often falling back to a generic `Map` and causing silent cache misses. To guarantee type safety and predictability, this service completely bypasses the default abstraction. Cache payloads are manually serialized to raw JSON strings using a strictly configured `ObjectMapper` (with `JavaTimeModule` for ISO-8601 compliance) and stored via `StringRedisSerializer`. Explicit control is prioritized over implicit framework magic.
* **Database as the Ultimate Source of Truth:** While Redis handles 99% of idempotency checks at sub-millisecond latency, cache evictions or Redis outages must not compromise transaction integrity. The `idempotency_key` column in PostgreSQL enforces a strict `UNIQUE` constraint at the disk level, acting as the final barrier against race conditions and double-charges.

## How to Run Locally

You don't need Java, Maven, or Postgres installed on your machine. You just need **Docker Desktop**.

```bash
# 1. Clone the repo and navigate to the directory
git clone <your-repo-url>
cd mock-payment-service

# 2. Spin up the API, PostgreSQL, and Redis
docker compose up --build
