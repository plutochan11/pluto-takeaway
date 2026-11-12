# M3 Management API Contract — Draft

Updated: 5 November 2026. Current decision: atomic complete creation (ADR-007). The earlier incremental creation proposal (ADR-006) is superseded before implementation. No M3 HTTP endpoints exist yet; V5 schema, meal entities and repositories are implemented. See [Decisions](./Decisions.md).

## Create a complete meal

`POST /api/admin/shops/{shopId}/meals`, JSON, valid same-session CSRF, authenticated SHOP_ADMIN owning target shop.

```json
{
  "categoryId": 10,
  "name": "Lunch Meal",
  "basePrice": 58.00,
  "groups": [
    {
      "name": "Main",
      "sortOrder": 10,
      "options": [
        {"productId": 3000, "supplement": 0.00, "sortOrder": 10}
      ]
    },
    {
      "name": "Drink",
      "sortOrder": 20,
      "options": [
        {"productId": 3004, "supplement": 3.00, "sortOrder": 10},
        {"productId": null, "label": "No Drink", "supplement": 0.00, "sortOrder": 20}
      ]
    }
  ]
}
```

Product IDs are hypothetical same-shop examples, not current seed IDs. Existing products/categories are referenced, not created or modified. Request nesting defines new parent/child membership. One transaction commits all new meal/group/option records or rolls all of them back. Meal and options begin UNAVAILABLE; activation/publication is separate.

## Request DTOs and input rules

| DTO | Fields and validation |
|---|---|
| CreateMealRequest | categoryId required positive Long; name nonblank <=100; basePrice required BigDecimal 0..9999.99 with at most two fractional digits; groups required nonempty list of non-null recursively validated CreateMealGroupRequest |
| CreateMealGroupRequest | name nonblank <=50; sortOrder required nonnegative Integer; options required nonempty list of non-null recursively validated CreateMealOptionRequest |
| CreateMealOptionRequest | optional positive productId; label conditionally required/nonblank <=100; supplement required BigDecimal 0..9999.99 with at most two fractional digits; sortOrder required nonnegative Integer |

Option shape: productId present -> label null/absent. productId null -> label required/nonblank and supplement exactly zero. Product-backed choices display the current Product name; no standalone price is accepted as supplement. Exactly-one selection is a later purchasing rule; groups may offer many options. Unavailable products may be configured but cannot be selected for purchase later.

A product ID can appear only once within a group, even if the options have different supplements. The same product may appear in different groups. Apply the same rule to the resulting group when a group-edit API is added. Non-product choices have no product ID and are outside this rule.

Ignore unknown properties consistently on all three request DTOs, retaining M2's explicit narrow-mapping policy. Body id/shopId/mealId/groupId/status cannot override generated IDs, checked ownership, nesting or initial availability. API money caps do not change DECIMAL(10,2) schema storage. No automatic trimming.

Initial collection limits: at most 20 groups per meal and 50 options per group (at most 1,000 options per create request). Enforce using DTO collection-size constraints before exposing the endpoint. These are adjustable application limits, not database constraints or a general HTTP request-body size limit.

## Response

201 with nested DTOs (shape fixed 6 November 2026):

| DTO | Fields |
|---|---|
| MealResponse | id, shopId, categoryId, name, basePrice, status, groups: List<MealGroupResponse> |
| MealGroupResponse | id, name, sortOrder, options: List<MealOptionResponse> |
| MealOptionResponse | id, nullable productId, name, supplement, status, sortOrder |

Option `name` is the current referenced Product name for product-backed choices, or the option label for non-product choices. The response omits internal group/shop references already represented by nesting. Never return entities directly. Sort groups/options by sortOrder, then generated id. No detail/list endpoint or Location URI for an unimplemented endpoint is implied.

## Service and error sequence

Security checks precede MVC; recursively validated DTO input can fail before service ownership checks. Tests aimed at access rules supply otherwise valid input and CSRF.

