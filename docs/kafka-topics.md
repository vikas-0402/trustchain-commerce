# Kafka topics

**Status: PLANNED. No topics are created and no application code produces or consumes messages.**
Phase 1 only configures a single-node Kafka broker (KRaft mode, `apache/kafka:3.8.0`) in Docker Compose; it starts only when you run `docker compose up`. It is
reachable at `localhost:9092` from the host and `kafka:29092` from other containers.

## Planned topics

| Topic | Producer | Consumers | Planned events |
|---|---|---|---|
| `order.events` | Order service | Inventory, Payment, Notification, Audit | `OrderCreated`, `OrderConfirmed`, `OrderCancelled` |
| `inventory.events` | Inventory service | Order, Audit | `InventoryReserved`, `InventoryFailed` |
| `payment.events` | Payment service | Order, Notification, Audit | `PaymentLocked`, `PaymentFailed`, `PaymentReleased`, `PaymentRefunded` |
| `order.events.dlq`, `inventory.events.dlq`, `payment.events.dlq` | Consumers | Manual inspection | Messages that failed after retries |

## Planned event envelope (proposal, not final)

`eventId`, `eventType`, `aggregateId` (the order ID), `occurredAt`, `payload`.
The message key is the order ID so that all events for one order stay in order on one partition.

These names and fields are a design proposal and may change when Phase 2 is implemented.
