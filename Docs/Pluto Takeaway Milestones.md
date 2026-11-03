# Pluto Takeaway — Milestones and Execution Plan

| Field                  | Value                                                                                     |
| ---------------------- | ----------------------------------------------------------------------------------------- |
| Version                | 1.0                                                                                       |
| Created / last updated | Created 19 October 2026; updated 3 November 2026 |
| Owner                  | Pluto                                                                                     |
| Document status        | Baseline execution plan; update as work is completed                                      |
| Requirements baseline  | [Product Scope and Requirements v0.3](./Pluto%20Takeaway%20Scope%20%26%20Requirements.md) |
| Current milestone | M3 — shop, menu and configurable-meal management; rules next |
| Implementation status | M1 completed 25 October 2026; bounded M2 backend completed 3 November 2026; 43 tests passing (developer/report evidence) |

## 1. Target and purpose

Build a Java backend application that supports **multiple food shops and collection-only orders**. A customer can browse shops, configure meals, save items from different shops, check out one shop at a time, and follow an order through collection. Administrators manage only their own shop's information, menu and orders.

The personal target is to rebuild an earlier project into a system Pluto understands and can explain: the data relationships, SQL, request flow, business rules, failure handling and trade-offs. The portfolio target is a reproducible graduate-level engineering project with working demonstrations and tests supporting truthful CV and interview claims.

Success means:

1. A complete customer-to-merchant collection journey works from a clean setup.
2. Shop and customer ownership boundaries are enforced by the backend.
3. Invalid meals, stale prices, unavailable items and closed shops cannot produce an invalid order.
4. Retries, concurrent checkout and conflicting order actions preserve consistent results.
5. Historical orders preserve what was purchased, even after menu changes.
6. Pluto can explain the core implementation and reproduce its verification.

There is no fixed completion date yet. Follow the dependency order and close milestones using evidence, rather than an arbitrary calendar or percentage estimate.

## 2. Confirmed boundaries and planning assumptions

### Confirmed business boundaries

- Collection only: no delivery, delivery address, courier or route features.
- Payment is simulated; no real payment integration or refunds.
- A basket can retain items from multiple shops, grouped by shop.
- Each checkout covers exactly one shop. A mixed-shop request is rejected in full, without order creation or basket mutation.
- A successful shop A checkout leaves shop B's basket unchanged.
- Administrators can manage only shops they are authorised for.
- Customers can cancel only before merchant acceptance.
- A retry of the same checkout must not create another order. A deliberate new purchase may create another order, even with identical products.
- Customers receive understandable information about price changes and withdrawn items.

### Recommended implementation plan

Use a layered monolith with Java, Spring Boot, Spring Data JPA/Hibernate and MySQL, matching the scope's proposed approach. Choose supported compatible versions during M1 and record them. Use database migrations, explicit API contracts and meaningful automated tests as each feature is built.

The tasks below are an actionable implementation proposal. They do not silently confirm the scope's open business decisions. Where a task depends on an open decision, resolve and record that choice at its stated checkpoint before implementing the dependent behaviour.

A small demonstration interface is planned in M7. A production storefront, microservices, inventory reservation, advanced promotions and forecasting are outside this release. Reporting, caching and push notifications are optional after the core release.

## 3. Starting point and source of truth

The workspace inspected on 19 October 2026 is:

```text
Pluto Takeaway/
├── Backend/                                  # Empty at inspection
└── Docs/
    ├── Pluto Takeaway Scope & Requirements.md
    └── Pluto Takeaway Milestones.md           # This plan
```

The related conversation is **“整理外賣項目”**, specifically its agreed first slice: start the application, read two shops from the database, and query each shop's menu without mixing their products. Earlier application code discussed there is not present in this workspace; descriptions of that code in the scope are historical context, not a fresh source-code review.

Use the scope for **what and why**, this plan for **sequence and completion**, and API/schema/decision documents for **implementation details**. If an agreed rule changes, update the scope first and then adjust affected tasks and tests here.

## 4. Tracking and working method

