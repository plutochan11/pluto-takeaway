# Pluto Takeaway
## Product Scope and Requirements

| Field | Value |
|---|---|
| Document version | 0.4 |
| Last updated | 25 October 2026 |
| Document owner | Pluto |
| Status | Working specification; confirmed decisions and proposals distinguished below |
| Intended audience | Project author, future maintainers and technical reviewers |
| Implementation status | M1 public browsing and bounded M2 identity/ownership backend complete; 43 tests pass (developer/local report evidence, 3 November 2026); M3 rules next |

## 1. Purpose and background

Pluto Takeaway is a personal Java backend project for ordering food from multiple shops for **collection only**. A customer can browse shops, select individual products or configurable meal deals, maintain shopping baskets across shops and place an order with one shop at a time. Shop administrators manage their own shop's information, menu and orders.

The project rebuilds an earlier takeaway application. The earlier source contains useful menu, basket, authentication, caching and transactional order examples, but its database schema is not reproducible from the uploaded source, some features are incomplete, and the author no longer has sufficient familiarity with its design. Rebuilding a bounded system provides an opportunity to make the business rules explicit and understand the implementation from the outset.

This document records what the project is intended to achieve, the problems it addresses and the reasons for its boundaries. It is a proportionate requirements document for an individual graduate-level project, not a claim of compliance with a particular formal standard. Detailed schema, API contracts and architecture decisions will be developed separately as the relevant features are implemented.

Food-ordering platforms provide product inspiration only. No claim is made about their current features or internal architecture.

## 2. Objectives and evidence of success

| ID | Objective | Evidence expected |
|---|---|---|
| OBJ-01 | Deliver an understandable customer ordering journey | Demonstration from shop discovery to order collection, including failure cases |
| OBJ-02 | Enforce ownership and shop boundaries | Tests showing that administrators cannot access another shop's protected resources and customers cannot access another customer's orders |
| OBJ-03 | Handle changing product conditions correctly | Demonstrations of price changes, withdrawn products and invalid meal selections at checkout |
| OBJ-04 | Create orders reliably | Tests for retries, concurrent checkout, rollback and order-history preservation |
| OBJ-05 | Improve the author's engineering and coding judgement | The author can explain core rules, implementation choices, trade-offs and test results |
| OBJ-06 | Produce credible portfolio evidence | Reproducible setup, schema migrations, API documentation and implemented features that support accurate CV statements |

Feature count and framework count are not success criteria. Performance improvements must not be claimed without measurements. Planned features must not be presented as completed work.

## 3. Requirement status and terminology

- **Confirmed:** explicitly agreed in project discussions. This records a decision, not implementation completion.
- **Proposed:** a recommended default awaiting a decision.
- **Deferred:** a possible later enhancement, outside the proposed initial release.
- **Excluded:** outside the current project scope.

“Must” expresses required behaviour for the stated feature. Where a row is proposed, its requirements become binding only when that proposal is accepted. Acceptance criteria describe future verification; none is reported as passed in this document.

| Term | Meaning |
|---|---|
| Shop | The merchant boundary for a menu, orders and administrative access |
| Shop administrator | An authenticated account authorised to manage a particular shop |
| Product | An individually purchasable menu item |
| Meal deal | A purchasable bundle with fixed contents or customer-selectable options |
| Basket | The customer's saved selections, presented in shop-specific groups |
| Checkout attempt | One intention to purchase a particular shop's selected basket contents |
| Retry | Re-submission of the same checkout attempt, for example after a network timeout |
| Repeat purchase | A deliberate new purchase, even if the products match an earlier order |
| Order snapshot | The names, selected options, quantities and prices recorded when an order is created |

## 4. Users and access boundaries

| Actor | Intended access |
|---|---|
| Visitor | Browse public shop and menu information; public browsing is a proposed default |
| Customer | Manage their own baskets, place orders and view or cancel eligible orders belonging to them |
| Shop administrator | Manage information, products, meal deals and orders for authorised shops only |

Authentication establishes identity. Authorisation determines which actions that identity may perform on a particular resource. An administrator role alone is insufficient: the requested resource must also belong to an authorised shop.

The server must derive identity from the authenticated session or token, validate shop membership and check resource ownership. It must not trust an administrator ID, customer ID or shop ID supplied by the client as proof of permission. These checks apply to read operations, write operations and any future sales reports.

Proposed simplification: each administrator belongs to one shop initially; a shop may have several administrators with the same permissions. Shop and administrator accounts can be provisioned through seed data. Self-service merchant onboarding, staff invitations and a platform administrator interface are excluded from the initial release.

