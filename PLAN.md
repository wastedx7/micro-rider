# micro-rider — Build Plan

A microservice-based ride-hailing backend (Spring Boot 4 / Java 21, gRPC-first, Kafka, Redis, Postgres, Eureka).

## Current state (as of planning)

- `common/common-proto` — message protos only (`ride_events`, `location_update`, `assignment_events`); **no gRPC service definitions yet**
- `common/common-events` — Kafka topics + Protobuf (de)serializers: done
- `eureka-server` — done
- `api-gateway` — JWT validation filter + UUID header propagation done; **missing route config, security filter chain bean, token issuance**
- All 6 business services (`ride`, `location`, `driver-assignment`, `notification`, both websocket servers) — empty shells with pre-wired config only
- Infra via docker-compose: Postgres 17, Redis 7, Kafka 3.8 (KRaft) with topics `ride-status`, `driver-location`, `driver-assignment`, `session-ready`

---

## Phase 0 — Foundations & shared contracts

- Add gRPC service definitions to `common-proto`:
  - `RideService` (create/get/cancel/status)
  - `NotificationService` (push match/status notifications)
  - WS session RPCs (register session, push to session)
- Define event envelope conventions, error codes, and auth claims shape
- Add `user-service` module skeleton under `apps/`
- Verify root build (`mvn clean install`) and infra (`docker compose up -d`)

## Phase 1 — User service & gateway hardening

- **User service** (new):
  - Rider/driver accounts, credentials storage (bcrypt), Postgres persistence
  - Login/registration REST endpoints; issue signed JWTs (userId + roles)
  - Refresh token flow
- **API Gateway**:
  - Route definitions to downstream services
  - Register `SecurityWebFilterChain` bean wiring the existing JWT auth manager
  - Forward `/auth/**` routes to user-service; rate-limiting basics

## Phase 2 — Ride Service core

- Add Postgres/JPA dependencies; ride entity + repository
- Ride status state machine: `REQUESTED → ACCEPTED → IN_PROGRESS → COMPLETED | CANCELLED`
- gRPC server implementing `RideService` RPCs (called by gateway)
- Publish `RideRequestedEvent` / `RideStatusChangeEvent` to Kafka topic `ride-status`

## Phase 3 — Websocket servers

- **websocket-server-driver**:
  - Session registry keyed by driverId; JWT handshake auth
  - Ingest location pings → publish `DriverLocationUpdate` to Kafka `driver-location`
  - Consume assignment notifications → push offers to drivers; accept/reject responses
- **websocket-server-rider**:
  - Consume `RideStatusChangeEvent` → push status updates to rider sessions
- Use `session-ready` topic for session lifecycle coordination

## Phase 4 — Location Service

- Kafka consumer for `DriverLocationUpdate`
- Redis GEO store (`GEOADD` / `GEOSEARCH`) as the driver-location cache
- Expose internal query API (gRPC) for "drivers near pickup point"

## Phase 5 — Driver Assignment Service

- Consume `DriverAssignmentRequest` → query Location Service for candidate drivers
- Redisson **distributed lock** per ride/driver to prevent double assignment
- Offer / accept / timeout matching flow with retry over next-nearest candidates
- On match: emit `DriverMatchNotification` → notification-service; trigger ride status change

## Phase 6 — Notification Service

- gRPC server receiving match/status notifications
- Forward notifications to the correct websocket server (driver/rider) via its gRPC API

## Phase 7 — End-to-end flows & resilience

- Full lifecycle E2E test: rider requests → driver matched → status pushed both ways
- Retries, dead-letter topics, idempotent consumers
- Health checks, graceful shutdown, connection recovery
- Observability: structured logging, actuator metrics

## Phase 8 — Polish

- Integration tests per service
- CI workflow (build + tests on PR)
- Update README with architecture diagram reference and service map

---

## Service port map

| Service | HTTP | gRPC |
|---|---|---|
| api-gateway | 8080 | — |
| eureka-server | 8761 | — |
| ride-service | 8081 | 9081 |
| location-service | — | (TBD) |
| driver-assignment-service | — | (client) |
| notification-service | 8084 | 9084 |
| user-service | 8085 | — |
| websocket-server-rider | 8090 | 9100 |
| websocket-server-driver | 8091 | 9101 |

## Infra

| Component | Port |
|---|---|
| Postgres | 5432 |
| Redis | 6379 |
| Kafka | 9092 (host) / 29092 (internal) |

Kafka topics: `ride-status`, `driver-location`, `driver-assignment`, `session-ready`
