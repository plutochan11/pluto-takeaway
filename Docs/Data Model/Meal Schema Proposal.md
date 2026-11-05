# Meal Schema Proposal

Updated: 4 November 2026. Status: engineering proposal for review; no migration applied.

Business policy: ADR-002/003/004 in [Decisions](../Decisions.md). Logical relationships and examples: [Meal Model Draft](./Meal%20Model%20Draft.md).

## Tables and ownership

| Table | Proposed columns | Ownership |
|---|---|---|
| meal | id, shop_id, category_id, name, base_price, status | Explicit shop; category must be same-shop |
| meal_option_group | id, shop_id, meal_id, name, sort_order | Composite FK ensures group shop matches parent meal |
| meal_option | id, shop_id, group_id, product_id (nullable), label (nullable), supplement, status, sort_order | Composite FKs ensure parent group and optional product match option shop |

Store shop_id on each table to support shop-scoped queries and enforce same-shop references. These repeated ownership values are constrained copies, not independent assignments a client can change. Option ownership reaches its meal through the group; an additional meal_id on option is unnecessary in this proposal.

## Proposed SQL types and defaults

- IDs: signed BIGINT; each table has generated AUTO_INCREMENT primary key id. Parent reference types match.
- meal.name: VARCHAR(100), required; group.name: VARCHAR(50), required; option.label: VARCHAR(100), conditionally required. Application validation rejects blanks.
- base_price and supplement: DECIMAL(10,2), required, nonnegative. This matches current product storage; domain maxima and total-overflow handling still need agreement before API/price-calculation implementation. Reject input overprecision in application validation; do not rely on database coercion.
- status on meal/option: VARCHAR(20), required, default UNAVAILABLE; CHECK restricts AVAILABLE/UNAVAILABLE. Group status is omitted; all groups are required.
- sort_order on groups/options: INT, required, default 0; CHECK >= 0. Order by sort_order, id within the parent.

## Foreign keys and candidate keys

| Child reference | Parent key | Purpose |
|---|---|---|
| meal(shop_id, category_id) | category(shop_id, id), already unique | Same-shop category |
| meal_option_group(shop_id, meal_id) | meal(shop_id, id), add UNIQUE | Same-shop parent meal |
| meal_option(shop_id, group_id) | meal_option_group(shop_id, id), add UNIQUE | Same-shop parent group |
| meal_option(shop_id, product_id) | product(shop_id, id), add UNIQUE | Same-shop product, when present |

The category reference anchors meal ownership to an existing Shop through category's FK. No additional direct Shop FK is required for each descendant in this proposal. A null product_id intentionally permits a non-product option; the group FK still enforces its shop ownership. Do not cascade-delete products, meals or groups in this baseline; disabling/withdrawal behaviour is designed in management APIs.

The product composite unique key is a new additive schema change. Existing global id uniqueness does not remove the need to provide an explicit referenced composite key. Add it in a new migration rather than editing V1.

## Conditional option constraint

Permit exactly one of these cases:

1. product_id is non-null, label is null, supplement is nonnegative.
2. product_id is null, label is non-null/nonblank, supplement equals zero.

Write the final CHECK with explicit IS NULL / IS NOT NULL branches to avoid accidentally accepting invalid rows through SQL's unknown/null CHECK result. Application validation must enforce the same rule with useful errors. Final SQL and MySQL constraint verification are pending.

## Rules outside simple row constraints

Application code must verify exactly one valid selection per required group, reject duplicate/foreign selections, determine effective availability using meal/option/product state, and calculate price on the server. Ordinary row CHECK constraints cannot enforce all of these cross-row and request-dependent rules.

## Review and implementation order

1. Resolve any product/business changes to this proposal; confirm monetary bounds before pricing API work.
2. Write and review a new additive migration for keys and tables; retain applied migrations unchanged.
3. Verify constraints against a fresh test database, including cross-shop references and both valid/invalid option shapes.
4. Design management setup workflow and public menu compatibility before writing endpoints. No existing M1 response shape is changed by this document.

## V5 implementation checkpoint — 4 November 2026

The proposed tables, ownership keys and row checks are now implemented in `Backend/pluto-takeaway/src/main/resources/db/migration/V5__create_meal_tables.sql`. Existing applied migrations were not edited. This supersedes the initial “no migration applied” status for the independent test database only; no development/Compose database migration was performed by the agent.

Agent ran `mvn test` with Docker Testcontainers: Flyway successfully applied V1–V5 to a fresh MySQL database; all 43 existing tests passed (zero failures/errors). This proves migration startup and regression coverage, not direct rejection tests for each new constraint. Constraint-negative tests, meal entities/APIs, seed meals and effective selection/pricing logic are still pending. Adding V5 to main resources means the next ordinary application startup will attempt it against the selected database.

## Constraint verification — 4 November 2026

Added MealSchemaTests with JdbcTemplate inserts against the migrated Testcontainers MySQL database. Seven tests cover valid product-backed/non-product options and safe defaults; cross-shop meal/category, group/meal, option/group and option/product rejection; four invalid product/label/supplement shapes; and a negative supplement. Assertions identify the rejecting constraint; CHECK failures also verify MySQL error 3819. Test-managed transactions roll back fixture rows after each method; no HTTP commit behaviour is inferred from these tests.

Agent ran the full Maven suite successfully: 50 tests (43 API/account tests plus 7 schema tests), zero failures/errors. The first constraint-test run rejected invalid rows correctly but two assertions assumed the wrong Spring exception translation; assertions were corrected without changing schema. This is targeted constraint coverage, not exhaustive validation of every CHECK or future meal selection/pricing rule.

## JPA mapping checkpoint — 4 November 2026

Pluto added Meal, MealOptionGroup and MealOption entities (field access, protected no-argument constructors, getters) and shop-scoped repositories. Three further tests insert rows through JDBC, then read through repositories to verify monetary/string/nullable field mappings, shop and parent filtering, and sort_order/id tie-breaking. No JPA save is used to prepare these rows, avoiding a test that only reads back an already-managed setup entity. Agent ran the full suite: 53 tests, zero failures/errors. Entity creation APIs, application defaults and pricing/selection validation remain pending.