## 5. Scope baseline

| Area | Scope and status |
|---|---|
| Shop discovery | Confirmed: customers can view multiple shops and their products. Proposed: simple name search and pagination |
| Shop management | Confirmed: authorised administrators manage their own shop's name, images and relevant details |
| Menu management | Confirmed: manage products and meal deals, including price and availability. Proposed: retain categories and basic image upload from the previous application |
| Configurable meal deals | Confirmed feature direction; selection and pricing rules remain proposed in Section 7 |
| Baskets | Confirmed: retain products from multiple shops and display them grouped by shop |
| Checkout | Confirmed: each checkout covers exactly one shop; reject requests containing items from multiple shops |
| Fulfilment | Confirmed: collection only. No delivery or delivery-address collection |
| Payment | Confirmed: simulation only; no real payment integration. Simulation timing and failure behaviour remain open |
| Orders | Confirmed: administrators accept or reject orders and update subsequent states; customers can cancel only before acceptance |
| Product changes | Confirmed: inform customers of price changes and withdrawn products. Update latency and transport remain open |
| Sales analysis | Deferred proposal: date-range sales counts and order-value summaries |
| Demonstration interface | Proposed: a small functional web interface, sufficient to demonstrate the customer and administrator journeys |

### Explicit exclusions

- Delivery, delivery addresses, couriers, route planning and location tracking.
- Checkout across multiple shops, automatic order splitting and combined settlement.
- Real card collection, payment gateways, financial settlement and real refunds.
- Numerical stock reservation or guarantees against overselling the last physical item.
- Scheduled orders, vouchers, loyalty programmes, reviews and recommendation systems.
- Nested meal deals, complex promotions and arbitrary bundle configuration.
- Sales forecasting, automated business advice and a separate analytics platform.
- Microservices or infrastructure added solely to increase the technology list.

## 6. Core functional requirements

| ID | Requirement | Status |
|---|---|---|
| FR-01 | Customers can browse shops and inspect their menus | Confirmed |
| FR-02 | Administrators can modify their own shop's information and menu; cross-shop access is denied | Confirmed |
| FR-03 | Customers can select valid meal-deal contents and see the resulting price | Confirmed direction; detailed rules proposed |
| FR-04 | Customers can retain basket contents for several shops and see separate shop groups | Confirmed |
| FR-05 | Every checkout contains items from exactly one shop; mixed-shop requests are rejected by the server | Confirmed |
| FR-06 | Customers are informed when relevant products change price or become unavailable | Confirmed; delivery mechanism open |
| FR-07 | The server validates the effective price, product availability, meal selections and shop status before creating an order | Proposed correctness requirement |
| FR-08 | Retrying the same checkout does not create additional orders; deliberate repeat purchases remain possible | Confirmed |
| FR-09 | Customers can cancel an order before acceptance; cancellation after acceptance is denied | Confirmed |
| FR-10 | Administrators can accept or reject eligible orders and advance accepted orders towards collection | Confirmed |
| FR-11 | Customers can view their order history, and administrators can view their own shop's orders | Proposed completion of the ordering journey |
| FR-12 | The demonstration uses simulated payment only and clearly identifies it as simulated | Confirmed |

## 7. Business rules

### 7.1 Baskets and single-shop checkout

**Confirmed:** adding an item from another shop does not require discarding existing selections. The basket overview groups selections by shop, with a separate checkout action for each shop.

The server must determine the actual shop ownership of all submitted products and meal deals. It must reject a mixed-shop request even if the client supplies a single shop ID or the interface normally prevents this action. Rejection must create no orders and consume no basket contents. The server must not silently split the request or purchase only part of it.

Successful checkout for shop A must leave shop B's basket unchanged. A customer may place separate orders with different shops; the restriction applies to the contents of each checkout, not to all purchases made by the customer around the same time.

Proposed concurrency rule: checkout consumes a defined version or set of basket contents. Items added concurrently must not be accidentally removed, and the same consumed basket must not be checked out twice under different request identifiers.

### 7.2 Meal selections and pricing

Confirmed on 3 November 2026 in ADR-002: a meal deal has a base price and required option groups. Each group requires exactly one selection. A fixed component can be represented by a required group containing one option. An option may carry an additional charge.

Example: a HK$58 lunch includes one main and one drink; a particular drink adds HK$3.

