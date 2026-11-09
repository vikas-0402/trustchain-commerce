# API specification (Phase 1, implemented)

Interactive docs: `/swagger-ui.html` on each service, OpenAPI JSON at `/v3/api-docs`.
Health: `/actuator/health`. No authentication in Phase 1.

## Order service (port 8081)

> **Phase 1 simplification:** `unitPrice` is supplied by the client and is **not** validated against
> inventory data, and stock is **not** checked or reserved. This is temporary.

| Method | Path | Success | Errors |
|---|---|---|---|
| POST | `/orders` | 201, `Location: /orders/{id}` | 400 validation or total too large |
| GET | `/orders/{id}` | 200 | 404 unknown id, 400 malformed id |
| GET | `/orders` | 200, newest first | none |

Request:

    {"customerId":"cust-1","items":[{"productId":"11111111-1111-1111-1111-111111111111","quantity":1,"unitPrice":899.00}]}

Response (status is always `PENDING` in Phase 1):

    {"id":"...","customerId":"cust-1","status":"PENDING","totalAmount":899.00,
     "createdAt":"2026-11-08T10:00:00Z",
     "items":[{"productId":"11111111-1111-1111-1111-111111111111","quantity":1,"unitPrice":899.00}]}

Validation (limits match the database columns):

| Field | Rule |
|---|---|
| customerId | required, not blank, max 64 characters |
| items | required, at least one item |
| items[].productId | required UUID |
| items[].quantity | integer, at least 1 |
| items[].unitPrice | required, at least 0.01, at most 10 integer digits and 2 decimals |
| order total | must not exceed 9,999,999,999.99 |

## Inventory service (port 8082)

| Method | Path | Success | Errors |
|---|---|---|---|
| POST | `/products` | 201, `Location: /products/{id}` | 400 validation, 409 duplicate SKU |
| GET | `/products/{id}` | 200 | 404 unknown id, 400 malformed id |
| GET | `/products` | 200 | none |

Request:

    {"sku":"SKU-1","name":"Widget","price":10.00,"stockQuantity":5}

Validation:

| Field | Rule |
|---|---|
| sku | required, not blank, max 64 characters, unique |
| name | required, not blank, max 255 characters |
| price | required, at least 0.01, at most 10 integer digits and 2 decimals |
| stockQuantity | integer, 0 or more |

Duplicate SKUs return 409 both when caught by the existence check and when two concurrent requests
race and the database unique constraint rejects the second insert.

## Error format

Errors raised by the application use:

    {"timestamp":"2026-11-08T10:00:00Z","status":404,"error":"Not Found","message":"Order not found: ..."}

Validation errors list each failing field in `message`, for example `customerId: must not be blank`.
A malformed path ID (not a UUID) or malformed JSON returns 400 with Spring Boot's default error body,
which has a different shape.

## Planned (not implemented)

Payment, delivery confirmation, audit verification (`GET /audit/verify/{eventId}`), gateway
authentication and an idempotency-key header on `POST /orders`.
