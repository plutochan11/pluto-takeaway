# M2 Identity and Shop Ownership — API Design

Created: 25 October 2026. Updated: 3 November 2026. Status: implemented bounded M2 backend contract; verification limits below. Read together with ADR-001 in `Decisions.md`.

## Endpoints

| Method | Path | Access | Successful behaviour |
|---|---|---|---|
| GET | `/api/auth/csrf` | Public | 200, token metadata JSON; creates CSRF/session state as needed |
| POST | `/api/auth/login` | Public, valid CSRF required | 200, current-user DTO and authenticated session cookie |
| GET | `/api/auth/me` | Authenticated | 200, current-user DTO |
| POST | `/api/auth/logout` | Valid CSRF required | 204, invalidate current session; no redirect |
| GET | `/api/admin/shops/{shopId}` | SHOP_ADMIN owning target shop | 200, ShopResponse |
| PATCH | `/api/admin/shops/{shopId}` | SHOP_ADMIN owning target shop, valid CSRF | 200, updated ShopResponse; initially update name only |

Existing M1 GET browsing endpoints remain public. Session credentials are carried in cookies; do not introduce JWT/Bearer authentication or HTTP Basic for this milestone.

## Requests and responses

Login content type: `application/x-www-form-urlencoded`, with `username` and `password` fields. This is an API contract using Spring Security form-login processing; a generated HTML login page is not required. Login is not JSON in this baseline.

CSRF response shape:

```json
{"headerName":"X-CSRF-TOKEN","parameterName":"_csrf","token":"runtime-token"}
```

Use the returned header/parameter name and token; do not hardcode runtime token values. Fetch the token using the same cookie session used for the subsequent login or state-changing request. Fetch a fresh token after authentication/logout.

Current-user DTO shape (example administrator):

```json
{"id":103,"username":"admin_a","role":"SHOP_ADMIN","shopId":1}
```

For customers, `shopId` is null. Never return a password/hash or a session identifier in this DTO. Example IDs are illustrative, not reserved fixture IDs.

Shop PATCH body:

```json
{"name":"Updated Shop Name"}
```

Name must be nonblank and at most 100 characters. The route selects the target shop; account identity, role and owning shop come from the server. The update DTO has no owner/account/shop assignment fields. Unknown JSON properties are deliberately ignored via the request DTO. Extra `id`, `shopId` and `status` do not change the selected shop, shop status or account ownership; an integration test verifies this behaviour. Missing/null/blank names are rejected; 100 characters are accepted and 101 rejected.

## Implemented error contract

Use JSON `{ "code": "...", "message": "..." }` for the following cases, without redirects, secrets or database details:

| Situation | HTTP status | Code |
|---|---|---|
| Protected request without valid authentication | 401 | `AUTHENTICATION_REQUIRED` |
| Bad username/password or disabled account at login | 401 | `INVALID_CREDENTIALS` |
| Authenticated customer calls management; administrator targets another shop | 403 | `ACCESS_DENIED` |
| Missing/invalid CSRF on an unsafe request | 403 | `CSRF_INVALID` |
| Invalid bound input or shop-name validation failure | 400 | `VALIDATION_FAILED` |
| Permitted actor targets a resource that does not exist | 404 | `RESOURCE_NOT_FOUND` |

CSRF checking can reject a request before authentication/ownership logic. A test intended to verify 401 or a role/ownership error on PATCH must provide valid CSRF; dedicated CSRF tests intentionally omit or corrupt it. GET checks require no CSRF.

Bad credentials share one generic response so login does not disclose whether a username exists. A valid-CSRF logout with no authenticated user may still return 204; a stale invalid CSRF token may return 403. Verify old-session access using a protected GET after successful logout.

## Acceptance matrix

1. Anonymous M1 browsing still returns 200.
2. Real customer/admin login succeeds, returns DTO and establishes reusable session state.
3. Wrong credentials and disabled accounts cannot establish authenticated state.
4. `/me` returns server-derived identity; missing/expired/invalidated sessions return 401.
5. Admin A can read/update shop A; admin A cannot read/update shop B.
6. A customer cannot access management endpoints.
7. Failed writes preserve the target shop's name.
8. Missing/invalid CSRF prevents a write; valid CSRF permits an otherwise authorised write.
9. Login rotates an existing pre-authentication session ID; valid logout invalidates authentication.
10. Tests exercise actual login/session persistence as well as permission rules.

## Verification record — 3 November 2026

The bounded M2 backend completion gate is met. The developer reports 43 passing tests, and the reviewed local Surefire report records 43 tests with zero failures/errors. Agent execution on 2 November independently established the earlier 40-test suite; the final 43-test run is developer/report evidence.

| Acceptance area | Evidence and limits |
|---|---|
| Public browsing, login, current identity, role/ownership reads/writes | Real login + Session reuse in MockMvc against Testcontainers MySQL; customers denied; A/B reads tested both directions; cross-shop writes denied |
| Password/account state | Correct/wrong password and disabled-account login; unknown username lookup/adapter tested, but no dedicated unknown-username HTTP login case |
| Write safety | DB queried after successful/rejected writes; extra JSON fields cannot change status/ownership; name boundaries and malformed JSON covered |
| CSRF | Missing/invalid tokens rejected with `CSRF_INVALID`; valid token taken from the same Session permits authorised operations |
| Session lifecycle | Mock Session ID changes on login; logout invalidates the authenticated mock Session; subsequent anonymous protected GET returns 401 |
| Expiration and transport | 30-minute inactivity timeout, HttpOnly and SameSite=Lax configured; actual Tomcat timeout, old-cookie replay and deployed HTTPS/Secure behaviour not integration-tested |

No listening HTTP server is started by MockMvc. Browser observations confirmed cookie creation, missing/invalid CSRF rejection and successful login in local development; a complete fresh manual setup replay has not been performed. Existing sessions are not immediately revoked on account edits, and sessions do not survive application restart. See ADR-001 for scope.