- Reject missing groups, duplicate selections, invalid options and options belonging to another shop.
- Calculate the price on the server: base price plus selected supplements, multiplied by quantity.
- Price meal supplements independently of standalone product prices; a standalone price change does not automatically change a meal price. This policy is confirmed in ADR-002.
- Prevent selection of withdrawn options. If a required group has no available options, the meal cannot be ordered.
- Treat different configurations of the same meal as distinct basket lines.
- Save the purchased configuration in the order snapshot.

Confirmed monetary policy: HKD only; nonnegative base prices/supplements with at most two fractional digits; reject higher precision. Use exact decimal arithmetic for addition and integer multiplication, requiring no rounding in this model. Quantity is an integer 1–99 per configuration line. Discounts, taxes and division-based pricing are outside this baseline; see ADR-002. Schema precision and overflow limits remain to be defined.

### 7.3 Price and availability changes

Customer notification and checkout correctness are separate concerns. A live update improves the interface but cannot replace server validation.

Proposed behaviour:

| Situation | Required outcome if this proposal is accepted |
|---|---|
| A basket item changes price | Show the previous and current price and require confirmation of the revised total |
| A selected item or meal option is withdrawn | Keep a visible explanation and prevent purchase until the customer removes or changes it |
| Conditions change after a checkout preview | Reject the stale attempt without creating an order and return actionable change information |
| A product changes after a successful order | Preserve the existing order snapshot and total |
| A notification connection is interrupted | Refresh authoritative state on recovery; checkout remains protected |

The authorisation and data consistency checks must also cover changes between validation and order insertion. The implementation needs an explicit transaction/versioning strategy; a separate read followed by an unconditional write is not sufficient evidence of correctness.

The initial freshness mechanism is undecided. Options include refresh-on-interaction, polling and server-sent events. A measurable update target must be selected before claiming near-real-time behaviour. No zero-latency guarantee is intended.

### 7.4 Retries and deliberate repeat purchases

**Confirmed:** identical products do not prove that two orders are accidental duplicates.

Proposed implementation contract:

- The client retains one idempotency key for the same checkout attempt, including timeout retries and repeated submission.
- The server returns the original result for an equivalent retry, rather than creating another order.
- Reusing the same key with different checkout content is rejected.
- A deliberate new purchase starts a new checkout with current prices and a new key.
- A new key must not bypass protection against consuming the same basket twice.

The customer may be warned about a recent or unfinished order with the same shop and allowed to confirm another purchase. The exact warning trigger is open. This prompt is a usability measure, not the mechanism that prevents duplicate processing.

### 7.5 Order lifecycle and cancellation

**Confirmed:** a shop administrator can accept or reject an order. A customer can cancel only while it has not been accepted.

Proposed minimal lifecycle:

| Current state | Action | Result | Actor |
|---|---|---|---|
| PLACED | Accept | ACCEPTED | Authorised shop administrator |
| PLACED | Reject | REJECTED | Authorised shop administrator |
| PLACED | Cancel | CANCELLED | Owning customer |
| ACCEPTED | Mark ready | READY_FOR_COLLECTION | Authorised shop administrator |
| READY_FOR_COLLECTION | Confirm collection | COMPLETED | Authorised shop administrator |

An additional PREPARING state is optional and not required merely for presentation. Terminal orders cannot be reopened through ordinary status updates.

When cancellation and acceptance occur concurrently, only one transition may succeed. The loser receives the current outcome or a clear conflict response. The system must not report both acceptance and cancellation as successful. Repeated status requests must have defined behaviour and must not repeat side effects.

Suggested shop rule: closing a shop prevents new orders but does not prevent fulfilment of already accepted orders. Withdrawing a product does not automatically cancel existing orders.

### 7.6 Order integrity and history

Proposed requirements:

- Create the order, its lines and selected-option snapshots, and update the relevant basket atomically.
- A failed write must not leave a partial order or remove unpurchased items.
- Preserve purchased names, prices, supplements and quantities independently of subsequent menu edits.
- Prefer withdrawing or archiving products over destroying records needed by historical orders.
- Use simulated payment outcomes only. Do not infer real payment success from an order status.

## 8. Acceptance criteria

These criteria are the intended verification baseline, not completed test results. Proposed policies referenced here require confirmation before implementation.

