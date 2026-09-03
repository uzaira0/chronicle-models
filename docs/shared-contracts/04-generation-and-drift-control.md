# Generation and Drift Control

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
| JVM contract export artifact | `chronicle-models` | `generated/domain-contracts/chronicle-domain-contracts.json` | Web/iOS generators, backend contract tests | Committed generated artifact; freshness checked by `scripts/generate-domain-contracts.py --check`. |
| TypeScript domain constants/types | JVM contract export | `chronicle-web/src/modern/generated/chronicle-contracts.ts` | Web study configuration and operations state | Committed; content hash and Methodic freshness checked. |
| TypeScript payload validators/fixtures | OpenAPI plus fixture registry | `chronicle-web/src/modern/generated/chronicle-payload-contracts.generated.ts` | Cross-language parity/freshness tests for the four iOS wire families; not a web runtime boundary | Committed; content hash and Methodic freshness checked. |
| Swift generated contract target | JVM contract export plus OpenAPI | `chronicle-ios/chronicle/Generated/` | iOS DTOs, enums, contract values, route builders | Committed; `generate-ios-contracts.py --check` enforced. |
| OpenAPI generated TypeScript | `chronicle-api/chronicle.yaml` | Existing web generated API types | Web API client/types | Existing behavior remains; freshness check continues. |
| iOS route builders | `chronicle-api/chronicle.yaml` | iOS generated source target | iOS API calls replacing `ApiUtils.swift` path duplication | Generated with compatibility wrappers during migration. |

## Implemented Local Commands

Run these from the `chronicle-models` repo root:

| Command | Behavior |
| --- | --- |
| `scripts/generate-domain-contracts.py` | Regenerates `generated/domain-contracts/chronicle-domain-contracts.json` from the Kotlin and Java model sources plus the payload fixture registry. Validates `fixtures/payloads/registry.json` (unique kebab-case family names, known module ids, schema versions, fixture files on disk) and embeds it as `contracts.fixtureRegistry`; a malformed registry fails generation. |
| `scripts/generate-domain-contracts.py --check` | Fails when the committed generated contract artifact is stale. |
| `scripts/verify-domain-contract-fixtures.py` | Fails when `fixtures/domain-contracts/domain-contract-fixture.json` is stale relative to the generated contract artifact. |
| `scripts/verify-domain-contract-fixtures.py --update` | Rewrites the reviewed fixture projection after an intentional domain contract change. |
| `scripts/check-domain-contracts.sh` | Runs the generated-contract freshness check and fixture projection parity check together. |
| `../gradlew test --tests 'com.openlattice.chronicle.fixtures.PayloadFixtureTest'` | JVM payload fixture gates: registry validation, canonical fixture decode/encode/decode round-trip, `invalid-*` fixture rejection, and active-module coverage against the hardcoded module-to-family map. |
| `chronicle-ios/scripts/generate-ios-contracts.py [--check]` | Generates Swift domain contracts and OpenAPI route builders into `chronicle/Generated/`, syncs canonical payload fixtures into `chronicleTests/Fixtures/`, and with `--check` fails on stale committed output. Run from the `chronicle-ios` repo root. |
| `methodic/scripts/generate-chronicle-payload-contracts.py [--check]` | Generates OpenAPI-derived Zod wire validators and embeds every canonical payload fixture for TypeScript registry-corpus parity; duplicate fixture paths fail generation. Run from the Methodic root. |
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
| Generate domain contracts | Export `chronicle-models` domain contracts into a deterministic artifact. Implemented locally by `scripts/generate-domain-contracts.py`. |
| Verify generated artifacts are fresh | `scripts/generate-chronicle-contracts.py --check`, `scripts/generate-chronicle-payload-contracts.py --check`, `chronicle-models/scripts/check-domain-contracts.sh`, and `chronicle-ios/scripts/generate-ios-contracts.py --check`. |
| Verify OpenAPI is valid | `./gradlew :chronicle-api:validateOpenApiSpec`; web API types use `bun run check:api-types`. |
| Verify JVM fixtures round-trip | Decode and encode canonical fixtures using JVM model code. Implemented locally by `PayloadFixtureTest` over `fixtures/payloads/` (round-trip, negative, and coverage gates) plus the projection parity check in `scripts/verify-domain-contract-fixtures.py`. |
| Verify Swift fixtures round-trip | Decode and encode the same fixtures using generated Swift contracts and iOS date/UUID rules. Implemented by `chronicle-ios/chronicleTests/ContractFixtureTests.swift` (xcodebuild simulator suite). |
| Verify TypeScript fixture parity and supported wire validation | `bun test src/modern/state/payload-contracts.test.ts` in `chronicle-web` verifies registry-corpus parity and Zod validation for the four explicitly supported iOS wire families. |
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
