# Development roadmap

| Phase | Scope | Status |
|---|---|---|
| 1 | Foundation: Maven multi-module, Order and Inventory services, PostgreSQL + Flyway, Docker Compose, tests, docs | Implemented. Build and test run still to be confirmed on a machine with Maven and Docker |
| 2 | Transactional outbox, Kafka producers and consumers, duplicate-event protection | Planned |
| 3 | Saga orchestration, idempotency keys, inventory-verified pricing and stock reservation, failure-scenario tests | Planned |
| 4 | Smart contracts: `Escrow.sol`, `AuditAnchor.sol`, Hardhat tests | Planned |
| 5 | Payment service with Web3j, escrow wired into the saga | Planned |
| 6 | Audit anchoring: Merkle batches, on-chain roots, verification endpoint | Planned |
| 7 | API gateway, notification service, dead-letter handling, load test | Planned |
| 8 | Sepolia deployment, CI, polished README and demo | Planned |

## Known gaps to close in later phases

| Gap | Planned phase |
|---|---|
| Client-supplied `unitPrice` | 3 |
| No stock check or reservation | 3 |
| Orders never leave `PENDING` | 3 |
| No authentication | 7 |
| No CI pipeline | 8 |

## Scope rule

Each phase stays within its listed scope. Nothing from a later phase is added early.
