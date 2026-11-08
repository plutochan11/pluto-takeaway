# Architecture and Implementation Decisions

## ADR-001 — Session authentication for M2

Date: 25 October 2026; implementation/evidence updated 3 November 2026.

Status: accepted and implemented for the bounded M2 backend. Core behaviour is covered by database-backed MockMvc tests; deployment cookie/timeout limits are recorded below.

### Context and decision

The current application is one Spring Boot backend with a planned browser demonstration interface. The author has previous JWT experience but wants M2 to focus on authentication, authorisation and shop ownership. Use Spring Security with server-side HTTP sessions. JWT is not part of the M2 implementation.

Initial account provisioning uses development/test fixtures: two customers and two administrators for different shops. Public registration and merchant onboarding are deferred. Passwords are stored as BCrypt hashes; real passwords are never committed. Fixture credentials will be deliberately public, development/test-only values and will not be reused as deployment secrets.

### Implemented bounded account model

One `user_account` table stores ID, username, password hash, role, enabled flag and optional shop ID. Each account has exactly one role initially. A `CUSTOMER` has no shop ID; a `SHOP_ADMIN` belongs to exactly one shop. A shop can have several administrators. A foreign key enforces shop existence; a role/shop CHECK enforces the allowed combinations. Multiple-shop membership and combined customer/admin roles require a future model change.

Database account records are persistent business data. HTTP sessions are separate runtime authentication state; M2 does not add a database session table or Redis.

### Authentication lifecycle

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

The 43-test suite covers real login and session reuse, CSRF behaviour, logout invalidation, Session ID rotation, disabled login, customer/admin role boundaries, cross-shop read/write denial, ignored assignment fields and anonymous browsing. Tests use framework authentication with fixture accounts, not mocked authenticated users.

Evidence: developer reports 43 passing tests; local Surefire report corroborated on 3 November. Agent independently executed the earlier 40-test suite on 2 November. No real-container idle-timeout or old-cookie replay check is claimed. HttpOnly/SameSite and the 30-minute timeout are explicit configuration; Secure cookie verification requires a future HTTPS deployment.

PATCH accepts only `name` through a dedicated DTO and explicit entity rename. Unknown JSON properties are deliberately ignored; tests show extra `id`, `shopId` and `status` fields cannot redirect the update or change status/account ownership. Names are required, nonblank and at most 100 characters; no trimming policy is implied.

Security errors are written by configured handlers; MVC validation/binding and selected `ResponseStatusException` statuses use advice. Both use `{code,message}`. Unmapped exception statuses retain framework resolution; this is not a universal catch-all error policy.

## ADR-002 — Bounded meal selections and independent supplements

Date: 3 November 2026. Status: accepted by Pluto; policy only, schema/API implementation pending.

### Context and decision

M3 introduces configurable meal deals. Agree business behaviour before selecting tables or controller DTOs. A meal belongs to one shop and has a base price and required option groups. Each group requires exactly one selected option. A fixed component is a group with one option. An explicit No Drink option with zero supplement may represent declining a drink; omission is not a valid selection. No nested meals or multi-select groups in this baseline.

Each selected option must belong to the requested meal and group, and all referenced resources must belong to the same shop. Reject missing groups, duplicate selections, foreign-shop or unrelated options, and unavailable selections. If any required group has no available options, the meal cannot be purchased. Server data determines membership, availability and prices; client IDs identify choices but do not prove permission or price.

### Pricing and quantities

- Currency: HKD. Base prices and supplements are nonnegative decimal monetary values with at most two fractional digits. Reject higher precision instead of silently rounding.
- Supplements are independent of standalone product prices; changing a standalone price does not automatically change a meal supplement.
- Unit price = meal base price + sum of selected supplements. Line total = unit price × quantity. Use BigDecimal, not binary floating-point arithmetic.
- Quantity is an integer from 1 through 99 per line. Quantity counts identical complete configurations; different configurations occupy distinct basket lines. Identical-configuration merging and aggregate-limit behaviour will be defined in M4.
- Addition and integer multiplication of these values need no rounding. Discounts, tax calculations and division-based pricing are outside this model and require a later decision if introduced.
- The initial two-decimal rule does not set a database monetary maximum; column precision and overflow validation remain part of schema/API design.

