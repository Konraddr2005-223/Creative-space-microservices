# Creative Space Microservices

Portfolio platform for hourly booking of creative resources (studios, equipment, workshops).

**Stack:** Java 25 · Spring Boot 4.0.3 · Spring Cloud 2025.1.0 · PostgreSQL · Kafka (KRaft) · Keycloak · Docker Compose

Goal: show distributed-systems patterns (concurrency, saga compensation, transactional outbox), not just “many Spring Boot apps”.

## Architecture highlights

| Challenge | Approach |
|-----------|----------|
| Overlapping bookings under concurrency | PostgreSQL `EXCLUDE` with `btree_gist` + `tstzrange` (`booking-service`) |
| Optimistic locking comparison | `@Version` on catalog/booking rows — protects **updates**, not overlapping **inserts** |
| Reservation → payment → confirm/compensate | Orchestrated saga: Feign + Resilience4j (`POST /api/bookings/{id}/checkout`) |
| Reliable domain events | Transactional Outbox in `booking-service` → Kafka `booking.events` |
| At-least-once delivery | Idempotent `notification-service` (`processed_events` PK = `eventId`) |
| Edge auth | Keycloak JWT validated at **API Gateway** only (for now) |

## Services & ports

| Service | Port | Role |
|---------|------|------|
| `service-discovery` (Eureka) | 8761 | Service registry |
| `api-gateway` | 8080 | Entry point + JWT |
| `resource-service` | 8081 | Catalog + price tiers |
| `booking-service` | 8082 | Bookings, EXCLUDE, saga, outbox |
| `payment-service` | 8083 | Simulated payments |
| `notification-service` | 8084 | Kafka consumer (log channel) |
| Keycloak | 8180 | OIDC / demo users |
| Kafka | 9092 | `booking.events` |
| Postgres (resource / booking / payment / notification) | 5432–5435 | Database-per-service |

## Prerequisites

- JDK 25 (`JAVA_HOME` set)
- Docker Desktop (Compose + Testcontainers)
- Maven Wrapper in repo (`./mvnw` / `mvnw.cmd`)

```bash
# Windows PowerShell example
$env:JAVA_HOME = "C:\Users\<you>\.jdks\openjdk-25"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
```

## 1. Start infrastructure

```bash
cp .env.example .env   # optional overrides
docker compose up -d
```

Wait until Postgres, Kafka, and Keycloak are healthy. Keycloak admin UI: http://localhost:8180 (`admin` / `admin`).

Imported realm `creative-space`:

| User | Password | Roles |
|------|----------|-------|
| `demo` | `demo` | USER |
| `admin-user` | `admin` | USER, ADMIN |

Public client for demos: `creative-space-public` (direct access grants / password grant enabled for local curl only).

## 2. Build

```bash
./mvnw -DskipTests package
# or run tests (Testcontainers need Docker):
./mvnw test
```

## 3. Start applications (order)

From separate terminals (or your IDE):

```bash
./mvnw -pl service-discovery spring-boot:run
./mvnw -pl api-gateway spring-boot:run
./mvnw -pl resource-service spring-boot:run
./mvnw -pl payment-service spring-boot:run
./mvnw -pl booking-service spring-boot:run
./mvnw -pl notification-service spring-boot:run
```

Check Eureka: http://localhost:8761 — all apps except Eureka itself should register.

## 4. Curl demo

### Get a JWT (password grant — demo only)

```bash
TOKEN=$(curl -s -X POST "http://localhost:8180/realms/creative-space/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=creative-space-public" \
  -d "username=demo" \
  -d "password=demo" | jq -r .access_token)
```

PowerShell:

```powershell
$body = @{
  grant_type = "password"
  client_id  = "creative-space-public"
  username   = "demo"
  password   = "demo"
}
$tokenResponse = Invoke-RestMethod -Method Post `
  -Uri "http://localhost:8180/realms/creative-space/protocol/openid-connect/token" `
  -ContentType "application/x-www-form-urlencoded" -Body $body
$TOKEN = $tokenResponse.access_token
```

### List seeded resources (via Gateway)

Seed UUIDs come from `resource-service` Flyway `V2__seed_demo_data.sql`.

```bash
curl -s "http://localhost:8080/resource-service/api/resources" \
  -H "Authorization: Bearer $TOKEN" | jq
```

### Create a booking (PENDING)

```bash
RESOURCE_ID=11111111-1111-1111-1111-111111111111

curl -s -X POST "http://localhost:8080/booking-service/api/bookings" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"resourceId\": \"$RESOURCE_ID\",
    \"userId\": \"demo\",
    \"startAt\": \"2026-11-01T10:00:00Z\",
    \"endAt\": \"2026-11-01T12:00:00Z\"
  }" | jq
```

Save `id` from the response as `BOOKING_ID`.

### Checkout success (saga → CONFIRMED → outbox → Kafka → notification)

```bash
curl -s -X POST "http://localhost:8080/booking-service/api/bookings/$BOOKING_ID/checkout" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"amount": 180.00, "currency": "PLN"}' | jq
```

Watch `notification-service` logs for a `Notification recorded: ... BOOKING_CONFIRMED ...` line (outbox poller delay ~1s).

### Checkout failure + compensation

Create another booking on a free slot, then force payment failure:

```bash
curl -s -X POST "http://localhost:8080/booking-service/api/bookings/$BOOKING_ID/checkout" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -H "X-Force-Payment-Failure: true" \
  -d '{"amount": 180.00, "currency": "PLN"}' | jq
```

Expect HTTP 409 and booking status `CANCELLED` (slot freed for EXCLUDE). A `BOOKING_CANCELLED` event should follow through the outbox.

### Overlap conflict (EXCLUDE)

Two overlapping create calls for the same `resourceId` → one `201`, one `409` with ProblemDetail about overlapping range. Automated proof: `BookingOverlapIntegrationTest` (50 parallel creates → exactly one success; requires Docker).

## Module map

```
creative-space-microservices/
  service-discovery/
  api-gateway/
  resource-service/
  booking-service/      # EXCLUDE, saga, outbox poller
  payment-service/      # simulated charge + force-failure header
  notification-service/ # idempotent Kafka consumer
  infra/keycloak/
  docker-compose.yml
```

## Known portfolio trade-offs

- JWT only at Gateway — services on the Docker/host network are not resource-servers yet.
- Saga is synchronous Feign (no Kafka for the payment step); Outbox/Kafka is for fan-out after state changes.
- Password grant is for local demos; do not use it in production.
- Docker must be running for Testcontainers integration tests; without it those tests are skipped.

## License

Portfolio / educational project.