- Task checkboxes start unchecked. Check a task only after its output exists and its checks pass.
- Milestone status: `Not started`, `In progress`, `Blocked`, or `Done`.
- Record the actual completion date, changed files or commit, checks run and remaining issues in the evidence log in Section 15.
- A milestone is done only when its tasks, acceptance checks and documentation are complete. Record a scope reduction explicitly rather than checking an omitted task.
- Start with the next unchecked task in the current milestone. Read its steps, implement the smallest complete change, verify it, and review it before continuing.
- Pluto writes the core learning portions, especially modelling, SQL, service rules and transaction logic. Codex can explain, review, help with scaffolding and diagnose failures. Before closing a milestone, Pluto should be able to explain the core path without relying on generated text.
- During this collaboration, Codex has read the files in this workspace. Save changes in the same folder and ask for a fresh review so the latest saved contents can be inspected. This plan does not establish a continuous background sync or watcher.

| Milestone | Demonstrable outcome                                           | Depends on     | Status                                              | Completed |
| --------- | -------------------------------------------------------------- | -------------- | --------------------------------------------------- | --------- |
| M1 | Database-backed shop list and isolated shop menus | Existing scope | Done | 25 October 2026 |
| M2        | Customer/admin identity and enforced ownership boundaries      | M1             | Not started                                         | —         |
| M3        | Administrators maintain menus and configurable meals           | M2             | Not started                                         | —         |
| M4        | Persistent multi-shop baskets with valid configurations        | M3             | Not started                                         | —         |
| M5        | Correct single-shop checkout, snapshots and retry handling     | M4             | Not started                                         | —         |
| M6        | Merchant order processing and safe customer cancellation       | M5             | Not started                                         | —         |
| M7        | Small interface demonstrating the complete journey and changes | M6             | Not started                                         | —         |
| M8        | Reproducible release and defensible portfolio evidence         | M7             | Not started                                         | —         |

## 5. M1 — Foundation and public shop menus

**Outcome:** start the application, fetch at least two shops from MySQL, and fetch each shop's own menu. Public browsing is the proposed first slice; protected management is introduced in M2.

**Traceability:** OBJ-01, OBJ-05, OBJ-06; FR-01. This preserves the first milestone proposed in the previous chat.

### Tasks, in execution order

- [x] **M1.1 — Create the application foundation.** Check the installed JDK and database tooling; choose and record the JDK, Spring Boot and MySQL versions and one build tool. Create the Spring Boot project inside `Backend/` with web, validation, Spring Data JPA, MySQL, migration and test support. Include the build wrapper. Initialise a project-root Git repository if absent, add an appropriate `.gitignore`, and exclude build output, IDE-local files and secrets. Put database settings behind environment variables and supply a sample configuration with placeholders. Start the application and document the exact command in the root `README.md`.
- [x] **M1.2 — Draw the minimum data model.** In `Docs/Data Model/Data Model.md`, define `Shop`, `Category` and `Product`, including primary keys, ownership, name, availability/status and product price. Give categories and products explicit shop ownership. State how the database or validated write path prevents a product from referencing another shop's category. Choose exact decimal money storage and document its scale; do not use floating-point price arithmetic. Include a small relationship diagram and explain every foreign key.
- [x] **M1.3 — Make the database reproducible.** Add the first schema migration under `Backend/pluto-takeaway/src/main/resources/db/migration/`. Add a separate development/test seed mechanism containing shops A and B, categories and distinguishable products for both, including an unavailable product. Document creation of an empty local database, migration and seed commands. Run the setup on an empty database and inspect the resulting rows. Keep sample data out of an assumed production startup path.
- [x] **M1.4 — Specify the first two APIs.** In `Docs/API Contract.md`, define proposed routes `GET /api/shops` and `GET /api/shops/{shopId}/menu`. Include concrete JSON examples, ID and money representation, shop status, category/product nesting, ordering, empty-menu behaviour and unknown-shop errors. Decide whether unavailable products are omitted or shown as unavailable. Do not include hidden products accidentally. Treat route names here as proposals until recorded in the contract.
- [x] **M1.5 — Implement one complete read path.** Write shop-scoped Spring Data JPA repository queries; implement the service and controller; return response DTOs rather than database entities. Verify HTTP 400/404 status behaviour; a custom uniform error body is deferred to M2 authentication/error handling. The present API contract documents framework-default error bodies. Fetch actual database rows, rather than returning a hardcoded list. Read through the complete controller → service → repository → Hibernate/SQL → DTO path.
- [x] **M1.6 — Verify and record the first slice.** Add integration checks using a migration-created test database: two shops returned; A menu contains only A products; B menu contains only B products; an unknown shop returns the agreed error; a known empty shop has the documented response; unavailable items follow the contract. Save sample requests and results in `Docs/Demo Guide.md` and record the command and actual result in the evidence log.

