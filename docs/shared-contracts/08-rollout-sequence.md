# Rollout Sequence

Status: all eight tranches complete as of 2026-07-12. Each tranche retains its entry criteria, exit criteria, proof commands, review-fixes gate, and rollback path as an operational regression checklist.

## Tranche 1: Inventory and Contract Freeze

Entry criteria:

- [01-current-state-inventory.md](01-current-state-inventory.md) is accepted by Android, iOS, web, backend, and DB owners.
- [02-contract-boundary-adr.md](02-contract-boundary-adr.md) ownership decisions are accepted.

Work:

- Freeze current `chronicle-models` domain value set.
- Create backend module-to-handler/table/export/RLS matrix draft.
- Identify all web/iOS manual duplicates to be replaced later.

Exit criteria:

- No ambiguous canonical owners remain for listed shared concepts.
- Retired/reserved IDs are explicitly documented.

Proof commands:

- Documentation validation for ten docs.
- Source-reference review against current repo paths.

Review-fixes gate:

- Review findings about missing owners, missing source evidence, or compatibility gaps must be resolved before code generation begins.

Rollback path:

- Revert only documentation changes from this tranche; no code or wire behavior changes exist.

## Tranche 2: Generated Domain Contract Artifact

Entry criteria:

- Tranche 1 complete.
- Export schema design reviewed by JVM, web, iOS, and backend owners.

Work:

- Add deterministic export from `chronicle-models` for domain contracts.
- Include module IDs, states, privacy/defaults, sensor mappings, policy enums, and fixture registry.
- Current local artifact: `generated/domain-contracts/chronicle-domain-contracts.json`.
- Current local generator: `scripts/generate-domain-contracts.py`.

Exit criteria:

- Export is deterministic and versioned.
- Retired IDs are decode-only in artifact semantics.

Proof commands:

- Generate domain contracts with `scripts/generate-domain-contracts.py`.
- Verify generated artifact freshness with `scripts/generate-domain-contracts.py --check`.
- JVM unit tests for duplicate IDs, retired active defaults, and required metadata.

Review-fixes gate:

- Generated schema diff, unknown-value behavior, and retired/reserved handling must pass review with no unresolved blocking findings.

Rollback path:

- Remove export task/artifact and leave existing `chronicle-models` runtime APIs unchanged.

## Tranche 3: Fixture Parity

Entry criteria:

- Tranche 2 complete.
- Fixture registry names and schema-version rules documented.

Work:

- Add canonical payload fixtures for JVM, Swift, TypeScript, and backend ingestion.
- Cover Screen Time, SensorKit, Android sensors, device settings, and representative collection tables.
- Current local fixture registry: `fixtures/payloads/registry.json` (17 families), validated and embedded as `contracts.fixtureRegistry` by `scripts/generate-domain-contracts.py`.
- Current local canonical fixtures: `fixtures/payloads/<family>/valid.json` plus `invalid-<reason>.json` variants, synthetic deterministic data only.
- Current local JVM gates: `src/test/kotlin/com/openlattice/chronicle/fixtures/PayloadFixtureTest.kt` with registry validation factored into `FixtureRegistry.parse`.
- Current local fixture projection: `fixtures/domain-contracts/domain-contract-fixture.json`.
- Current local fixture projection gate: `scripts/verify-domain-contract-fixtures.py`.

Exit criteria:

- Fixtures round-trip on JVM baseline.
- Fixture registry rejects malformed entries.

Proof commands:

- Verify local fixture projection parity with `scripts/verify-domain-contract-fixtures.py`.
- Verify JVM fixtures round-trip with `../gradlew test --tests 'com.openlattice.chronicle.fixtures.PayloadFixtureTest'`.
- Verify malformed fixtures fail (same test class, `invalid-*` fixtures and inline malformed registry entries).

Review-fixes gate:

- Fixture contents, synthetic-data safety, date/UUID encoding, and `@class` handling must pass review.

Rollback path:

- Remove fixture suite and registry additions; no generated consumer migration proceeds.

## Tranche 4: Web Generated Constants and Types

Entry criteria:

- Tranche 2 complete.
- Web owners accept generated/manual replacement boundary.

Work:

- Generate web domain constants/types from the domain contract artifact.
- Replace safe duplicates from `study-constants.ts`, `study-operations-api.ts`, and `zod-schemas.ts`.
- Keep UI labels/descriptions web-owned with exhaustive coverage checks.

Current implementation: `chronicle-web/src/modern/generated/chronicle-contracts.ts`, `chronicle-payload-contracts.generated.ts`, generated OpenAPI types, `scripts/check-domain-contracts.ts`, and `state/payload-contracts.test.ts`.

Exit criteria:

- Web no longer hand-maintains canonical module/status/policy value sets in migrated areas.
- UI presentation metadata has exhaustive active-module coverage.

Proof commands:

- Generate contracts.
- Verify generated artifacts are fresh.
- Verify OpenAPI is valid.
- Web typecheck and targeted tests.
- TypeScript registry-corpus parity and Zod validation for the four explicitly supported iOS wire families.

Review-fixes gate:

- Review generated/manual overlap, presentation ownership, unknown enum behavior, and defaults/privacy consumption.

Rollback path:

- Restore previous manual web constants or previous generated output without changing backend/API wire values.

