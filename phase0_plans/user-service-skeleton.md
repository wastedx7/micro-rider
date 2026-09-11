# User Service Skeleton Plan

This plan adds the `user-service` module required by Phase 0. It is a buildable Spring Boot/Eureka service skeleton only; registration, login, persistence, password hashing, and token issuance remain Phase 1 work.

## Goals

- Add `apps/user-service` to the root Maven reactor.
- Create a runnable Spring Boot application with a stable service name and local HTTP port.
- Register the service with Eureka.
- Provide health/readiness endpoints through Actuator.
- Establish configuration placeholders for Postgres and JWT without implementing authentication yet.
- Keep the module independent from ride and WebSocket services.

## Module structure

```text
apps/user-service/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/com/ridehailing/user/UserServiceApplication.java
    │   └── resources/application.properties
    └── test/
        └── java/com/ridehailing/user/UserServiceApplicationTests.java
```

## Maven configuration

Create `apps/user-service/pom.xml` with the root project as its parent.

Initial dependencies:

- `spring-boot-starter-web`;
- `spring-boot-starter-actuator`;
- `spring-cloud-starter-netflix-eureka-client`;
- `spring-boot-starter-validation`;
- `spring-boot-starter-test` for tests.

Do not add JPA, PostgreSQL, security, JWT, or Redis dependencies until Phase 1 requires them. This keeps the Phase 0 skeleton small and independently verifiable.

Add the Spring Boot Maven plugin so the module produces an executable jar.

## Application class

Create `UserServiceApplication` with:

- `@SpringBootApplication`;
- `@EnableDiscoveryClient`;
- a standard `main` method.

Use package `com.ridehailing.user` so future controllers, services, repositories, and security components are discovered automatically.

## Configuration

Use:

```properties
spring.application.name=user-service
server.port=8085

eureka.client.service-url.defaultZone=http://localhost:8761/eureka/
eureka.instance.prefer-ip-address=true

management.endpoints.web.exposure.include=health,info
management.endpoint.health probes.enabled=true
```

The exact HTTP port may be changed if the local environment adopts a different service map, but it must be documented in `PLAN.md` and remain distinct from the existing services.

Add optional placeholders for future configuration without requiring them at startup:

```properties
spring.config.import=optional:file:.env[.properties]
security.jwt.issuer=${JWT_ISSUER:micro-rider-user-service}
```

Do not store credentials or signing keys in the repository.

## Initial endpoint scope

Phase 0 needs no public business endpoints. Actuator health is sufficient for the skeleton.

Optional placeholder endpoint:

- `GET /internal/info` returning service name and version.

Avoid adding login or registration endpoints until the Phase 1 user-service design is finalized.

## Test plan

Add a context-load test that verifies:

- the Spring application context starts;
- the application name is `user-service`;
- no database, Kafka, Redis, or JWT secret is required for startup.

When Eureka is unavailable locally, tests should not require a running registry. Use test properties or disable discovery during the test context if Eureka auto-configuration prevents isolated startup.

## Root project integration

Add this module to the root `pom.xml` modules list, near the other application services:

```xml
<module>apps/user-service</module>
```

Update the service port table in `PLAN.md`:

```text
user-service | 8085 | —
```

Update the README service map once the module exists.

## Phase 0 definition of done

- `apps/user-service/pom.xml` exists and inherits from the root build.
- The module is included in the root Maven reactor.
- The application starts without external infrastructure.
- The service registers with Eureka when Eureka is available.
- Actuator health is available.
- A context-load test exists.
- No authentication or persistence behavior is implied to be complete.

## Phase 1 follow-up

After the skeleton is merged, implement:

1. Rider and driver account models.
2. PostgreSQL migrations and repositories.
3. Bcrypt password hashing.
4. Registration and login validation.
5. JWT access-token and refresh-token issuance using the shared auth-claims convention.
6. Role and ownership authorization.
7. Gateway routes for `/auth/**`.