### Completion gate

From an empty database, the documented setup produces the same two-shop demonstration. Both APIs work, isolation checks pass, and Pluto can explain the three-table relationship and where `shopId` is enforced. No management login or order creation is required to close M1.

## 6. M2 — Identity and ownership boundaries

**Outcome:** authentication identifies the actor, and authorisation restricts that actor to permitted resources.

**Traceability:** OBJ-02; FR-02; scope Section 4 and OPEN-05. Order-specific ownership checks are completed when orders exist in M5/M6.

### Tasks, in execution order

- [x] **M2.1 — Set the identity policy.** Resolve OPEN-05 in `Docs/Decisions.md`: customer login/registration scope, administrator provisioning, session/token mechanism, expiry and logout behaviour. Recommended minimum: seeded administrators and reusable customer accounts; no merchant self-service onboarding. Distinguish demonstration accounts from a public registration flow.
- [x] **M2.2 — Model accounts and shop membership.** Add migrations for accounts and the agreed administrator-to-shop mapping. Seed two customers and administrators for A and B. Hash passwords using a suitable password-hashing implementation; keep real credentials outside source control. Define unique login identifiers and inactive-account behaviour.
- [x] **M2.3 — Implement authentication.** Add the documented login endpoint and chosen session/token validation. Derive the current user and role on the server. Return useful but non-sensitive errors for invalid credentials, missing credentials and expired credentials. Document how a caller authenticates each request.
- [x] **M2.4 — Enforce shop-scoped access.** Implement a protected own-shop read and update for a small field such as the shop name. Verify membership and actual target ownership in the backend; changing a path ID or request body must not confer access. Define consistent unauthenticated and forbidden/not-found responses without leaking protected data. Apply the same rule to protected reads and writes.
- [x] **M2.5 — Prove the boundary.** Test unauthenticated management access; a customer calling management APIs; admin A reading/updating A; admin A reading/updating B; and a forged owner/shop field. Confirm B remains unchanged after rejected writes. Record the access matrix and test evidence in the API contract and evidence log.

### Completion gate

Valid actors can authenticate and perform permitted actions. Role checks and resource ownership checks both exist. All negative access cases pass, and no request-supplied identity is trusted as authorisation.

## 7. M3 — Shop, menu and configurable-meal management

**Outcome:** each administrator can maintain their own shop and menu, and a customer can inspect valid meal choices and prices.

**Traceability:** FR-01–FR-03; AC-01, AC-06; scope Section 7.2 and OPEN-01.

### Tasks, in execution order

- [ ] **M3.1 — Fix the meal rules before schema work.** Resolve OPEN-01: recommended first model is one required choice per group, optional supplements priced independently of standalone product prices, and no nested meals. Record a HK$58 meal with a HK$3 drink supplement as an example, including quantity calculation. Define money rounding, positive quantity bounds and behaviour when every option in a required group is unavailable.
- [ ] **M3.2 — Complete basic management.** Add shop details/open-status updates, category management and product create/update/withdraw operations. Validate required names, nonnegative prices, allowed status values and same-shop category ownership. Prefer withdrawal/archiving to deleting records later needed for order history. Apply M2 ownership checks to every operation, including list/detail reads.
- [ ] **M3.3 — Implement the meal model and validation.** Add migrations for meals, groups and options with clear shop ownership. Implement administrator maintenance and public menu responses. Validate group/option membership and reject foreign-shop references, duplicate choices and missing required choices. Centralise the selection and price calculation so baskets and checkout can reuse the same rules.
- [ ] **M3.4 — Decide and implement basic image handling.** Record whether this release includes uploaded images, as proposed in the scope, or deliberately uses seeded image references. If uploads are included, constrain size/type, generate safe storage names, configure storage externally, enforce administrator access and document local setup. Do not add cloud infrastructure solely for this task. If deferred, record the explicit scope adjustment and use valid demo references.
- [ ] **M3.5 — Verify maintenance and pricing.** Test CRUD/withdrawal and cross-shop denial for products, categories, meals, groups and options. Test the base meal, supplement price, quantity multiplication, missing/duplicate/foreign choices and an unavailable required group. Update the schema diagram, API examples and seed data with at least one configurable meal in each shop.

