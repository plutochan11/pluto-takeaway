# Pluto Takeaway — Data Model

| Field | Value |
|---|---|
| Last updated | 22 October 2026 |
| Milestone | M1 — shop and menu browsing |
| Status | M1 schema implemented in Flyway V1; see entity mappings and API contract for current behaviour |
| Confirmed decision | Each shop owns its categories; categories are not shared across shops |
| Implementation baseline | SQL types, name limits, status strings, ordering and ownership constraints below are implemented in M1; enum conversion and future management rules remain refinements |

## 1. Relationships

```text
Shop 1 ─── N Category
Shop 1 ─── N Product
Category 1 ─── N Product
```

- Each category belongs to exactly one shop.
- Each product belongs to exactly one shop and one category in the initial model.
- A shop or category may initially have no products.
- Categories with the same name in different shops are independent records.
- A product and its referenced category must belong to the same shop.

## 2. Shop

Proposed SQL table name: `shop`. All fields below are required (`NOT NULL`).

| Field | Proposed MySQL type | Example | Purpose / rule |
|---|---|---|---|
| `id` | `BIGINT` | `1` | Primary key; database-generated using `AUTO_INCREMENT`; no business meaning |
| `name` | `VARCHAR(100)` | `Pluto Kitchen` | Display name; reject blank names; maximum length is a proposed business limit |
| `status` | `VARCHAR(20)` | `OPEN` | Proposed allowed values: `OPEN`, `CLOSED`; current ordering availability, not account suspension or opening-hours scheduling |

## 3. Category

Proposed SQL table name: `category`. All fields below are required (`NOT NULL`).

| Field | Proposed MySQL type | Example | Purpose / rule |
|---|---|---|---|
| `id` | `BIGINT` | `10` | Primary key; database-generated using `AUTO_INCREMENT` |
| `shop_id` | `BIGINT` | `1` | Foreign key referencing `shop.id`; identifies the owning shop |
| `name` | `VARCHAR(50)` | `Main Courses` | Shop-customised display name; reject blank names |
| `sort_order` | `INT` | `10` | Display priority within the owning shop; lower numbers appear first; proposed default `0`; must be nonnegative |

`sort_order` sorts category records, not the products inside them. For example, Main Courses = 10, Drinks = 20, Desserts = 30. An administrator can put Drinks first by changing its value to 5.

Numbers need not be consecutive or unique. Use `ORDER BY sort_order ASC, id ASC` for a stable category order within a shop. For M1, product ordering can use `id ASC` within each category; custom product ordering can be added separately if required.

## 4. Product

Proposed SQL table name: `product`. All fields below are required (`NOT NULL`).

| Field | Proposed MySQL type | Example | Purpose / rule |
|---|---|---|---|
| `id` | `BIGINT` | `100` | Primary key; database-generated using `AUTO_INCREMENT` |
| `shop_id` | `BIGINT` | `1` | Foreign key referencing `shop.id`; supports explicit shop ownership and shop-scoped queries |
| `category_id` | `BIGINT` | `10` | References a category belonging to the same shop |
| `name` | `VARCHAR(100)` | `Chicken Rice` | Display name; reject blank names |
| `price` | `DECIMAL(10,2)` | `58.00` | Exact decimal unit price; proposed currency HKD; must be nonnegative |
| `status` | `VARCHAR(20)` | `AVAILABLE` | Proposed allowed values: `AVAILABLE`, `UNAVAILABLE`; not a numerical inventory count |

`DECIMAL(10,2)` means ten decimal digits in total, including two fractional digits. The proposed nonnegative range is `0.00` to `99999999.99`; this is storage capacity, not a suggested normal menu price. Use Java `BigDecimal`, not `float` or `double`, for prices and calculations.

Proposed input policy: reject unit prices with more than two fractional digits instead of relying on database conversion. Confirm rounding rules separately before introducing calculations that require rounding.

## 5. Ownership consistency and constraints

Product stores both `shop_id` and `category_id`. This makes shop ownership explicit, but introduces a consistency rule:

```text
product.shop_id must equal the shop_id of its referenced category
```

Two independent foreign keys to `shop.id` and `category.id` do not enforce that equality.

Proposed database enforcement:

1. Add a unique key on category `(shop_id, id)`.
2. Add a composite foreign key from product `(shop_id, category_id)` to category `(shop_id, id)`.
3. Keep category and product ownership linked to an existing shop.
4. Validate ownership in the application as well, so callers receive a useful error.

Use identical types and signedness for corresponding ID columns. Proposed baseline: signed `BIGINT` mapped to Java `Long`, with generated positive IDs. Sequential IDs are identifiers, not access-control secrets; authorisation is still required.

Proposed checks include nonnegative prices and sort values, and allowed status strings. Confirm the installed MySQL version supports enforced `CHECK` constraints before writing the migration. Do not cascade-delete menu records by default; withdrawal and historical order references will be designed in later milestones.

## 6. SQL types versus Java types

| Meaning | Proposed MySQL storage | Java representation |
|---|---|---|
| Identifier | `BIGINT` | `Long` |
| Name | `VARCHAR(n)` | `String` |
| Category order | `INT` | `Integer` |
| Price | `DECIMAL(10,2)` | `BigDecimal` |
| Shop status | `VARCHAR(20)` | `ShopStatus` enum, stored by explicit name |
| Product status | `VARCHAR(20)` | `ProductStatus` enum, stored by explicit name |

A Java enum and a MySQL `ENUM` column are separate choices. The intended refinement is Java enums plus readable database strings and database constraints. Do not store Java enum ordinal positions such as 0 or 1. The current M1 JPA entities use String status fields; Java enum mapping is a later refinement. Verify the explicit database string mapping when introducing enums.

## 7. Decisions still to confirm

- Accept numeric `BIGINT AUTO_INCREMENT` IDs for this single-database application?
- Accept name limits of 100 characters for shops/products and 50 for categories?
- Accept the proposed status values and nonnegative category ordering rule?
- Accept HKD unit prices with two fractional digits and the proposed overprecision rejection rule?
- Decide category visibility/archiving when management is implemented; no category status field is included in this M1 draft.

## 8. Markdown table example

Write a header row, a separator row, then one row per item. Backticks format code names and types. Blank lines separate the table from surrounding paragraphs.

```markdown
| Field | MySQL type | Purpose |
|---|---|---|
| `id` | `BIGINT` | Primary key |
| `name` | `VARCHAR(100)` | Display name |
```

This renders as:

| Field | MySQL type | Purpose |
|---|---|---|
| `id` | `BIGINT` | Primary key |
| `name` | `VARCHAR(100)` | Display name |

Column widths in the source do not need to line up. The `|` characters separate columns. To include a literal pipe inside a cell, escape it as `\|`.

## 9. References

- [MySQL: Exact fixed-point types](https://dev.mysql.com/doc/refman/8.4/en/fixed-point-types.html)
- [MySQL: ENUM type](https://dev.mysql.com/doc/refman/8.4/en/enum.html)

The MySQL references explain type behaviour. The proposed field lengths and business policies are project decisions, not MySQL requirements.