| ID | Scenario | Expected result | Traceability |
|---|---|---|---|
| AC-01 | Administrator A requests a protected resource belonging to shop B | Read or write denied; B's data unchanged | FR-02 |
| AC-02 | Customer A requests customer B's order | Access denied; no private order information returned | FR-11 |
| AC-03 | A basket contains shop A and shop B items; A is checked out | One A order; B's basket remains unchanged | FR-04, FR-05 |
| AC-04 | A direct API request mixes A and B items | Entire request rejected; no order or basket mutation | FR-05 |
| AC-05 | Separate valid checkout requests target A and B | They are not rejected solely because they target different shops | FR-05 |
| AC-06 | A required meal option is missing, invalid or from another shop | Checkout rejected with a useful reason | FR-03, FR-07 |
| AC-07 | Price changes before checkout is committed | Customer must confirm the new conditions; no silent purchase at an unconfirmed price | FR-06, FR-07 |
| AC-08 | A selected product is withdrawn or the shop closes | New checkout denied; historical orders preserved | FR-06, FR-07 |
| AC-09 | The same checkout is retried or submitted concurrently | At most one order for that checkout; consistent retry result | FR-08 |
| AC-10 | The customer deliberately starts another valid purchase | A new order is permitted, even for identical products | FR-08 |
| AC-11 | Cancellation and acceptance race | One valid transition succeeds; no contradictory success responses | FR-09, FR-10 |
| AC-12 | Cancellation is requested after acceptance | Denied; order remains in its current valid state | FR-09 |
| AC-13 | Order-detail insertion fails | No partial order; original unpurchased basket contents remain | FR-07 |
| AC-14 | The menu is renamed or repriced after purchase | Order history retains the purchased snapshot | FR-11 |
| AC-15 | A full collection journey is demonstrated | Customer orders; administrator accepts, marks ready and completes collection | FR-01–FR-12 |

## 9. Engineering deliverables and quality expectations

The selected approach is a layered monolithic application using Java, Spring Boot, Spring Data JPA/Hibernate and MySQL. Spring Data JPA replaces the earlier MyBatis proposal, following the author's preference for consistency with another project and deeper understanding of one persistence stack. Specific versions and authentication details are design decisions, not frozen by this scope document.

The initial release should include:

1. Database migrations and seed data sufficient to recreate a demonstration with at least two shops, two administrators and two customers.
2. A README with prerequisites, configuration, startup instructions and a demonstration script.
3. API documentation covering input, output, authorisation requirements and expected errors.
4. Automated tests for ownership boundaries, invalid selections, transaction rollback, retries and conflicting state changes.
5. A small demonstration interface or equivalent reproducible API journey.
6. Short architecture decision records for significant choices and their consequences.

Credentials must be externalised and sensitive tokens must not be logged. Server errors must not expose secrets or raw database internals. Error responses must distinguish validation failures, access failures and internal faults. Monetary calculations must be deterministic and server-controlled.

Performance and availability targets are not yet defined. This is a demonstrable portfolio application; production readiness must not be claimed on that basis alone.

## 10. Deferred enhancements

| Enhancement | Reason for deferral | Conditions for considering it |
|---|---|---|
| Near-real-time notifications | Correct checkout is more important than transport selection | Core change handling works; a measurable freshness target is agreed |
| Redis menu caching | Adds invalidation and isolation complexity | Uncached behaviour is correct; keys and invalidation respect shop boundaries |
| Basic sales reports | Useful but not necessary to complete ordering | Completed-order data and historical snapshots are reliable |
| Daily/monthly charts | Presentation depends on agreed metrics | Basic reporting has defined counting and date rules |

Proposed reporting semantics: count quantities and distinct orders separately; include completed orders only; use historical line values; do not double-count meal revenue as standalone component revenue. Reports must remain shop-scoped. Date ranges should use a defined business timezone and half-open boundaries. Simulated order value is not actual settled revenue. Forecasts and automated trading recommendations remain excluded.

## 11. Relationship to the previous application

The comparison is based on the uploaded local source and its static review, not a verified live GitHub comparison or successful execution.

| Previous capability | Treatment in the rebuild |
|---|---|
| Product, category and meal management | Retain the business capability; add explicit shop ownership and validation |
| Fixed meal contents | Extend with a bounded selectable-option model |
| Basket CRUD using JWT user identity | Retain the intent; add multiple shop groups and protected single-shop checkout |
| Transactional order creation | Retain the transaction boundary; address snapshots, retries and concurrent changes |
| Administrator BCrypt/JWT login | Retain secure credential handling; replace permissive management access with enforced authorisation |
| Simplified customer identity creation | Replace with a reusable customer identity approach; exact login scope open |
| Shop status stored under one Redis key | Replace with shop-specific authoritative status |
| Partial Redis cache-aside reads | Reconsider after functional correctness; do not claim complete cache-failure protection |
| OSS image upload | Retain basic upload capability if needed; validate ownership, files and client lifecycle |
| Disabled/partial WebSocket notification | Preserve the relevant notification requirement; choose the transport afresh |
| Disabled unpaid-order timeout logic | Do not carry forward without a corresponding agreed payment rule |
| Missing reproducible schema and incomplete tests | Address through migrations and meaningful tests from the start |