### Completion gate

A can update its own menu and cannot modify B's resources. Public reads reflect saved changes. Meal totals are deterministic, server-calculated and backed by validation tests. The agreed image scope is documented and demonstrable.

## 8. M4 — Persistent multi-shop baskets

**Outcome:** a customer maintains selections across shops and sees the groups independently, without compromising ownership or meal rules.

**Traceability:** FR-03, FR-04; scope Sections 7.1–7.3. Checkout is implemented in M5.

### Tasks, in execution order

- [ ] **M4.1 — Define basket identity and concurrency.** Model basket lines, customer ownership, shop grouping, selected options and a revision/version. Specify when adding an identical product/configuration merges quantities and when a different meal configuration creates a distinct line. Specify quantity bounds and the response to an outdated update. Record the basket versioning part of OPEN-07.
- [ ] **M4.2 — Add persistence and API contracts.** Create migrations and endpoints to read the grouped basket, add a selection, change quantity and remove a line. Derive the customer from authentication. Define errors for unknown items, invalid configurations, invalid quantities and another customer's line ID. Preserve other shop groups when editing one shop.
- [ ] **M4.3 — Implement authoritative validation.** Load actual product/meal ownership and current option definitions before accepting an addition. Calculate display totals on the server. Preserve enough observed price/version information to explain later price changes; a basket price is not a guaranteed checkout price. Return unavailable or changed selections with actionable reasons when reading existing baskets.
- [ ] **M4.4 — Prepare the checkout handoff.** Define a selected shop and the exact line IDs/revision that a checkout will consume. Return a shop-specific summary. Do not trust a client-supplied total or allow a shop ID to override actual ownership. Leave order creation to M5.
- [ ] **M4.5 — Verify basket behaviour.** Test A and B in one customer's basket; edits to A leave B intact; two configurations of the same meal stay distinct; identical configurations follow the merge rule; stale revisions are handled; and another customer cannot read or mutate the basket. Verify withdrawn items are explained rather than silently disappearing.

### Completion gate

The basket survives a reload/restart, groups shops correctly, enforces customer ownership and retains distinct meal selections. Its version and selected-item contract is precise enough to support safe checkout.

## 9. M5 — Reliable single-shop checkout

**Outcome:** one intended checkout creates one complete, correctly priced order, or fails without consuming the basket. This milestone is the main transaction and concurrency exercise.

**Traceability:** FR-05–FR-08, FR-11, FR-12; AC-02–AC-10, AC-13, AC-14; OPEN-02 and OPEN-07.

### Tasks, in execution order

