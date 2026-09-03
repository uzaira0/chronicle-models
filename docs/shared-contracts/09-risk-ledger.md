# Risk Ledger

Each risk has an owner, mitigation, proof, and stop condition. Stop conditions block the tranche until resolved.

## R1: Jackson `@class` Polymorphism Breakage

Owner: Backend and iOS owners.

Risk: `chronicle-ios/chronicle/Models/IOSDevice.swift` and other payloads use Jackson-style `@class` values. Generated DTOs can omit, rename, or encode these values differently.

Mitigation: Keep `@class` values explicit in fixture contracts and generated DTO tests. Do not rename polymorphic markers without a backend compatibility adapter.

Proof: JVM, Swift, and backend fixture round-trip tests include `@class` payloads. TypeScript verifies registry-corpus parity and Zod behavior for the four explicitly supported iOS wire families; `ios-device` remains covered by generated corpus parity rather than a TypeScript round-trip. JVM `PayloadFixtureTest` and Swift `ContractFixtureTests` pin the `@class` string `com.openlattice.chronicle.sources.IOSDevice` byte-exactly against the canonical `ios-device` fixture, and the Swift test asserts decode failure when it is missing.

Stop condition: Any generated payload changes a `@class` value, omits it where backend expects it, or makes backend decode ambiguous.

## R2: Swift Date/UUID Encoding Drift

Owner: iOS owner.

Risk: `ScreenTimeUsagePayload.swift` encodes dates and UUIDs in Swift-specific ways. Generated DTOs can change precision, timezone, optionality, or casing.

Mitigation: Add canonical fixtures for dates, UUIDs, nil values, and boundary timestamps before replacing DTOs.

Proof: Swift fixture round-trip output must byte-match or semantically match the JVM-approved canonical fixture rules. Implemented by `ContractFixtureTests` (chronicle-ios): ISO-8601 renderings are byte-exact against canonical fixtures (second precision, `Z`), and UUID-shaped strings compare case-insensitively because Swift re-encodes uppercase while the live app already emits uppercase UUIDs on the wire.

Stop condition: Backend ingestion differs between old Swift DTO output and generated Swift DTO output for the same fixture.

## R3: Web Generated/Manual Type Overlap

Owner: Web owner.

Risk: `study-constants.ts`, `study-operations-api.ts`, and `zod-schemas.ts` can keep manual copies after generated types exist, creating two sources of truth.

Mitigation: Generated domain values replace canonical unions/enums; web keeps only labels, descriptions, layout, and UI validation messages with exhaustive generated coverage checks.

Proof: Web typecheck, generated content/freshness checks, exhaustive active-module presentation tests, and canonical valid/invalid payload fixture tests.

Stop condition: A canonical value set exists in both generated code and manual web code without a documented compatibility wrapper.

## R4: Android Room/Storage Coupling

Owner: Android owner.

Risk: `collection-base` currently combines `chronicle-models` consumption with Room/SQLCipher storage concerns. Contract cleanup can accidentally become a storage migration.

Mitigation: Keep pure contract consumption in `collection-contracts`; keep Room/SQLCipher/queued persistence in `collection-base`; enforce both module source and shared Gradle convention dependencies in `ContractBoundaryTest`.

Proof: `collection-contracts:testDebugUnitTest` boundary checks and the research app unit/build suite. No Room entity, schema, migration, or SQLCipher behavior changed.

Stop condition: A contract-only tranche changes Room entities, SQLCipher behavior, or local queued upload persistence.

## R5: Retired Module IDs Accidentally Reactivated

Owner: `chronicle-models` owner.

Risk: Generated active lists may include retired IDs from `CollectionModuleId.retiredIds`, making them selectable in UI or expected in backend active coverage.

Mitigation: Export all-known, active, reserved, and retired lists separately. Add tests that retired IDs are decode-only and absent from defaults.

Proof: Domain export unit tests and web/iOS generated active-list tests.

Stop condition: A retired ID appears in active generated lists, default enablement, web selectors, or backend active-module requirements without an ADR.

## R6: DB RLS/Export Mismatch

Owner: Backend and DB owners.

Risk: Active modules can have upload handlers and tables but lack RLS policy, export eligibility, idempotency behavior, or correct study/participant scoping.

Mitigation: Maintain module-to-handler/table/export/RLS matrix and enforce it in backend contract tests.

Proof: DB migration/RLS matrix tests and backend fixture ingestion tests.

Stop condition: Any active module lacks required RLS/export/scoping/idempotency coverage and has no approved exception.

## R7: Generated Artifacts Committed Stale

Owner: Build/tooling owner.

Risk: Generated TypeScript, Swift, or domain export artifacts can be committed stale relative to `chronicle-models` or `chronicle-api/chronicle.yaml`.

Mitigation: Add deterministic generation and freshness checks to CI. Generated files must include source and command headers.

Proof: Methodic `domain-contract-guardrails.sh`, iOS generator `--check`, web content hashes, OpenAPI type generation diff, and deterministic payload fixture generation all fail on stale output.

Stop condition: CI allows stale generated artifacts or cannot reproduce generated outputs deterministically.

## R8: Wire-Breaking Rename During Cleanup

Owner: Architecture and API owners.

Risk: While deduplicating values, a cleanup may rename a persisted ID, enum value, endpoint segment, or payload field.

Mitigation: Treat all existing wire values as stable. Any rename requires ADR update, compatibility adapter, fixture proof, migration plan, rollout plan, and rollback path.

Proof: OpenAPI drift checks, fixture parity, backend ingestion compatibility, and old-client payload tests.

Stop condition: A rename changes wire output or accepted input without compatibility handling.
