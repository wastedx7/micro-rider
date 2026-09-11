# Event Envelope, Error-Code, and Auth-Claims Conventions

This plan defines the shared conventions required before implementing the ride-hailing services. The conventions should be represented in protobuf/common Java code where they cross service boundaries, while transport-specific behavior remains in each service.

## 1. Event envelope convention

### Purpose

All Kafka events should carry consistent metadata for tracing, ordering, replay, idempotency, and schema identification.

### Proposed envelope

Add `common/common.proto` or a dedicated `events/event_envelope.proto` with:

```proto
message EventEnvelope {
  string event_id = 1;
  string event_type = 2;
  string event_version = 3;
  string aggregate_id = 4;
  string aggregate_type = 5;
  string correlation_id = 6;
  string causation_id = 7;
  string producer = 8;
  int64 occurred_at_epoch_ms = 9;
  string subject_id = 10;
  bytes payload = 11;
}
```

### Rules

- `event_id` is globally unique and is used for consumer deduplication.
- `event_type` uses a stable name such as `ride.requested` or `ride.status_changed`.
- `event_version` starts at `v1`; incompatible changes require a new version.
- `aggregate_id` identifies the ride, assignment, driver, or session affected.
- `correlation_id` links a complete business flow; `causation_id` identifies the event that caused this event.
- `producer` is the logical service name, matching `spring.application.name`.
- `occurred_at_epoch_ms` is assigned by the producer and is not rewritten by consumers.
- `subject_id` is the authenticated user or system subject when applicable.
- Kafka message keys should normally use `aggregate_id` to preserve per-aggregate ordering.
- Consumers must be idempotent and retain processed event IDs for the required replay window.
- Sensitive data, JWTs, and secrets must never be placed in event metadata or payloads.

### Serialization decision

Use protobuf for the envelope and payload. Prefer typed event messages over arbitrary JSON. The existing `ProtobufSerializer` and `ProtobufDeserializer` should be extended or paired with a typed envelope serializer so the message type is unambiguous at consumption time.

### Topic mapping

| Topic | Event types | Aggregate key |
|---|---|---|
| `ride-status` | `ride.requested`, `ride.status_changed` | `ride_id` |
| `driver-location` | `driver.location_updated` | `driver_id` |
| `driver-assignment` | `driver.assignment_requested`, `driver.match_notified` | `ride_id` or `assignment_id` |
| `session-ready` | `session.ready`, `session.closed` | `session_id` |

## 2. Error-code convention

### Goals

- Map consistently to gRPC and HTTP transport statuses.
- Make errors searchable and safe to expose to clients.
- Distinguish retryable failures from validation and authorization failures.

### Code format

Use uppercase, namespaced codes:

```text
<DOMAIN>_<CONDITION>
```

Examples:

- `AUTH_INVALID_TOKEN`
- `AUTH_FORBIDDEN`
- `RIDE_NOT_FOUND`
- `RIDE_INVALID_STATE`
- `RIDE_ALREADY_CANCELLED`
- `DRIVER_NOT_AVAILABLE`
- `ASSIGNMENT_TIMEOUT`
- `SESSION_NOT_CONNECTED`
- `DEPENDENCY_UNAVAILABLE`
- `INTERNAL_ERROR`

### Required error fields

The shared `ErrorDetail` should support:

- `code`: stable machine-readable code;
- `message`: safe, short diagnostic text;
- `retryable`: whether the caller may retry;
- `correlation_id`: request/event trace ID;
- `field`: invalid request field when relevant.

Do not expose stack traces, SQL details, tokens, or internal hostnames to clients.

### Transport mapping

| Error category | gRPC status | HTTP status | Retry |
|---|---:|---:|---|
| Invalid input | `INVALID_ARGUMENT` | 400 | No |
| Missing authentication | `UNAUTHENTICATED` | 401 | No |
| Insufficient permission | `PERMISSION_DENIED` | 403 | No |
| Resource missing | `NOT_FOUND` | 404 | No |
| State conflict | `FAILED_PRECONDITION` | 409 | No |
| Rate limited | `RESOURCE_EXHAUSTED` | 429 | After backoff |
| Dependency unavailable | `UNAVAILABLE` | 503 | Yes |
| Deadline exceeded | `DEADLINE_EXCEEDED` | 504 | Carefully |
| Unexpected failure | `INTERNAL` | 500 | Only with idempotency |

### Implementation rules

- Use native gRPC status codes for transport behavior.
- Attach structured `ErrorDetail` metadata using standard gRPC error details where supported.
- REST gateway responses should translate the same code and correlation ID.
- Log the full internal exception server-side with the correlation ID.
- Retry only operations explicitly designed to be idempotent.

## 3. Authentication and authorization claims

### JWT claims

The user service should issue signed JWTs with this minimum shape:

```json
{
  "sub": "user-uuid",
  "roles": ["RIDER"],
  "token_type": "access",
  "iss": "micro-rider-user-service",
  "iat": 1700000000,
  "exp": 1700003600,
  "jti": "token-uuid"
}
```

Optional claims:

- `tenant_id` for future tenant isolation;
- `driver_id` when a driver identity differs from the user identity;
- `scope` for fine-grained permissions.

### Claim rules

- `sub` is the canonical authenticated subject and must be a UUID/string ID.
- `roles` is always an array; do not alternate between `role` and `roles`.
- Roles use uppercase stable values: `RIDER`, `DRIVER`, `ADMIN`, `SYSTEM`.
- `token_type` distinguishes access tokens from refresh tokens.
- `iss`, `iat`, `exp`, and `jti` are mandatory for access tokens.
- Access tokens must be short-lived; refresh tokens use a separate flow and storage policy.
- Services validate signature, issuer, expiration, and token type before trusting claims.
- A downstream service must not trust a user ID supplied solely in the request body when a verified subject exists.

### Gateway propagation

After JWT validation, the gateway may propagate non-secret identity context:

- `X-User-ID`: verified `sub`;
- `X-User-Roles`: normalized roles, if needed for HTTP downstream calls;
- `X-Request-ID`: correlation/request ID.

These headers are trusted only from the gateway network path. Downstream services should strip or overwrite client-supplied copies at the edge.

For gRPC, prefer metadata or the shared `RequestContext`; never forward the raw JWT unless a service explicitly needs to perform independent token validation.

### Authorization checks

- Gateway authorization handles coarse route access.
- The owning service performs resource-level authorization.
- Riders may access their own rides; drivers may access assigned rides and driver operations.
- Admin/system roles are explicit and must not be inferred from any other role.
- Internal service calls should use authenticated service identity or mTLS when introduced.

## Implementation sequence

1. Finalize the envelope and shared error/auth protobuf messages.
2. Add constants/enums for event types, error codes, and roles.
3. Update Kafka serialization to preserve envelope metadata and typed payloads.
4. Update `JwtService` to require the canonical claims shape and handle missing `roles` safely.
5. Add gateway request/correlation ID propagation.
6. Add shared exception-to-gRPC/HTTP mapping utilities.
7. Add contract tests for event round trips, error mappings, and JWT claim validation.

## Definition of done

- One documented envelope format is used by all Kafka events.
- Every error has a stable code, transport mapping, and retry policy.
- JWT issuer and consumers agree on mandatory claims and role values.
- User identity and correlation IDs are propagated consistently across HTTP, gRPC, and Kafka.
- Sensitive credentials are excluded from logs, metadata, events, and error responses.
- Contract tests reject incompatible event versions and malformed auth claims.