- [ ] **M5.1 — Close the checkout policy decisions.** Resolve OPEN-02 and the remaining OPEN-07 details in `Docs/Decisions.md`: simulated payment timing/outcomes, price confirmation contract, basket consumption, product/menu version checks and transaction strategy. Define the currency and rounding policy from M3 consistently. Include a sequence diagram showing preview, confirmation and committed order creation. Clearly label simulated payment; define failure as no order/basket consumption if using pre-order simulation, or explicitly model the agreed alternative.
- [ ] **M5.2 — Add order and request-integrity persistence.** Create migrations for orders, lines, selected-option snapshots and idempotency records/constraints. Store owner, shop, collection fulfilment, status, currency, purchased names/prices/options and timestamps. Define key scope, payload fingerprint and retention/expiry semantics; prevent a key from exposing another customer's result. Protect against consumption of the same basket selection under different keys.
- [ ] **M5.3 — Implement checkout preview and rejection reasons.** Re-read current shop status, products, options and prices on the server. Validate exactly one actual shop across all submitted items, customer ownership, quantities and selections. Return revised prices/unavailability with enough detail for the customer to fix or confirm the basket. A preview must not silently authorise a later purchase after conditions change.
- [ ] **M5.4 — Implement the atomic commit.** Revalidate or conditionally enforce the confirmed revisions inside the transaction. Create the order, line/option snapshots, idempotency outcome and selected-basket consumption consistently. Use database constraints plus the chosen locking/version strategy to close the gap between validation and write. Preserve B's basket during A checkout and preserve items added concurrently that were not part of the purchased selection. On any failure, roll back all related writes.
- [ ] **M5.5 — Implement retry and deliberate-repeat semantics.** Equivalent retries with the same key return the original outcome; changed content with the same key is rejected. Concurrent submissions of the same attempt create at most one order. Different keys cannot consume the same basket twice. After a new selection/purchase intent is created, identical products can be ordered again. Define in-progress/conflict responses and how the client recovers after a timeout.
- [ ] **M5.6 — Add protected order reads.** Implement customer order history/detail and shop-scoped merchant order reads. Read purchased values from snapshots, not live menu prices. Use the authenticated identity to prevent customer A reading B's order and administrator A reading shop B's order.
- [ ] **M5.7 — Prove failure and race behaviour.** Run database-backed tests for mixed-shop rejection with zero mutation; A checkout preserving B; a forged price; changed price after preview; withdrawal/closure before commit; invalid meal options; equivalent sequential and simultaneous retries; changed payload with the same key; different-key double consumption; and a deliberate repeat purchase. Inject a failure during line insertion and verify no partial order or basket loss. Change the menu after purchase and verify historical snapshots. Use coordinated transactions for race cases and inspect persisted results, not just HTTP statuses.

### Completion gate

The relevant acceptance criteria pass with saved evidence, including actual concurrency and rollback checks. The server never silently buys at a newly unconfirmed price. Pluto can explain the transaction boundary, idempotency key versus purchase intent, and how the implementation prevents duplicate basket consumption.

## 10. M6 — Order lifecycle and collection

**Outcome:** the merchant can accept/reject and fulfil orders; customer cancellation competes safely with acceptance.

**Traceability:** FR-09–FR-11; AC-01, AC-02, AC-11, AC-12; OPEN-06.

### Tasks, in execution order

- [ ] **M6.1 — Confirm the state transition table.** Resolve OPEN-06. Recommended minimum: `PLACED → ACCEPTED → READY_FOR_COLLECTION → COMPLETED`, with `PLACED → REJECTED` or `PLACED → CANCELLED`. Record who can perform each action, repeated-action behaviour, conflict errors and terminal states. Decide who confirms collection. Avoid a free-form endpoint that accepts any status supplied by a client.
- [ ] **M6.2 — Implement permitted commands.** Add merchant accept, reject, ready and collected commands plus customer cancellation. Verify actor ownership and expected source state in the same protected operation. Return a clear conflict when the state is no longer eligible. Keep existing accepted orders fulfilable when a shop closes, if adopting the scope's suggested rule.
- [ ] **M6.3 — Make state changes safe under concurrency.** Use a conditional update/version or another documented database strategy. A cancellation and acceptance race must have one winner. Define repeated commands without repeating effects. Record the resulting state and useful transition timestamps; avoid logging secrets or treating simulated payment as settled money.
- [ ] **M6.4 — Prove the lifecycle.** Test every allowed transition and representative forbidden transitions, cancellation after acceptance, terminal-state reopening, wrong customer/shop, repeated commands and simultaneous cancellation/acceptance. Assert exactly one winner and a valid final persisted state. Demonstrate both the happy collection path and rejection/cancellation paths.
- [ ] **M6.5 — Update the demo and contracts.** Document request examples, actor credentials setup, state diagram, errors and the saved race-test result. Explain why a menu withdrawal does not rewrite an already placed order.

### Completion gate

A full API-based collection journey works. Invalid actors and transitions are denied. Acceptance and cancellation cannot both report success for the same pending order.

## 11. M7 — Demonstration interface and customer change handling

**Outcome:** a reviewer can follow the customer and merchant journeys without manually assembling every HTTP request.

