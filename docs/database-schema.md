# Database schema

Schema changes are made only through Flyway migrations. Never edit a migration that has been applied;
add a new version instead.

## Databases

`infra/postgres/init.sql` creates four databases on the local server:

| Database | Used by | Status |
|---|---|---|
| `orderdb` | order-service | In use |
| `inventorydb` | inventory-service | In use |
| `paymentdb` | payment-service | Created empty, **unused** (planned) |
| `auditdb` | audit-service | Created empty, **unused** (planned) |

## Implemented: orderdb (`V1__create_orders.sql`)

`orders`

| Column | Type | Notes |
|---|---|---|
| id | UUID | Primary key |
| customer_id | VARCHAR(64) | Not null |
| status | VARCHAR(20) | `PENDING`, `CONFIRMED`, `CANCELLED` |
| total_amount | NUMERIC(12,2) | Not null |
| created_at | TIMESTAMPTZ | Not null |

`order_items`

| Column | Type | Notes |
|---|---|---|
| id | UUID | Primary key |
| order_id | UUID | Foreign key to `orders`, cascade delete |
| product_id | UUID | Not null. **Not** a foreign key: products live in another service's database |
| quantity | INTEGER | Must be greater than 0 |
| unit_price | NUMERIC(12,2) | Client-supplied in Phase 1 |

Indexes: `order_items(order_id)`, `orders(status)`.

## Implemented: inventorydb

`V1__create_products.sql`: table `products`

| Column | Type | Notes |
|---|---|---|
| id | UUID | Primary key |
| sku | VARCHAR(64) | Not null, unique (constraint `products_sku_key`) |
| name | VARCHAR(255) | Not null |
| price | NUMERIC(12,2) | Not null |
| stock_quantity | INTEGER | Not null, must be 0 or more |
| created_at | TIMESTAMPTZ | Not null |

`V2__seed_products.sql`: three development products (`SKU-LAPTOP-01`, `SKU-HEADPHONE-01`,
`SKU-KEYBOARD-01`) with fixed IDs `1111...`, `2222...`, `3333...` for easy manual testing.

## Planned (not implemented)

- Order: `outbox_events`, `saga_state`
- Inventory: `reservations`, `processed_events`
- Payment: `payments`, `outbox_events`, `processed_events`
- Audit: `audit_events`, `merkle_batches`