### Worked example (hypothetical Shop 1 data)

Lunch meal base price: HK$58. Main group: Chicken Rice +0 or Beef Rice +5. Drink group: Hot Tea +0, Iced Tea +2, Coffee +3. These are proposed same-shop options, not references to the existing Shop 2 coffee fixture.

- Chicken Rice + Coffee: unit price 58 + 0 + 3 = HK$61; quantity 1 line total HK$61.
- Beef Rice + Iced Tea: unit price 58 + 5 + 2 = HK$65; quantity 2 line total HK$130.
- One coffee configuration and one tea configuration require separate lines; neither configuration may omit a required group.

### Consequences and next work

This bounded model supports explicit validation and deterministic server-side pricing. It is not an implementation claim: tables, constraints and APIs still need design; ADR-003 subsequently resolves product linkage and availability propagation. Basket and checkout must later reuse the agreed rules; checkout must revalidate effective data and save purchased snapshots rather than trusting a previously displayed total.

## ADR-003 — Product-backed meal options and explicit non-product choices

Date: 3 November 2026. Status: accepted by Pluto; logical design only, schema/API implementation pending. Complements ADR-002.

### Context

One product may be offered by several meal groups, with different supplements. Duplicating its name and availability in each option risks inconsistent updates. A choice such as No Drink does not represent a standalone product, so requiring a product for every option would require artificial product records.

### Decision

- Each option belongs to one group; each group belongs to one meal. A product may be referenced by multiple options.
- A product-backed option references a same-shop product. Display its product name, but store its meal supplement independently of the standalone price.
- An option has its own availability. A product-backed option is selectable only if both the option and its product are available, subject also to the meal/group rules in ADR-002. Product unavailability therefore prevents selection wherever it is referenced; option unavailability can be specific to one meal group.
- Permit an option without a product reference for an explicit choice such as No Drink. It requires its own nonblank display label and a zero supplement; it still counts as the group's one selection.
- For the first model, product-backed options use the product name rather than an independently editable label. Exact columns, nullability constraints and cross-shop database enforcement remain design work.

### Consequences and follow-up

The product reference is optional, introducing two cases that schema constraints and application validation must distinguish. Independent supplements do not mean independent product availability. Product names are live menu data; later orders must snapshot purchased display values. Category placement was subsequently resolved in ADR-004; complete management APIs remain to be designed.

The logical model is maintained by the assistant from discussions with Pluto; Pluto decides the business/design choices. The current model is [Meal Model Draft](./Data%20Model/Meal%20Model%20Draft.md). Accepted decisions belong here; field details and examples belong in the model, without copying the entire ADR into it. A later change should record a superseding decision rather than silently replacing the rationale.


## ADR-004 — Reuse shop categories for meals

Date: 4 November 2026. Status: accepted by Pluto; schema/API implementation pending.

### Decision and rationale

Reuse the existing Category entity. Every meal belongs to exactly one category owned by the same shop. A category may contain products, meals, both or neither; there is no separate MealCategory table or category-type restriction. Shops may create a Lunch Meals category, but that name is not reserved or required.

The category is a display grouping, not a restriction on which products the meal can reference. A meal may reference products in other categories of the same shop. Cross-shop references remain invalid. Product and meal records remain distinct entities with separate identities.

### Consequences

Add category_id to Meal and enforce (shop_id, category_id) against the existing Category (shop_id, id) key, following the product ownership pattern. Existing public menu response compatibility and ordering between products/meals remain API design work. No category or product migrations already applied are edited to implement this policy.

## ADR-005 — Atomic complete-meal creation and monetary input limits

Date: 5 November 2026. Status: atomic-creation workflow superseded by ADR-006 before implementation. Monetary input limits remain accepted. The text below preserves the original rationale and proposal.

Pluto prefers one request containing the complete meal, its groups and options. Adopt this approach: existing schema already represents the hierarchy; nested validation and atomic persistence add manageable complexity. Split development into small tasks without requiring API clients to perform separate parent/child creation requests. Subsequent editing/publication APIs remain separate design work.

Use one service transaction. Validate actor ownership, category ownership, all referenced products and nested input before writes where practical. Persist meal, groups and options with server-generated IDs, deriving child ownership and parent references on the server. If any write fails, roll back the entire creation; generated IDs may still have gaps after rollback.

