# gRPC Service Definitions Plan

This plan covers the missing gRPC contracts in `common/common-proto`. The goal is to define stable, internal APIs for ride lifecycle operations, notifications, and WebSocket session delivery before implementing the services.

## Goals

- Add protobuf `service` definitions to `common/common-proto`.
- Reuse existing ride, assignment, and location messages where practical.
- Generate Java gRPC stubs through the existing protobuf Maven build.
- Keep the contracts internal, versionable, and independent of Spring implementation details.
- Make error handling and authentication context consistent across services.

## Services to define

### 1. RideService

Implemented by `ride-service` and called by the API Gateway or other internal services.

Planned RPCs:

- `CreateRide(CreateRideRequest) returns (RideResponse)`
- `GetRide(GetRideRequest) returns (RideResponse)`
- `CancelRide(CancelRideRequest) returns (RideResponse)`
- `GetRideStatus(GetRideStatusRequest) returns (RideStatusResponse)`

Core messages:

- `CreateRideRequest`: rider ID, pickup, dropoff, optional client request ID.
- `GetRideRequest`: ride ID and caller context.
- `CancelRideRequest`: ride ID, caller ID, and cancellation reason.
- `RideResponse`: ride ID, rider ID, driver ID when assigned, locations, status, timestamps.
- `RideStatusResponse`: ride ID, status, driver ID, and updated timestamp.

The service should return standard gRPC status codes such as `NOT_FOUND`, `INVALID_ARGUMENT`, `PERMISSION_DENIED`, `FAILED_PRECONDITION`, and `ALREADY_EXISTS` rather than embedding transport errors in every response.

### 2. NotificationService

Implemented by `notification-service` and called by assignment and ride workflows.

Planned RPCs:

- `PushDriverMatch(DriverMatchNotification) returns (NotificationResponse)`
- `PushRideStatus(RideStatusChangeEvent) returns (NotificationResponse)`

`NotificationResponse` should include:

- delivery status;
- target ID;
- an optional correlation/request ID;
- a human-readable diagnostic message for logs only.

Notification delivery should be idempotent using the event or correlation ID.

### 3. DriverSessionService

Implemented by `websocket-server-driver` and called by notification/assignment flows.

Planned RPCs:

- `RegisterDriverSession(RegisterSessionRequest) returns (SessionResponse)`
- `RemoveDriverSession(RemoveSessionRequest) returns (SessionResponse)`
- `PushDriverOffer(DriverMatchNotification) returns (SessionResponse)`

The WebSocket server remains responsible for the actual socket connection. These RPCs only manage or address the in-memory session registry.

### 4. RiderSessionService

Implemented by `websocket-server-rider` and called by `notification-service`.

Planned RPCs:

- `RegisterRiderSession(RegisterSessionRequest) returns (SessionResponse)`
- `RemoveRiderSession(RemoveSessionRequest) returns (SessionResponse)`
- `PushRideStatus(RideStatusChangeEvent) returns (SessionResponse)`

## Shared contract conventions

### Package and file layout

Add service definitions under:

```text
common/common-proto/src/main/proto/ride/ride_service.proto
common/common-proto/src/main/proto/notification/notification_service.proto
common/common-proto/src/main/proto/session/session_service.proto
```

Use Java packages matching the existing generated messages:

- `com.ridehailing.proto.ride`
- `com.ridehailing.proto.notification`
- `com.ridehailing.proto.session`

Use `java_multiple_files = true` and distinct `java_outer_classname` values.

### Request metadata

Every request that crosses a service boundary should support:

- `request_id` for tracing and idempotency;
- `user_id` or subject ID when authorization has already been performed;
- optional `roles` where downstream authorization is required.

Prefer a shared `RequestContext` message imported by all service definitions rather than duplicating these fields.

### Timestamps and identifiers

- Use `string` for UUIDs and external IDs.
- Use `int64` epoch milliseconds, matching the existing event messages.
- Preserve existing field numbers permanently once published.
- Add new fields instead of renaming or reusing old field numbers.

### Errors

Use gRPC status codes for transport-level failures. Define a small shared `ErrorDetail` message only where callers need structured application details, for example:

- error code;
- retryable flag;
- correlation ID;
- invalid field name.

Do not return successful responses containing an error status.

## Implementation sequence

1. Add a shared `common.proto` containing `RequestContext`, `ErrorDetail`, and generic response metadata if needed.
2. Add `ride_service.proto` and reuse `Location`, `RideStatus`, and existing ride events.
3. Add `notification_service.proto` and import existing assignment and ride event messages.
4. Add `session_service.proto` with shared session registration/removal messages.
5. Update `common-proto/pom.xml` only if the current protobuf plugin does not discover the new files automatically.
6. Run Maven generation and confirm Java message and `*Grpc` stub classes are produced.
7. Add compile-only tests or descriptor checks for service names, RPC names, and key field types.
8. Update each service module with the generated `common-proto` dependency and document the intended server/client ownership.

## Compatibility and security checks

- Do not place JWT secrets or raw access tokens in protobuf messages.
- Treat `user_id` and roles as claims propagated from the authenticated gateway context, not as trusted client input.
- Validate ownership and roles again in the receiving service.
- Ensure retry-safe RPCs have request or event IDs.
- Decide whether each RPC is safe to retry before adding client retries.
- Use deadlines on all cross-service calls.

## Definition of done

- All planned services and RPCs exist in `.proto` files.
- `mvn clean install` generates and compiles Java protobuf and gRPC classes.
- Existing event messages remain source-compatible.
- Service ownership and caller relationships are documented.
- Request context, error behavior, idempotency, and authentication propagation are documented.
- At least one generated-stub compilation check exists for each service.

## Follow-up implementation work

After the contracts are merged:

- implement `RideService` in `ride-service`;
- implement notification RPCs in `notification-service`;
- implement driver and rider session RPCs in the WebSocket services;
- add gateway-to-ride client integration;
- add integration tests covering ride creation, driver match notification, and rider status delivery.
