# M1/M2 Demo Guide and Evidence

Updated: 3 November 2026. Follow the root README for setup and startup.

## Repeatable manual demonstration

1. Confirm the selected `DB_URL` and `dev` profile. The Flyway exercise uses `pluto_takeaway_m1`; earlier SQL experiments used `pluto_takeaway`.
2. Open `requests.http` in IDEA and run requests with its HTTP Client, or send the equivalent browser/curl requests. Inspect HTTP status, not just the rendered page.
3. Read `/api/shops` and verify shops 1 and 2 are present and ordered by ID.
4. Read shop 1's menu: Chicken Rice, no Coffee/Lemon Tea.
5. Read shop 2's menu: Coffee and unavailable Lemon Tea, no Chicken Rice.
6. Request an absent shop ID and inspect HTTP 404. Request a nonnumeric ID and inspect HTTP 400. These cases now return the M2 `{code,message}` error JSON.
7. If testing empty data/ordering, import `Data Model/menu-test-fixtures.sql` once into the CURRENT target database, then request shops 9001 and 9002. Do not import the full schema draft into a Flyway-managed database.
8. Shop 9001 must return an empty categories array. Shop 9002 must return Drinks (9102), Main Courses (9103), Desserts (9101), retaining empty product arrays. Only Drinks contains Test Coffee.
9. Inspect logs for one shop query, one category query and one product query per successful menu request. The three query statements were observed during agent test execution; no automated query-count assertion is installed.
10. Restart the application, confirm Flyway reports no pending migration, inspect history and compare application row counts before/after restart.

The manual fixtures are not part of dev V2. Files copied with `docker compose cp` are snapshots; recopy after editing. Failed multi-statement imports may leave earlier INSERT statements committed. Inspect existing records before retrying instead of rerunning all inserts blindly.

## Evidence recorded so far

| Check | Source | Result / limits |
|---|---|---|
| Database constraints and defaults | Agent executed SQL on 20 October; saved output under Data Model | 11 expected outcomes passed against original `pluto_takeaway`, MySQL 8.4.8; test rows rolled back |
| Entity, repository, service, DTO and controller paths | Agent static source review | No correctness issue found in reviewed menu flow; not an automated execution result |
| Shop listing, details and both menus | Developer browser verification | Reported working; independent HTTP response capture pending |
| Empty shop, empty categories and category ordering | Developer browser verification | Reported working after correcting stale container fixture copy; earlier exercise DB, not proof that fixtures exist in migration DB |
| First Flyway setup | Developer inspected history | V1/V2 records observed; migration files and dev locations reviewed by agent |
| Second Flyway startup | Developer-provided log | `Schema pluto_takeaway_m1 is up to date. No migration necessary.` |
| Complete setup using README alone | Not yet independently run | Pending |
| Automated API integration tests on migration-created database | Agent execution on 24 October | 8 tests, 0 failures, 0 errors; real Testcontainers MySQL with Flyway V1 and test V2 |
| HTTP 400/404 assertions | Executed integration tests | Both pass; query count observed in logs, not an automated assertion |
| Git history | Local history inspected | c47855d implementation; 4df9f85 integration tests; developer performs commit/push commands |
| Fresh GitHub clone and full mvn test | Developer report on 25 October | 8 tests pass without dev database setup; no independent clean-machine claim |

## Automated run and next milestone

Run `mvn test` in the backend directory with Docker Desktop running. The test profile uses its own MySQL container and fixture migration. Do not import the manual fixture files into this test container; test V2 already contains those records.

M1 completed on 25 October 2026. The bounded M2 backend completed on 3 November 2026. The full manual README setup was not independently replayed; the evidence below distinguishes automated checks from browser observations.

## M2 local browser demonstration

Run the backend with the README's dev profile and migrated database. Use one browser tab at `http://localhost:8080/api/hello`; open DevTools Console and Network. Keep the same host throughout. The values below are deliberately public dev fixtures; do not use real credentials in saved console examples.

1. Obtain CSRF state. Inspect the response JSON and, if a new Session was needed, `Set-Cookie`. Existing sessions need not receive another cookie.