Base price and each option supplement are nonnegative HKD amounts with at most two fractional digits and an API maximum of 9,999.99 inclusive. Non-product options still require zero supplement. Storage remains DECIMAL(10,2); no migration is required for the smaller input cap. Limits on calculated unit/line totals and request collection sizes remain follow-up design items, not implied by this per-amount cap.

The creation slice retains explicit UNAVAILABLE initial state for meal and options, consistent with the schema's safe defaults. Complete structure does not automatically mean published or purchasable. Activation/public menu behaviour is not implemented by creation. Referenced unavailable products may be configured, but cannot later pass purchase eligibility. The detailed API design is maintained in [M3 API Contract](./M3%20API%20Contract.md).


## ADR-006 — Incremental meal management with frontend orchestration

Date: 5 November 2026. Status: superseded before implementation by ADR-007. This entry preserves the incremental workflow considered during design; ADR-005's monetary cap remains accepted.

### Context and decision

A single UI action does not require a single backend endpoint. Pluto chooses separate creation operations for meal, group and option. The frontend can collect a complete form and orchestrate these requests behind one Create action, while the backend offers smaller operations usable during later editing.

Create the unavailable meal first and retain its returned ID; create each group under that meal and retain each group ID; create options under the correct group. These dependencies require ordering; do not fire all requests concurrently before parent IDs exist. Same-level parallelism is optional later and is not required for this learning implementation.

### Partial failure and safety

Each request has its own transaction. Successful earlier requests remain committed if a later request fails; frontend orchestration is not a database transaction across HTTP calls. Retain the created IDs and display the incomplete state/error so the admin can continue configuring that meal rather than blindly recreating it. A network timeout may have an unknown commit outcome; automatic POST retries and idempotency are not promised by this baseline and require read/reconciliation support before a complete frontend wizard.

The meal stays UNAVAILABLE during setup. Publication must be explicit and validate the complete effective configuration; no completion inference from frontend success alone. The first create-meal endpoint may legitimately persist a meal with no groups/options. Initial option availability and activation/editing details must be specified in their own slices. No automatic destructive compensation of already-created records is added.

### Consequences

More HTTP calls and intermediate records, but simpler bounded DTOs/services and reusable management operations. Server-side role, target-shop ownership and parent-child membership are checked for every operation independently. A failed option creation must not corrupt its own request's data; it does not roll back the meal/group creation requests. Current schema requires no change for this workflow.


## ADR-007 — Final complete-meal creation boundary

Date: 5 November 2026. Status: confirmed by Pluto; implementation pending. Supersedes ADR-006. Reaffirms ADR-005's atomic creation and monetary policy.

Create a complete Meal with its required groups and options through one POST request and one service transaction. This matches the desired business operation: either all new records are committed or none are. The modest nested validation/persistence complexity is preferable to coordinating partial creation across HTTP requests for this project. The atomic boundary does not require one oversized method: DTO validation, access checks, product lookup and entity creation may be factored into focused helpers.

Initial meal and option statuses remain server-controlled UNAVAILABLE in this creation slice. Explicit activation and later editing are separate operations; creation does not publish the meal. Preserve the accepted HKD 9,999.99 maximum for each base-price/supplement input, nonnegative two-decimal values and zero supplement for non-product choices.

Frontend still handles validation failures and network errors. A committed response lost in transit is not resolved by database atomicity; retry/idempotency or reconciliation is follow-up work. No separate group/option creation endpoints are required for the first complete-create slice. Detailed current contract: [M3 API Contract](./M3%20API%20Contract.md).

## ADR-008 — Publish a meal and its option selection together

Date: 7 November 2026. Status: workflow confirmed by Pluto, implementation pending.

The admin selects the enabled options and publishes the meal in one action. The backend validates membership and requires a selectable choice in every required group. On 8 November 2026, Pluto chose to reject publication if any selected option references an unavailable product, even when another selected option covers the group. One transaction applies the complete selection and sets the meal to AVAILABLE. Failure preserves the prior state. Later product changes affect effective availability without rewriting stored meal or option status. See the [publication contract](./M3%20API%20Contract.md#publish-a-meal) for the HTTP design.