1. Resolve actor, require SHOP_ADMIN and target-shop ownership. Missing account -> 401 AUTHENTICATION_REQUIRED; denied scope -> 403 ACCESS_DENIED.
2. Resolve category: missing -> 404 RESOURCE_NOT_FOUND; another shop -> 403 ACCESS_DENIED.
3. Validate option shapes and referenced products before writes where practical. Missing product -> 404 RESOURCE_NOT_FOUND; foreign-shop product -> 403 ACCESS_DENIED. Collect product IDs for batch lookup rather than querying once per option.
4. Within one write transaction, create Meal, obtain its generated ID, create Groups, obtain their IDs and create Options. Derive every shop/parent reference on the server. Explicitly initialise safe statuses.
5. Build DTO and commit; a child-write failure rolls back all created records. ID sequence gaps after rollback are normal.

Invalid input/shape -> 400 VALIDATION_FAILED; missing/invalid CSRF -> 403 CSRF_INVALID. Use existing `{code,message}` representation. No blanket catch of database errors as success/client errors. Frontend still handles failures and response-loss uncertainty; automatic POST retry is not promised.

## Development slices and acceptance

1. Nested request DTOs and recursive/basic validation; conditional option validation follows before endpoint completion.
2. Creation factories and shared shop-access policy; transactional service with category/product checks and focused helpers.
3. Controller and nested response, successful complete persistence for both shops.
4. Access/CSRF denial; invalid/null/empty nested inputs; price boundaries; missing/foreign references; ignored assignment fields; late-child failure proving actual rollback after earlier writes.

HTTP tests must observe real service commit independently of outer test transactions, inspect DB rows/links and clean up their generated data. Distinguish rejection before writes from rollback after writes. Do not change applied migrations or dev seeds merely for test setup.

## Publish a meal

`PUT /api/admin/shops/{shopId}/meals/{mealId}/publication` accepts JSON and requires a valid session CSRF token and a SHOP_ADMIN who owns the target shop.

```json
{"enabledOptionIds": [101, 104]}
```

The list replaces the meal's complete enabled-option selection. It must contain 1 to 1,000 distinct positive option IDs, all belonging to this meal. An ID from another meal or shop is invalid input. Every selected product-backed option must reference a product that is currently AVAILABLE. Selecting even one unavailable product rejects the request, even if another option covers its group. A non-product choice can be selected on its own. Every required group must contain at least one selected option. A meal with no groups cannot be published.

In one transaction, lock the target meal before loading its options, validate the proposed selection, set selected options to AVAILABLE and omitted options to UNAVAILABLE, and set the meal to AVAILABLE. The meal lock serializes concurrent Publish requests for the same meal. Successful publication returns 204. Repeating Publish replaces the selection again, subject to current eligibility checks. A rejected request leaves the prior selection and meal status intact.

Return 404 RESOURCE_NOT_FOUND for a meal absent from the target shop, 400 VALIDATION_FAILED for invalid input, and 409 MEAL_NOT_PUBLISHABLE when any selected product is unavailable or the selection cannot cover every required group. Existing authentication, ownership and CSRF responses apply. Stored publication status records admin intent. Public menu and purchase eligibility must use current product availability, so a later product change can make a published meal unpurchasable.

## Public menu meals

Extend `GET /api/shops/{shopId}/menu` with a `meals` array on each category. Keep the existing shop, category and product fields. Return an empty array for categories without published meals.

- Include meals whose saved status is AVAILABLE. Show only options enabled by the admin. An enabled option backed by a currently unavailable product remains visible with `selectable: false`.
- Include all required groups, even when a group has no selectable options. A published meal has `configurable: true` only when it has at least one group and every group has a selectable option. Keep published meals visible when this flag is false. The shop's separate status continues to indicate whether it is open.
- A public meal exposes `id`, `name`, `basePrice`, `configurable` and `groups`. Each group exposes `id`, `name` and `options`. Each option exposes `id`, `name`, `supplement` and `selectable`. Use the current product name for a product-backed option and the stored label for a non-product option.
- Order meals by ID and groups and options by sort order, then ID. Load the hierarchy in batches and reuse the products already loaded for the menu.

## Remaining design work

Verify collection-size boundaries in tests. Define calculated unit and line-total limits before pricing implementation. Implement publication, effective availability, later editing, reads/reconciliation and compatible public menu responses in subsequent slices.
