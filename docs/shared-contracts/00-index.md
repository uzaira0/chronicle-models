# Chronicle Common Contracts Refactor

This directory is the control plane and as-built record for the Chronicle shared-contract refactor. It defines what is shared, what stays platform-owned, how the apps and services stay in sync, and which proof gates must remain green.

Status: implementation complete as of 2026-07-12. No wire value or database migration was changed by the refactor; generated consumers, fixture coverage, boundary modules, and drift gates were added around the existing contract.

## Document Map

Read these documents in order:

| Order | Document | Owner | Purpose | Status |
| --- | --- | --- | --- | --- |
| 1 | [01-current-state-inventory.md](01-current-state-inventory.md) | Architecture | Evidence inventory and duplication matrix across Android, iOS, web, backend, and DB. | Accepted baseline |
| 2 | [02-contract-boundary-adr.md](02-contract-boundary-adr.md) | Architecture | Binding ADR for canonical owners, non-goals, and compatibility rules. | Accepted and implemented |
| 3 | [03-domain-contract-catalog.md](03-domain-contract-catalog.md) | `chronicle-models` maintainers | Catalog of shared domain concepts and canonical ownership. | Implemented |
| 4 | [04-generation-and-drift-control.md](04-generation-and-drift-control.md) | Build/tooling maintainers | Generated outputs and freshness checks. | Implemented |
| 5 | [05-mobile-refactor-plan.md](05-mobile-refactor-plan.md) | Android and iOS maintainers | Mobile migration and app-install compatibility guards. | Implemented |
| 6 | [06-web-backend-db-alignment.md](06-web-backend-db-alignment.md) | Web and backend maintainers | Web generation alignment plus backend/DB contract matrices. | Implemented |
| 7 | [07-proof-and-testing-matrix.md](07-proof-and-testing-matrix.md) | QA and platform owners | Required proof matrix for every shared-contract tranche. | Passing gate |
| 8 | [08-rollout-sequence.md](08-rollout-sequence.md) | Release owners | Completed tranche sequence with proof commands and rollback paths. | Complete |
| 9 | [09-risk-ledger.md](09-risk-ledger.md) | Architecture and release owners | Risk ledger with owners, mitigations, proof, and stop conditions. | Mitigated and monitored |

## Control Rules

- `chronicle-models` is the canonical repo for shared domain contracts: domain IDs, DTOs, settings, collection module metadata, privacy classes, fixture names, and enum stability.
- `chronicle-api/chronicle.yaml` is the canonical source for HTTP paths and request/response wire shapes.
- Backend database schema remains backend-owned. It is validated against shared collection contracts but is not exported to clients as a shared schema.
- Android, iOS, and web runtime behavior stays platform-owned.
- Generated consumers must tolerate unknown values unless the contract explicitly says a value is impossible.
- Retired IDs remain decode-only and must not be reactivated by generated code or UI defaults.
- No implementation tranche may start until its entry criteria, proof commands, review-fixes gate, and rollback path are documented in [08-rollout-sequence.md](08-rollout-sequence.md).

## Source Repositories

| Repository | Role in this refactor |
| --- | --- |
| `chronicle-models` | Shared JVM/domain model source, fixtures, and this document package. |
| `chronicle-api` | OpenAPI source of HTTP paths and request/response shapes. |
| `chronicle` | Android app and Android collection modules. |
| `chronicle-ios` | iOS app consuming generated domain contracts, route builders, and wire DTOs. |
| `chronicle-web` | Web app consuming generated OpenAPI/domain contracts; generated iOS payload validators are parity/freshness test artifacts because the web app does not submit mobile payloads. |
| `chronicle-server` | Backend upload handlers, Flyway migrations, RLS policies, and export behavior. |

## Acceptance Checklist

- All ten Markdown documents exist under `chronicle-models/docs/shared-contracts/`.
- Every shared concept has one canonical owner and named consumers.
- Every implementation tranche lists entry criteria, exit criteria, proof commands, review-fixes gate, and rollback path.
- The proof matrix covers Android, iOS, web, backend, and DB.
- Compatibility handling is required before any wire value, persisted ID, generated type, or database behavior changes.
- Generated Swift DTOs and web domain/API types are in active runtime use and freshness-gated; web mobile-payload validators are explicitly parity-test-only.
- Android contract code is isolated from Room/SQLCipher/WorkManager storage concerns.
- Backend fixture ingestion and DB/RLS matrix tests pass against PostgreSQL.
