# M1 Demo Guide and Evidence

Updated: 22 October 2026. Follow the root README for setup and startup.

## Repeatable manual demonstration

1. Confirm the selected `DB_URL` and `dev` profile. The Flyway exercise uses `pluto_takeaway_m1`; earlier SQL experiments used `pluto_takeaway`.
2. Open `requests.http` in IDEA and run requests with its HTTP Client, or send the equivalent browser/curl requests. Inspect HTTP status, not just the rendered page.
3. Read `/api/shops` and verify shops 1 and 2 are present and ordered by ID.
4. Read shop 1's menu: Chicken Rice, no Coffee/Lemon Tea.
5. Read shop 2's menu: Coffee and unavailable Lemon Tea, no Chicken Rice.
6. Request an absent shop ID and inspect HTTP 404. Request a nonnumeric ID and inspect HTTP 400. Body formatting is not yet a custom API contract.
7. If testing empty data/ordering, import `Data Model/menu-test-fixtures.sql` once into the CURRENT target database, then request shops 9001 and 9002. Do not import the full schema draft into a Flyway-managed database.
8. Shop 9001 must return an empty categories array. Shop 9002 must return Drinks (9102), Main Courses (9103), Desserts (9101), retaining empty product arrays. Only Drinks contains Test Coffee.
9. Inspect logs for one shop query, one category query and one product query per successful menu request. This query count is predicted by static review but not yet independently recorded as measured evidence.
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
| Automated API integration tests on migration-created database | Not implemented | Pending |
| Query count, HTTP 400/404 assertions | Static expectation only | Saved execution evidence pending |
| Git repository / first commit | No project-root .git found during inspection | Pending |

## Next learning task

Introduce database-backed API integration tests on a separate test database. Cover shop isolation, unavailable products, unknown/invalid IDs and empty-menu behaviour. Keep test fixtures separate from dev data. Then verify this README from a clean setup and initialise Git before marking M1 complete.