**Traceability:** FR-01–FR-12; AC-07, AC-08, AC-10, AC-15; OPEN-03 and OPEN-04.

### Tasks, in execution order

- [ ] **M7.1 — Choose a bounded interface and freshness policy.** Select a simple UI approach and document setup; create `Frontend/` only if a separate client is used. Resolve OPEN-03: when the repeat-purchase warning appears and how confirmation creates a new intent. Resolve OPEN-04: refresh on interaction, polling, or push; define a measurable freshness target if claiming near-real-time behaviour. Keep backend checkout validation independent of this mechanism.
- [ ] **M7.2 — Build the customer path.** Add shop list, shop menu, meal choice controls, grouped basket, one-shop checkout, clearly simulated payment, order history/detail and eligible cancellation. Show validation errors near the affected selection. Keep other shop groups after checkout. Preserve the same checkout key across retries/timeouts; generate a new key only for a new purchase intent.
- [ ] **M7.3 — Build the merchant path.** Add login, own-shop details/open-status changes, product/meal maintenance and the order action list. Handle forbidden responses correctly even when the UI normally hides the action. Avoid adding a separate platform administrator product.
- [ ] **M7.4 — Expose changing conditions clearly.** Show old/current prices and require revised-total confirmation. Mark withdrawn products/options and block invalid purchase until corrected. When an update connection or polling request fails, show a useful state and refresh authoritative data on recovery. A late change returned by checkout must be visible and actionable. Implement the agreed same-shop repeat warning without banning legitimate repeat orders.
- [ ] **M7.5 — Run the two-session demonstration.** Use separate customer and merchant sessions. While the customer has a basket open, change a price, withdraw an option and close the shop; verify the agreed UI behaviour and backend rejection. Restore valid conditions, complete collection, then intentionally repeat a purchase. Verify another shop's basket remains. Record any measured update latency and save reproducible steps/screenshots in the demo guide.

### Completion gate

AC-15 is demonstrable through the interface, supported by the backend evidence from M5/M6. Customers understand changed conditions and can recover. Any freshness claim matches the agreed target and actual observed results.

## 12. M8 — Release readiness and portfolio evidence

**Outcome:** a future reader can run, assess and discuss the project using the repository alone.

**Traceability:** OBJ-01–OBJ-06; scope Section 9 and all AC-01–AC-15.

### Tasks, in execution order

- [ ] **M8.1 — Verify a clean setup.** Follow only the README in a clean directory and empty database. Verify prerequisites, environment placeholders, wrapper/build command, migrations, seed data, backend/client startup and test commands. Include two shops, two administrators and two customers. Fix undocumented local assumptions; do not depend on a personal absolute path or an existing database.
- [ ] **M8.2 — Complete release verification.** Run the relevant automated suite and the documented complete journey on the final revision. Check ownership, invalid selections, price/availability changes, rollback, retry/double-consumption and state races. Inspect errors/logs for raw SQL internals, passwords and tokens. Record failures and fixes, then rerun affected checks. Do not mark acceptance rows passed without an actual result.
- [ ] **M8.3 — Complete the engineering documents.** Reconcile the final schema, APIs and decisions with code. Include request/response/error examples and authentication rules; explain transaction/concurrency choices and known limitations. Update the scope where implementation decisions were confirmed. Separate deferred features from defects in the delivered core.
- [ ] **M8.4 — Prepare a short reviewer demonstration.** Write a sequence covering shop browsing, configurable meal pricing, multi-shop basket, mixed-shop rejection, stale-price recovery, successful checkout/retry, acceptance/collection and cross-shop denial. Link each claim to a runnable test, API example or screenshot. Keep the presentation focused on verified behaviour.
- [ ] **M8.5 — Prepare accurate CV and interview notes.** Draft 2–3 CV bullets from delivered work. For each, record the implementation and evidence that support it. Prepare explanations of shop isolation, exact price arithmetic, transaction rollback, snapshots, idempotency and cancellation races. Include alternatives considered and limitations. Do not claim production readiness, real payments or measured performance improvements without evidence.
- [ ] **M8.6 — Close the baseline release.** Ensure all required tasks and acceptance rows are complete or explicitly removed through an agreed scope change. Record the final revision/release identifier, completion date, remaining limitations and demo instructions. Leave optional enhancements in a separate backlog.

