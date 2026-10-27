# Architecture and Implementation Decisions

## ADR-001 — Session authentication for M2

Date: 25 October 2026.

Status: Session authentication explicitly selected by Pluto. The endpoint and lifecycle details below are proposed implementation defaults, to review before security code is introduced.

### Context and decision

The current application is one Spring Boot backend with a planned browser demonstration interface. The author has previous JWT experience but wants M2 to focus on authentication, authorisation and shop ownership. Use Spring Security with server-side HTTP sessions. JWT is not part of the M2 implementation.

Initial account provisioning uses development/test fixtures: two customers and two administrators for different shops. Public registration and merchant onboarding are deferred. Passwords are stored as BCrypt hashes; real passwords are never committed. Fixture credentials will be deliberately public, development/test-only values and will not be reused as deployment secrets.

### Proposed bounded account model

One `user_account` table stores ID, username, password hash, role, enabled flag and optional shop ID. Each account has exactly one role initially. A `CUSTOMER` has no shop ID; a `SHOP_ADMIN` belongs to exactly one shop. A shop can have several administrators. A foreign key enforces shop existence; a role/shop CHECK enforces the allowed combinations. Multiple-shop membership and combined customer/admin roles require a future model change.

Database account records are persistent business data. HTTP sessions are separate runtime authentication state; M2 does not add a database session table or Redis.

### Proposed authentication lifecycle

- Use Spring Security's form-login processing at `/api/auth/login`, accepting URL-encoded username/password fields, with JSON success/failure handlers rather than redirects. This uses the framework authentication/session persistence flow instead of implementing a login controller manually.
- Use a session cookie (normally `JSESSIONID`), HttpOnly, SameSite=Lax; use Secure over HTTPS when deployed. Local HTTP development cannot require a Secure cookie.
- Use a 30-minute inactivity timeout initially, not a fixed 30-minute lifetime from login. Sessions are in memory; restarting the application invalidates them.
- Rotate the session ID on authentication using framework protection. A valid logout invalidates the session; subsequent protected requests with the old session are unauthenticated.
- Keep CSRF protection for unsafe methods, including login/logout. Add a public token endpoint for API clients. The client retrieves a fresh token after login/logout because the security flow clears the previous token.
- Initially allow several simultaneous sessions. Global logout, remember-me, session sharing across instances and immediate revocation on account/role edits are deferred. Disabled accounts cannot log in; future account-disable management must explicitly define how existing sessions are revoked.

### Authorisation

Public browsing remains anonymous. Protected management requires `SHOP_ADMIN` plus target shop ownership, determined from trusted server-side identity/account data. A client-supplied shop ID is a resource selector, never evidence of permission. Roles alone are insufficient.

### Consequences and verification

Session lookup and logout are straightforward for this bounded deployment. Clients must preserve cookies and supply valid CSRF tokens for state changes. Multiple backend instances would require a deliberate session-sharing/routing design. JWT remains a possible later learning exercise, not a promised migration.

Tests must prove real login and session reuse, CSRF behaviour, logout invalidation, customer/admin role boundaries, cross-shop read/write denial and preservation of anonymous M1 browsing. Tests that use only mocked authenticated users are insufficient to establish the login/session lifecycle.
