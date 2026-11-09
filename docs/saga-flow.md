# Saga flow

**Status: PLANNED. Nothing in this document is implemented.** In Phase 1 the Order service only
stores an order with status `PENDING`; no event is published, no stock is reserved, no payment is made.

## Planned happy path

| Step | Actor | Result |
|---|---|---|
| 1 | Client calls `POST /orders` | Order saved as `PENDING`, `OrderCreated` recorded in the outbox |
| 2 | Inventory service | Stock reserved, `InventoryReserved` |
| 3 | Payment service | Funds locked in the escrow contract, `PaymentLocked` |
| 4 | Order service | Order becomes `CONFIRMED` |
| 5 | Delivery confirmed (simulated endpoint) | Escrow released, `PaymentReleased` |

## Planned compensation

| Failure | Compensation |
|---|---|
| Stock unavailable (`InventoryFailed`) | Order becomes `CANCELLED` |
| Payment lock fails (`PaymentFailed`) | Reserved stock released, order `CANCELLED` |
| Order cancelled after funds were locked | Escrow `refund()`, `PaymentRefunded` |

## Planned reliability rules

- Events are written to an outbox table in the same transaction as the state change.
- Consumers ignore events they have already processed.
- Failed messages go to a dead-letter topic after retries.

## Order statuses

`PENDING`, `CONFIRMED`, `CANCELLED` exist as enum values today. Only `PENDING` is ever assigned
in Phase 1.
