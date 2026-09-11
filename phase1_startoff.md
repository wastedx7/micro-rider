# Phase 1 Startoff: User Service and Gateway Hardening

Phase 1 turns the Phase 0 user-service shell into the first usable identity boundary for the platform.

## Objective

Deliver registration, login, JWT access tokens, refresh tokens, and gateway routing while preserving the shared auth-claims and error conventions established in Phase 0.

## Starting point

Phase 0 provides:

- `apps/user-service` as a runnable Eureka-aware Spring Boot module;
- canonical JWT claims in the shared protobuf contract;
- canonical role values: `RIDER`, `DRIVER`, `ADMIN`, `SYSTEM`;
- a gateway JWT validator;
- a configured issuer: `micro-rider-user-service`;
- gRPC contracts for future internal communication.

## Workstreams

### 1. Account domain

- Add PostgreSQL and Spring Data JPA dependencies.
- Create account and role models.
- Add migrations for accounts, roles, and refresh tokens.
- Store password hashes only; never store plaintext passwords.
- Add unique constraints for login identifiers.

### 2. Authentication API

Implement REST endpoints in `user-service`:

- `POST /auth/register`
- `POST /auth/login`
- `POST /auth/refresh`
- `POST /auth/logout`

Validate input with Bean Validation and return the shared error-code format.

### 3. Token service

- Issue signed access tokens with `sub`, `roles`, `token_type`, `iss`, `iat`, `exp`, and `jti`.
- Keep access tokens short-lived.
- Store refresh tokens hashed or otherwise securely revocable.
- Rotate refresh tokens on use.
- Reject wrong issuer, expired tokens, wrong token type, and invalid signatures.
- Keep signing secrets outside source control.

### 4. Gateway hardening

- Add the missing `SecurityWebFilterChain`.
- Permit `/auth/**` and health endpoints.
- Require authentication for protected routes.
- Add gateway route definitions for user and downstream services.
- Propagate verified user ID, roles, and request ID.
- Strip client-provided identity headers before adding trusted values.

### 5. Testing and security

- Unit-test password hashing and token claims.
- Integration-test registration, login, refresh, logout, and duplicate accounts.
- Test unauthorized, forbidden, expired, malformed, and wrong-type tokens.
- Add repository tests against PostgreSQL or Testcontainers.
- Add rate limiting and brute-force protections to login.
- Avoid logging passwords, refresh tokens, access tokens, or sensitive claims.

## Suggested implementation order

1. Database migrations and account repository.
2. Registration service and validation.
3. JWT access-token service.
4. Login endpoint.
5. Refresh/logout lifecycle.
6. Gateway security chain and routes.
7. End-to-end authentication tests.
8. Documentation and local run instructions.

## Definition of done

- A new rider or driver can register.
- Valid credentials produce a conforming access token.
- Refresh tokens can be rotated and revoked.
- Gateway-protected routes reject missing or invalid credentials.
- Downstream services receive verified identity context.
- Tests cover the main success and failure paths.
- Secrets and credentials are supplied through environment or deployment configuration.