Existing implementation choices are references, not requirements to reproduce old defects or unnecessary complexity.

## 12. Decision log

| ID | Date | Decision and status | Rationale / consequence |
|---|---|---|---|
| DEC-01 | 19 October 2026 | Rebuild a bounded project; confirmed direction | Restore understanding and practise deliberate engineering rather than reconstruct every old design choice |
| DEC-02 | 19 October 2026 | Collection only; confirmed correction | Avoid delivery and address-management complexity; supersedes the delivery assumption in v0.2 |
| DEC-03 | 19 October 2026 | Simulated payment only; confirmed | Payment integration is not a learning priority for this project |
| DEC-04 | 19 October 2026 | Multiple shop baskets, one shop per checkout; confirmed | Preserve a useful overview while avoiding split-order coordination |
| DEC-05 | 19 October 2026 | Reject mixed-shop checkout on the server; confirmed | UI restrictions alone do not enforce the business boundary |
| DEC-06 | 19 October 2026 | Customer cancellation only before acceptance; confirmed | Establish a clear boundary between cancellation and preparation |
| DEC-07 | 19 October 2026 | Permit intentional repeat purchases; confirmed | Duplicate protection must identify retries, not prohibit identical orders |
| DEC-08 | 19 October 2026 | Enforce shop-scoped administrative access; confirmed | Multi-shop ownership is a core requirement, not an optional UI restriction |
| DEC-09 | 19 October 2026 | Maintain the scope document in English; confirmed | Make objectives, rules and reasons understandable to future readers |
| DEC-10 | 22 October 2026 | Use Spring Data JPA/Hibernate; confirmed in implementation | Align with the author's other project; retain explicit SQL schema constraints and Flyway migrations |
| DEC-11 | 22 October 2026 | M1 menu displays unavailable products with status; implemented | Preserve menu visibility; later checkout must independently reject unavailable selections |
| DEC-12 | 25 October 2026 | Use server-side Session authentication for M2; confirmed | Focus on authentication, authorisation and shop ownership; lifecycle and API defaults documented for implementation review in ADR-001 |

| DEC-13 | 3 November 2026 | Accept bounded meal selections and independent supplements | One choice per required group; server pricing; HKD; quantity 1–99; details in ADR-002 |

## 13. Open decisions and next steps

| ID | Question | When to resolve |
|---|---|---|
| OPEN-01 — resolved | Exactly one choice per required group, independent supplements, HKD two-decimal inputs and quantity 1–99; see ADR-002. | Confirmed 3 November 2026; implementation pending |
| OPEN-02 | How does simulated payment interact with order creation, and are failure cases required? | Before checkout implementation |
| OPEN-03 | When should a same-shop repeat-purchase warning appear? | Before checkout interface design |
| OPEN-04 | What freshness target is required for changes on an already open page? | Before choosing polling or push notifications |
| OPEN-05 — resolved | Seeded customers/admins; Session login/logout; no public registration or merchant onboarding in M2. See ADR-001. | Implemented; 3 November 2026 |
| OPEN-06 | What exact order states and collection-confirmation action are needed? | Before order API design |
| OPEN-07 | What pricing confirmation, basket versioning and concurrency strategy will be used? | Before checkout implementation |

Next, resolve the minimum identity, meal and checkout rules, then design one vertical slice with its schema, API examples and acceptance tests. Do not expand the scope merely to include an additional framework.

## 14. Document maintenance

Update this document when a business rule or scope decision changes. Preserve stable requirement IDs where possible, record the reason and identify affected acceptance criteria. Keep implementation details and experiments in architecture decision records or the engineering notebook. Record actual test evidence separately from intended acceptance criteria.

| Version | Date | Change |
|---|---|---|
| 0.1 | 19 October 2026 | Initial scope discussion and comparison with the earlier application |
| 0.2 | 19 October 2026 | Recorded multi-shop baskets, cancellation and repeat-purchase handling; included a delivery interpretation subsequently corrected by the author |
| 0.3 | 19 October 2026 | Rewritten in English; collection-only scope confirmed; single-shop checkout and server-side rejection confirmed; added traceable requirements, acceptance criteria and decision history |

| 0.4 | 22 October 2026 | Recorded selected JPA stack, M1 implementation status and unavailable-product browsing policy |