### Completion gate

A reviewer can reproduce setup and demonstration, the core acceptance matrix has evidence, and Pluto can explain the main design decisions. The portfolio describes implemented capabilities accurately.

## 13. Decision checkpoints

Record decisions in `Docs/Decisions.md` with ID, date, chosen rule, reason, alternatives, consequences and affected tests. These are decision tasks within the milestones, not unresolved questions that prevent writing this plan.

| Scope ID | Decision                                                        | Must be resolved before        | Task          |
| -------- | --------------------------------------------------------------- | ------------------------------ | ------------- |
| OPEN-01  | Required meal groups, independent supplements and pricing rules | Meal schema and validation     | M3.1          |
| OPEN-02  | Simulated payment timing and failure behaviour                  | Order creation                 | M5.1          |
| OPEN-03  | Repeat-purchase warning trigger                                 | Checkout interface             | M7.1          |
| OPEN-04  | Freshness mechanism and target                                  | Live change presentation       | M7.1          |
| OPEN-05  | Identity scope and administrator provisioning                   | Account implementation         | M2.1          |
| OPEN-06  | Exact order states and collection confirmation                  | Order commands                 | M6.1          |
| OPEN-07  | Price confirmation, basket versions and concurrency             | Basket model / checkout commit | M4.1 and M5.1 |

Technical choices such as framework versions and error shape can be recorded during their relevant task. If a choice changes customer-visible rules, update the scope as well.

## 14. Acceptance coverage and optional backlog

### Core acceptance matrix

All rows are **Not run** at document creation. “Coverage” identifies planned verification, not a passed result.

| Scope acceptance ID | Behaviour                                             | Planned coverage                     | Actual result / evidence |
| ------------------- | ----------------------------------------------------- | ------------------------------------ | ------------------------ |
| AC-01               | Cross-shop protected reads/writes denied              | M2.5, M3.5, M6.4                     | Not run                  |
| AC-02               | Another customer's order denied                       | M5.6, M6.4                           | Not run                  |
| AC-03               | A checkout preserves B basket                         | M5.7, M7.5                           | Not run                  |
| AC-04               | Mixed-shop request rejected without mutation          | M5.7                                 | Not run                  |
| AC-05               | Separate valid purchases from different shops allowed | M5.7: run separate A and B checkouts | Not run                  |
| AC-06               | Invalid meal selection rejected                       | M3.5, M5.7                           | Not run                  |
| AC-07               | Unconfirmed price change prevents purchase            | M5.7, M7.5                           | Not run                  |
| AC-08               | Withdrawn item or closed shop prevents new order      | M5.7, M7.5                           | Not run                  |
| AC-09               | Retry/concurrent checkout creates at most one order   | M5.7                                 | Not run                  |
| AC-10               | Deliberate repeat purchase permitted                  | M5.7, M7.5                           | Not run                  |
| AC-11               | Acceptance/cancellation race has one winner           | M6.4                                 | Not run                  |
| AC-12               | Cancellation after acceptance denied                  | M6.4                                 | Not run                  |
| AC-13               | Failed order-line write rolls back everything         | M5.7                                 | Not run                  |
| AC-14               | Historical snapshot survives menu edits               | M5.7                                 | Not run                  |
| AC-15               | Complete collection journey demonstrated              | M7.5, M8.4                           | Not run                  |

### Optional milestones after M8

These do not count as unfinished core-release work. Promote an item only after recording its purpose, acceptance checks and effect on scope.

| Optional milestone              | Concrete tasks                                                                                                                                                                                                                                                                  | Completion evidence                                                                                               |
| ------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------- |
| O1 — Basic sales reporting      | Define business timezone and half-open date ranges; aggregate completed orders by shop; separate item quantity, distinct order count and simulated order value; use purchased snapshots; avoid counting meal/component value twice; expose a protected report and a simple view | Known seeded totals match the report; range boundaries, cancelled/rejected exclusion and cross-shop denial tested |
| O2 — Faster change notification | If the baseline freshness policy is insufficient, choose polling/SSE using a specific target; implement shop-scoped updates, reconnect refresh and stale-view recovery                                                                                                          | Measured latency meets the target; disconnect recovery works; checkout tests still pass                           |
| O3 — Menu caching               | Measure uncached reads first; define shop-specific keys, invalidation after writes and cache-failure behaviour; add caching only for a measured need                                                                                                                            | Before/after measurements plus isolation, invalidation and fallback tests; no unsupported performance claim       |

