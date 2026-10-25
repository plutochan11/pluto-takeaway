# M1 Demo Guide and Evidence

Updated: 25 October 2026. Follow the root README for setup and startup.

## Repeatable manual demonstration

1. Confirm the selected `DB_URL` and `dev` profile. The Flyway exercise uses `pluto_takeaway_m1`; earlier SQL experiments used `pluto_takeaway`.
2. Open `requests.http` in IDEA and run requests with its HTTP Client, or send the equivalent browser/curl requests. Inspect HTTP status, not just the rendered page.
3. Read `/api/shops` and verify shops 1 and 2 are present and ordered by ID.
4. Read shop 1's menu: Chicken Rice, no Coffee/Lemon Tea.
5. Read shop 2's menu: Coffee and unavailable Lemon Tea, no Chicken Rice.
6. Request an absent shop ID and inspect HTTP 404. Request a nonnumeric ID and inspect HTTP 400. Body formatting is not yet a custom API contract.
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

M1 is complete on 25 October 2026. The full manual README setup was not independently replayed; developer startup checks and fresh-clone automated execution provide the stated evidence. Error-body formatting is deferred to M2, while 400/404 status behaviour is tested.

Next: agree M2 account provisioning, authentication mechanism and endpoint/error contracts before implementing protected management.
