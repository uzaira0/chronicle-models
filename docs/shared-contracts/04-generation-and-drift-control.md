# Generation and Drift Control

> Superseded design record; it describes collectors that no longer exist. Current collection contracts are described in [03-domain-contract-catalog.md](03-domain-contract-catalog.md) and [06-web-backend-db-alignment.md](06-web-backend-db-alignment.md).

This document specifies the implemented generation flow and proof gates.

## Canonical Inputs

| Input | Owner | Purpose |
| --- | --- | --- |
| `chronicle-models` domain model source | `chronicle-models` maintainers | Domain IDs, module metadata, enum stability, privacy/default policy, fixture registry. |
| `chronicle-api/chronicle.yaml` | API maintainers | HTTP paths, request/response wire shapes, API schema compatibility. |
| Backend Flyway migrations and upload services | Backend maintainers | Persistence, RLS, export, ingestion behavior validated against domain contracts. |

## Generated Outputs

| Output | Source | Destination | Consumer | Commit policy |
| --- | --- | --- | --- | --- |
| JVM contract export artifact | root generator | `generated/domain-contracts/chronicle-domain-contracts.json` (root) | Web/iOS generators, backend contract tests | Committed generated artifact; freshness checked by `root scripts/generate-chronicle-contracts.py --check`. |
| TypeScript domain constants/types | JVM contract export | `chronicle-web/src/modern/generated/chronicle-contracts.ts` | Web study configuration and operations state | Committed; content hash and Methodic freshness checked. |
| Swift generated contract target | JVM contract export plus OpenAPI | `chronicle-ios/chronicle/Generated/` | iOS DTOs, enums, contract values, route builders | Committed; `generate-ios-contracts.py --check` enforced. |
| OpenAPI generated TypeScript | `chronicle-api/chronicle.yaml` | Existing web generated API types | Web API client/types | Existing behavior remains; freshness check continues. |
| iOS route builders | `chronicle-api/chronicle.yaml` | iOS generated source target | iOS API calls replacing `ApiUtils.swift` path duplication | Generated with compatibility wrappers during migration. |

## Implemented Local Commands

Run these from the `chronicle-models` repo root:

| Command | Behavior |
| --- | --- |
| `root scripts/generate-chronicle-contracts.py` | Regenerates `generated/domain-contracts/chronicle-domain-contracts.json` from the LinkML ontology and the declared payload fixture registry. Embeds `fixtures/payloads/registry.json` as `contracts.fixtureRegistry`. JVM fixture tests validate family names, module ids, versions, and files using `FixtureRegistry`. |
| `root scripts/generate-chronicle-contracts.py --check` | Fails when the committed generated contract artifact is stale. |
| `../gradlew test --tests 'com.openlattice.chronicle.fixtures.PayloadFixtureTest'` | JVM payload fixture gates: registry validation, canonical fixture decode/encode/decode round-trip, `invalid-*` fixture rejection, and active-module coverage against the hardcoded module-to-family map. |
| `chronicle-ios/scripts/generate-ios-contracts.py [--check]` | Generates Swift domain contracts and OpenAPI route builders into `chronicle/Generated/`, syncs canonical payload fixtures into `chronicleTests/Fixtures/`, and with `--check` fails on stale committed output. Run from the `chronicle-ios` repo root. |
| `chronicle-web/bun run check:domain-contracts` | Verifies content hashes for both generated web contract modules. |

## Payload Fixture Registry

`fixtures/payloads/registry.json` (schema `chronicle-fixture-registry/v1`) is the
reviewed catalog of upload payload families. Each family records the collection
module id (or null for non-module payloads such as iOS enrollment), payload
schema version, the JVM DTO class (or null when the DTO lives outside
`chronicle-models`, e.g. Screen Time), fixture file paths, event-time semantics,
required scoping fields, and the backend handler/table target. Canonical
fixtures live under `fixtures/payloads/<family>/` as `valid.json` plus
`invalid-<reason>.json` variants; they contain only synthetic deterministic data
(fixed UUIDs, fixed `2026-07-01T12:00:00Z`-family timestamps). Valid fixtures
were produced by serializing constructed DTO instances with the shared test
mapper conventions, so field names, date encoding, and the polymorphic `@class`
discriminator match real wire output. The registry is embedded in the generated
artifact as `contracts.fixtureRegistry`, which is intentionally not part of the
LinkML mirror-key comparison.

The generated artifact exports both raw module metadata and derived safety lists:
`activeCollectionModuleIds`, `activeDefaultEnabledCollectionModuleIds`,
`inactiveCollectionModuleIds`, and `androidSensorModuleIds`. Downstream consumers
must use those lists instead of re-deriving active/default/decode-only behavior
from privacy classes alone.

## Cross-Repo Check Commands

| Command family | Required behavior |
| --- | --- |
| Generate domain contracts | Export `chronicle-models` domain contracts into a deterministic artifact. Implemented locally by `root scripts/generate-chronicle-contracts.py`. |
| Verify generated artifacts are fresh | `scripts/generate-chronicle-contracts.py --check` and `chronicle-ios/scripts/generate-ios-contracts.py --check`. |
| Verify OpenAPI is valid | `./gradlew :chronicle-api:validateOpenApiSpec`; web API types use `bun run check:api-types`. |
| Verify JVM fixtures round-trip | Decode and encode canonical fixtures using JVM model code. Implemented locally by `PayloadFixtureTest` over `fixtures/payloads/` (round-trip, negative, and coverage gates). |
| Verify Swift fixtures round-trip | Decode and encode the same fixtures using generated Swift contracts and iOS date/UUID rules. Implemented by `chronicle-ios/chronicleTests/ContractFixtureTests.swift` (xcodebuild simulator suite). |
| Verify backend ingestion contract | `./gradlew :chronicle-server:test --tests 'com.openlattice.chronicle.contract.PayloadFixtureIngestionTest'`. |
| Verify DB contract matrix | `./gradlew :chronicle-server:test --tests 'com.openlattice.chronicle.contract.CollectionModuleCoverageMatrix*'`. |

## Drift Gates

| Drift class | Gate |
| --- | --- |
| Domain value drift | Generated domain output must be fresh and deterministic. |
| OpenAPI drift | OpenAPI validation and generated API type freshness must pass. |
| Fixture drift | JVM, Swift, TypeScript, and backend fixture parity must pass. |
| DB coverage drift | Active collection module matrix must be complete for handler/table/export/RLS expectations. |
| Presentation drift | Web-owned labels/descriptions must be exhaustive for active generated module IDs. |
| Retired ID drift | Retired IDs must remain decode-only and excluded from active/default generated lists. |

## Generated File Policy

Generated files must be clearly marked with source, generation command, and manual-edit prohibition. Manual source files may wrap generated contracts but must not redefine canonical value sets.

Generated outputs must support compatibility wrappers during migration. For example, iOS may keep `ApiUtils` call sites temporarily while route construction moves behind generated route builders.

## Review Requirements

Each generator tranche must include:

- Source schema review.
- Generated diff review.
- Compatibility fixture review.
- Review-fixes pass after automated proof commands.
- Rollback path that restores the previous generated artifacts and leaves wire values unchanged.