Delivery, real payment, stock reservation, cross-shop checkout, nested meals and forecasting remain excluded. An optional reporting view must label values as simulated order value, not actual settled revenue.

## 15. Evidence log and immediate next action

### Initial inspection record

| Date            | Work                               | Evidence / outcome                                                                          |
| --------------- | ---------------------------------- | ------------------------------------------------------------------------------------------- |
| 19 October 2026 | Retrieved the related conversation | “整理外賣項目”; first milestone and collection-only correction recovered                          |
| 19 October 2026 | Inspected the current workspace    | Scope v0.3 present under `Docs/`; `Backend/` empty; no application implementation inspected |
| 19 October 2026 | Created the execution baseline     | This document; all implementation tasks and acceptance results remain incomplete            |

### Completion record template

Copy a row for each completed task or milestone. Record exact commands/results rather than “tested” alone. Do not put credentials or tokens into evidence.

| Task / milestone | Completed date | Files / revision | Check command or demo steps | Actual result | Remaining issue / decision |
| ---------------- | -------------- | ---------------- | --------------------------- | ------------- | -------------------------- |
| —                | —              | —                | —                           | —             | —                          |

### What to do next

1. Review and commit the M2 completion documentation separately from application code.
2. Start M3.1: agree meal groups/options, required selections, supplements, quantity and money rules using worked examples.
3. Write the M3 model/API contract before migrations; then implement one bounded management slice at a time.

M1 completion evidence, 25 October 2026:

- Public shop list/detail/menu endpoints implemented using JPA, Hibernate and DTOs.
- Flyway V1 creates the schema; dev V2 and test V2 are selected independently by profile.
- Agent independently ran the eight-test suite on 24 October: 8 tests, 0 failures, 0 errors. The later strengthened assertions were also rerun successfully before the Git exercise.
- Developer reports that a fresh clone from GitHub passes the full `mvn test` suite: 8 tests, 0 failures, 0 errors. This validates committed sources against a fresh Testcontainers database, not a completely uncached machine.
- Git commits `c47855d` and `4df9f85` record implementation and tests; remote tracking was observed in the local repository. No new Git mutations were performed for this documentation update.
- Browser startup and Flyway restart were previously verified by the developer. A complete independent replay of every manual README instruction is not claimed.

Scope refinement: M1 accepts the documented Spring Boot default error representation and verifies HTTP 400/404. A stable custom error DTO is deferred to M2 so authentication, authorisation and application errors can be designed together. Status behaviour remains required and tested. Query-count logs were observed but are not enforced by an automated performance assertion.


M2 completion evidence, 3 November 2026:

- Account schema V3 with dev/test V4 fixtures; BCrypt password validation, enabled-account handling and Session authentication implemented.
- Login/current-user/logout, CSRF endpoint, admin own-shop GET/PATCH and uniform documented error codes implemented. PATCH updates name only; unknown assignment fields are ignored and cannot change status/ownership.
- Agent executed the 40-test suite on 2 November with no failures/errors. Developer subsequently reports 43 tests passing; reviewed local Surefire report corroborates 43 tests, 0 failures/errors. Final additions cover unknown fields, Session ID rotation and disabled login.
- M2.1–M2.5 core completion gate met. Session idle timeout and HttpOnly/SameSite are configured; actual timeout, old-cookie replay, fresh M2 setup and HTTPS deployment cookie behaviour are not claimed tested. See M2 contract and Demo Guide for exact limits.
- Completed implementation commits include `e217f43` role/ownership, `0a48fd0` shop-name updates, `62d65e4` error responses and `2b6ee23` Session lifecycle. Documentation work performs no Git staging/commit/push.
- M3 menu management, configurable meals, baskets and orders remain incomplete. Cross-shop management evidence applies to current shop endpoints only, not future resources.
