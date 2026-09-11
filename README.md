# micro-rider

Micro-rider is a learning-oriented ride-hailing backend built as a Java/Spring microservice system. A rider requests a ride, nearby drivers are discovered, a driver is assigned, and status updates are delivered in real time.

## Phase 0 in plain English

Phase 0 prepares the shared foundation. It does not yet provide a complete ride-booking product.

The foundation includes:

- separate Spring Boot applications for each responsibility;
- Eureka, a service registry that lets applications discover one another;
- Kafka, used for asynchronous events such as ride status and driver location;
- Redis, planned for fast driver-location searches and distributed coordination;
- PostgreSQL, planned for durable account and ride data;
- protobuf messages shared by every service;
- gRPC contracts for synchronous internal calls;
- shared conventions for event metadata, errors, and JWT claims;
- a user-service application shell that can be started and registered, but does not yet authenticate users.

## Repository layout

```text
common/common-proto     Protobuf messages and generated gRPC contracts
common/common-events    Kafka topics, serializers, event/error/auth constants
apps/eureka-server      Service registry
apps/api-gateway        HTTP entry point and JWT validation
apps/user-service       Phase 0 shell; Phase 1 owns accounts and login
apps/ride-service       Ride lifecycle owner (future implementation)
apps/location-service   Driver location cache (future implementation)
apps/driver-assignment-service  Driver matching (future implementation)
apps/notification-service       Notification fan-out (future implementation)
apps/websocket-server-* Real-time rider/driver connections (future implementation)
phase0_plans            Plans and conventions for the foundation phase
docker-compose.yml      Local PostgreSQL, Redis, and Kafka infrastructure
```

## Local infrastructure

Install Docker Desktop, then run:

```bash
docker compose up -d
```

This starts:

| Component | Address | Purpose |
|---|---|---|
| PostgreSQL | `localhost:5432` | Durable application data |
| Redis | `localhost:6379` | Fast location/cache data |
| Kafka | `localhost:9092` | Event streaming |
| Eureka | `localhost:8761` | Started separately; service registry |

Kafka topics created by Compose are `ride-status`, `driver-location`, `driver-assignment`, and `session-ready`.

## Services and ports

| Service | HTTP | gRPC |
|---|---:|---:|
| API Gateway | 8080 | — |
| Eureka | 8761 | — |
| User service | 8085 | — |
| Ride service | 8081 | 9081 |
| Location service | 8082 | — |
| Driver assignment | 8083 | — |
| Notification | 8084 | 9084 |
| Rider WebSocket | 8090 | 9100 |
| Driver WebSocket | 8091 | 9101 |

The business services are currently mostly application shells. Their planned responsibilities are documented in [PLAN.md](PLAN.md).

## Build

The root Maven project builds all modules:

```bash
mvn clean install
```

The build generates Java protobuf and gRPC classes from `common/common-proto/src/main/proto`. If Maven cannot access its default local repository, provide a writable Maven repository with `-Dmaven.repo.local=<path>`.

## Phase 0 contracts

- [gRPC service plan](grpcPlan.md)
- [Event, error, and auth conventions](phase0_plans/event-error-auth-conventions.md)
- [User-service skeleton plan](phase0_plans/user-service-skeleton.md)
- [Phase 1 kickoff](phase1_startoff.md)

The architecture image is available at [images/image.png](images/image.png).