```javascript
var csrf = await (await fetch('/api/auth/csrf')).json();
```

2. Login with valid CSRF. Same-origin fetch sends applicable cookies automatically. The client must explicitly send the CSRF header.

```javascript
var login = await fetch('/api/auth/login', {
  method: 'POST',
  headers: { [csrf.headerName]: csrf.token },
  body: new URLSearchParams({ username: 'admin_a', password: 'password' })
});
console.log(login.status, await login.json());
```

Expect 200 and admin_a / SHOP_ADMIN / shopId 1, with no password/hash. Browser-managed `JSESSIONID` may change at login. Never hardcode cookie or token values.

3. Read the current identity and management boundaries:

```javascript
console.log(await (await fetch('/api/auth/me')).json());
console.log((await fetch('/api/admin/shops/1')).status); // 200
console.log((await fetch('/api/admin/shops/2')).status); // 403
```

4. Fetch a fresh CSRF token after login. A valid name update should return 200. Record the original name so the demo can restore it.

```javascript
var originalShop = await (await fetch('/api/admin/shops/1')).json();
csrf = await (await fetch('/api/auth/csrf')).json();
var update = await fetch('/api/admin/shops/1', {
  method: 'PATCH',
  headers: { 'Content-Type': 'application/json', [csrf.headerName]: csrf.token },
  body: JSON.stringify({ name: 'Pluto Demo Name' })
});
console.log(update.status, await update.json());
```

5. Repeat the update without the CSRF header: expect 403 / CSRF_INVALID and unchanged data. With valid CSRF but `name: '   '`, expect 400 / VALIDATION_FAILED. With valid input/token targeting shop 2, expect 403 / ACCESS_DENIED. Inspect Network response bodies, not only status.
6. Restore the name before logout:

```javascript
await fetch('/api/admin/shops/1', {
  method: 'PATCH',
  headers: { 'Content-Type': 'application/json', [csrf.headerName]: csrf.token },
  body: JSON.stringify({ name: originalShop.name })
});
var logout = await fetch('/api/auth/logout', {
  method: 'POST', headers: { [csrf.headerName]: csrf.token }
});
console.log(logout.status); // 204; no JSON body to parse
console.log((await fetch('/api/auth/me')).status); // 401
```

To demonstrate customer denial, obtain a new token and login as customer_a; management GET must return 403. Admin_b owns shop 2. Restarting the app loses in-memory sessions: re-fetch CSRF and login rather than reusing stale values.

## M2 evidence and limitations

| Check | Evidence |
|---|---|
| Security and MVC error responses, core login/ownership/write paths | Agent ran `mvn test` on 2 November: 40 tests, 0 failures/errors |
| Unknown JSON assignment fields, Session ID rotation and disabled login | Source reviewed; developer reports final 43-test suite passing; local Surefire report corroborated on 3 November: 43, 0 failures/errors |
| Session cookie and CSRF browser flow | Developer observed cookie state, missing/invalid-token rejection and successful fresh-token login; `/error` dispatch masking was corrected |
| Database isolation | Independent MySQL Testcontainer; Flyway V1/V3 + test V2/V4; name-mutation scripts reset fixture names before/after tests; disabled-account test restores enabled state |
| Not claimed | Final browser demo above fully replayed; fresh M2 clone/setup; actual container idle expiration; old-cookie replay; deployed HTTPS cookie settings; concurrent execution |

MockMvc uses real filters/controllers/services/repositories but mock Servlet objects. It does not verify browser cookie transport. The final logout test asserts the old mock Session is invalid, then makes an anonymous protected request; it does not simulate Tomcat looking up an old cookie.

The configured timeout/cookie policy is 30 minutes of inactivity, HttpOnly and SameSite=Lax. Deployment must use Secure cookies over HTTPS. Account-role edits do not immediately revoke existing sessions; there is no Redis/distributed Session store, public registration or remember-me.

Next milestone: M3 menu/configurable-meal rules, beginning with a written pricing/selection contract before schema changes.
