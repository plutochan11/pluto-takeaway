# M1 Public Browsing API Contract

Updated: 3 November 2026. Describes the current implementation, not future ordering endpoints.

Base URL for local development: `http://localhost:8080`. Authentication is not required for these routes.

## Representations and rules

- IDs are JSON integers, represented as Java `Long` internally.
- Prices are JSON numbers backed by Java `BigDecimal` and SQL `DECIMAL(10,2)`; proposed currency is HKD. Clients must not rely on trailing-zero formatting.
- Shop status is currently a string: `OPEN` or `CLOSED`.
- Product status is currently a string: `AVAILABLE` or `UNAVAILABLE`.
- Public browsing currently includes closed shops and unavailable products; this does not grant permission to place an order.
- Shop lists use ID ascending order. Categories use `sortOrder ASC, id ASC`; products within each category use ID ascending order.
- Empty collections are `[]`, not `null`.

## GET /api/hello

Returns HTTP 200 with plain text `Pluto Takeaway is running.`. This verifies the HTTP path, not database connectivity.

## GET /api/shops

Returns HTTP 200 with a JSON array of all shops. An empty database returns `[]`. Pagination is not implemented.

```json
[
  {"id": 1, "name": "Pluto Kitchen", "status": "OPEN"},
  {"id": 2, "name": "Harbour Cafe", "status": "OPEN"}
]
```

Additional manually created fixture shops also appear in this list.

## GET /api/shops/{shopId}

Returns HTTP 200 with a single shop object if it exists:

```json
{"id": 1, "name": "Pluto Kitchen", "status": "OPEN"}
```

An unknown ID returns HTTP 404 via `ResponseStatusException`. A nonnumeric value that cannot bind to `Long` returns HTTP 400. Positive-ID input validation has not been separately implemented.

## GET /api/shops/{shopId}/menu

Returns HTTP 200 for an existing shop, including an empty shop. All category/product queries are filtered by the requested shop ID on the server.

```json
{
  "shop": {"id": 2, "name": "Harbour Cafe", "status": "OPEN"},
  "categories": [
    {
      "id": 20,
      "name": "Drinks",
      "products": [
        {"id": 200, "name": "Coffee", "price": 28.00, "status": "AVAILABLE"},
        {"id": 201, "name": "Lemon Tea", "price": 25.00, "status": "UNAVAILABLE"}
      ]
    }
  ]
}
```

A shop with no categories returns `{"shop": {...}, "categories": []}`. A category with no products remains present with `"products": []`. An unknown shop returns HTTP 404; a nonnumeric ID returns HTTP 400.

## Errors and current limits

M2 introduced a stable JSON body for these documented cases:

- Unknown shop: HTTP 404, `{"code":"RESOURCE_NOT_FOUND","message":"Resource not found"}`.
- Nonnumeric shop ID: HTTP 400, `{"code":"VALIDATION_FAILED","message":"Request input is invalid"}`.

Status, JSON content type and error fields have integration-test assertions. See [M2 contract](./M2%20API%20Contract.md) for security and write-validation errors. Other framework errors are not universally normalised by this contract.
