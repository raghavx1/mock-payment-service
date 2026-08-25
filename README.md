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

## Lessons Learned: Java 17 Records vs. Spring Magic
While building this, I ran into a classic serialization trap. Spring's default `GenericJackson2JsonRedisSerializer` tries to be clever, but it completely breaks when trying to deserialize into Java 17 `record` types because they are immutable and `final`. It would silently fall back to parsing the JSON into a generic `Map`, bypassing the cache entirely. 

**The Fix:** I stripped out the framework magic, switched to `StringRedisSerializer`, and manually serialized/deserialized the payloads using a custom `ObjectMapper` with the `JavaTimeModule` enabled. Explicit control > implicit magic.

## How to Run Locally

You don't need Java, Maven, or Postgres installed on your machine. You just need **Docker Desktop**.

```bash
# 1. Clone the repo and navigate to the directory
git clone <your-repo-url>
cd mock-payment-service

# 2. Spin up the API, PostgreSQL, and Redis
docker compose up --build