## Tranche 5: iOS Generated Contracts and Route Builders

Entry criteria:

- Tranche 2 and Tranche 3 complete.
- iOS owners accept generated target layout and compatibility wrappers.

Current local implementation: generator `chronicle-ios/scripts/generate-ios-contracts.py` (with `--check` freshness gate), generated `chronicle/Generated/ChronicleDomainContracts.swift` and `chronicle/Generated/ChronicleRoutes.swift`, ApiUtils compatibility wrappers, Sensor adapter over generated contract values, canonical fixture sync into `chronicleTests/Fixtures/`, and `chronicleTests/ContractFixtureTests.swift` route-equivalence and fixture round-trip tests.

Work:

- Generate Swift domain constants and DTOs.
- Generate route builders from `chronicle-api/chronicle.yaml`.
- Replace `ApiUtils.swift` path construction behind wrappers.
- Move base URL configuration out of hard-coded static constants.
- Replace local SensorKit contract enums while keeping native adapters hand-written.

Exit criteria:

- iOS generated contracts compile.
- Existing API call sites continue through compatibility wrappers.
- Fixture parity proves no payload wire drift.

Proof commands:

- Generate contracts.
- Verify generated artifacts are fresh.
- Verify OpenAPI is valid.
- Swift build/tests.
- Swift fixture round-trip.
- Backend ingestion of Swift fixture outputs.

Review-fixes gate:

- Review route compatibility, base URL configuration behavior, `@class`, date/UUID encoding, and queued upload compatibility.

Rollback path:

- Revert generated Swift target usage and wrappers to previous manual DTO/path code while preserving wire values.

## Tranche 6: Backend DB Contract Tests

Entry criteria:

- Tranche 1 and Tranche 3 complete.
- Backend module-to-handler/table/export/RLS matrix approved.

Current local implementation: `chronicle-server/src/test/kotlin/com/openlattice/chronicle/contract/CollectionModuleCoverageMatrixTest.kt` (static completeness, handler wiring, export lanes, plus a Testcontainers DB test applying all migrations and asserting tables, scoping/timestamp columns, idempotency constraints, and RLS enable/force/policy) and `PayloadFixtureIngestionTest.kt` (canonical payload fixtures ingested through the real upload services with idempotent re-ingest and malformed-fixture rejection).

Work:

- Add tests comparing active collection modules to backend ingestion/storage/export/RLS coverage.
- Add fixture ingestion tests for representative modules.
- Verify Flyway migrations and RLS expectations.

Exit criteria:

- Every active module has handler/table/export/RLS coverage or documented exception.
- Fixtures ingest and persist with expected idempotency.

Proof commands:

- Backend ingestion fixture tests.
- DB migration/RLS matrix tests.
- OpenAPI validation for upload paths.

Review-fixes gate:

- Review scoping, RLS, export eligibility, idempotency, and active-module exceptions before merge.

Rollback path:

- Remove tests/matrix additions only; do not remove existing tables, handlers, or migrations.

## Tranche 7: Android Storage/Contract Boundary Cleanup

Entry criteria:

- Tranche 2 and Tranche 6 complete.
- Android owners approve storage/contract split scope.

Current local implementation: new `chronicle/collection-contracts` Android library holds the pure contract-consumption layer (constants, upload DTO, sensor contract interfaces and mapping, settings/policy preferences); `collection-base` re-exports it via an api dependency so downstream modules are unchanged; `ContractBoundaryTest` asserts the contract layer carries no Room/SQLCipher/WorkManager/storage imports; Room entities, migrations, SQLCipher behavior, and queued-upload persistence are byte-identical.

Work:

- Separate pure collection contract consumption from Android-only Room/SQLCipher storage concerns in `collection-base`.
- Preserve existing module dependency behavior.

Exit criteria:

- Contract-facing code does not require Android storage imports.
- Android app behavior and queued uploads are unchanged.

Proof commands:

- Android build/unit tests.
- JVM contract tests.
- Room migration tests where storage boundaries change.
- Backend ingestion of Android fixture outputs.

Review-fixes gate:

- Review module boundaries, dependency graph, storage migration impact, and queued upload compatibility.

Rollback path:

- Revert Android storage-boundary changes; generated contract artifact remains usable.

## Tranche 8: Gradle and Dependency Consolidation

Entry criteria:

- Tranche 7 complete.
- No active generated-contract or storage-boundary instability.

Current implementation: `chronicle/gradle/collection-library.gradle` centralizes the common repository, SDK, JVM, lint, desugaring, and baseline test setup for all `collection-*` Android libraries. Module namespaces and feature dependencies remain local, and composite-build behavior is unchanged.

Work:

- Consolidate repeated Gradle setup.
- Keep composite build behavior for `chronicle-models` and `chronicle-api` intact unless a separate ADR changes it.

Exit criteria:

- Dependency graph is simpler without changing runtime or wire behavior.
- Build reproducibility is preserved.

Proof commands:

- Android full build.
- Dependency lock/freshness checks.
- Contract generation/freshness checks.

Review-fixes gate:

- Review build reproducibility, dependency locks, and no accidental generated artifact drift.

Rollback path:

- Revert Gradle consolidation commit independently from contract-generation and runtime refactor commits.
