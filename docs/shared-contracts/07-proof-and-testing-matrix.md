# Proof and Testing Matrix

This matrix defines the proof obligations for the shared-contract refactor. A tranche is not complete until its row obligations pass or are explicitly marked not applicable with owner approval.

## Matrix

| Surface | Unit | Invalid input | Integration | Contract drift | Cross-language fixture parity | Persistence/idempotency | Privacy/security | Migration/rollback | Architecture boundaries | Health/smoke |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Domain contract export | Verify `CollectionModuleId`, sensor mappings, privacy, defaults, status enums, consent, and disposition are exported deterministically. | Reject duplicate IDs, incomplete sensor mappings, retired IDs in active/default lists, and malformed fixture registry rows. | Export artifact consumed by sample JVM test. | Freshness check fails on unstaged/generated differences. | JVM fixture decode/encode is canonical baseline. | Not applicable except fixture registry stability. | Privacy class and active-default enablement included. | Export format version documented with rollback to previous artifact. | No Android/iOS/web runtime imports into export logic. | Command runs in CI and local dev. |
| OpenAPI drift | Validate `chronicle-api/chronicle.yaml`. | Invalid schema, missing path params, incompatible enum changes fail. | Generated web and iOS API DTO sample compiles. | OpenAPI generated output freshness check. | API fixtures align with domain fixtures when payloads overlap. | Not applicable directly. | Auth/security requirements remain declared. | Wire-breaking changes require ADR and compatibility adapter. | OpenAPI owns paths, not domain policy. | API validation command in CI. |
| Web generation | Generated domain constants/types compile. | Unknown enum and invalid module cases tested in Zod/UI boundary. | Web study creation/configuration uses generated values where migrated. | `check:api-types` plus domain generation freshness. | TypeScript fixture validation matches JVM baseline. | Not applicable directly. | UI honors privacy/default metadata; labels exhaustive. | Rollback restores prior manual constants or previous generated output. | Web owns presentation only. | Web typecheck and targeted tests pass. |
| iOS generation | Generated Swift contracts compile. | Unknown values, bad UUIDs, and date parse failures tested. | Route builders and DTOs used behind compatibility wrappers. | Swift generated freshness check. | Swift fixture decode/encode matches JVM and TypeScript. | Queued upload payloads remain decodable. | Sensitive payload defaults and consent values match domain contracts. | Rollback returns to previous generated target/wrappers with wire values unchanged. | Apple framework adapters remain hand-written. | iOS build and targeted contract tests pass. |
| Android model consumption | Existing Gradle composite model consumption compiles. | Invalid module IDs handled through existing model APIs such as `fromIdOrNull`. | Collection modules map to active IDs and upload families. | JVM dependency lock/generation checks detect drift. | Android/JVM uses canonical fixture baseline. | Room/SQLCipher data remains readable. | Privacy/default settings match `chronicle-models`. | No storage migration during generation tranches; later storage changes require Room migration proof. | Android runtime storage remains outside pure contract export. | Android unit/build smoke pass. |
| Backend ingestion | Upload handlers accept canonical fixtures. | Malformed, duplicate, unknown, and missing-scope payloads reject correctly. | Controller to service to DB ingestion tested. | Active module matrix coverage check. | Backend accepts JVM/Swift/TypeScript fixture outputs. | Idempotency keys and duplicate behavior tested. | Auth, scoping, RLS, and export eligibility verified. | Rollback keeps old payload ingestion path. | Backend owns persistence, not client-generated DB schema. | API smoke test for upload endpoints. |
| DB migrations/RLS | Migration unit checks for expected tables/columns. | Invalid migration state fails early. | Flyway applies from empty DB and representative existing DB. | Matrix compares active modules to tables/RLS/export expectations. | Fixture-ingested rows match expected table shape. | Idempotency columns/constraints verified. | RLS policies and scoped access verified. | Operational rollback or forward-fix plan documented. | DB schema not exported to clients. | Migration smoke in CI/test DB. |
| Payload fixtures | Fixture registry validates. | Fixtures include missing/unknown/bad-date/bad-UUID cases. | Fixtures run through JVM, Swift, TypeScript, and backend. | Fixture schema version change requires review. | Required parity axis for all generated consumers. | Duplicate fixture ingestion tested. | Sensitive fixture handling avoids real personal data and verifies policy fields. | Old fixture versions remain accepted or have migration adapter. | Fixtures test contracts, not platform runtime internals. | Fixture suite runs locally and in CI. |
| Rollout compatibility | Tranche-specific compatibility tests pass. | Old app payloads, unknown enum values, and stale generated clients tested. | End-to-end smoke from generated client to backend where applicable. | Freshness checks block stale committed artifacts. | Cross-language parity blocks rollout. | Existing local/queued mobile data remains ingestible. | Privacy/security review passes for generated data surfaces. | Rollback command/path verified before merge. | Ownership boundaries rechecked in review. | Post-merge smoke and monitoring checklist complete. |

## Required Command Families

Implemented commands are:

- Domain contract generation. Local command: `scripts/generate-domain-contracts.py` (also validates and embeds `fixtures/payloads/registry.json`).
- Domain generated freshness check. Local command: `scripts/generate-domain-contracts.py --check`.
- OpenAPI validation: `./gradlew :chronicle-api:validateOpenApiSpec` from Methodic.
- Web typecheck and generated freshness: `bun run check:domain-contracts`, `bun run check:api-types`, `bun run typecheck`.
- Swift contract build/test: `python3 scripts/generate-ios-contracts.py --check` plus the `ContractFixtureTests` and `CoreDataTests` xcodebuild suite.
- JVM fixture round-trip. Local commands: `scripts/verify-domain-contract-fixtures.py` for the projection parity check and `../gradlew test --tests 'com.openlattice.chronicle.fixtures.PayloadFixtureTest'` for registry validation, canonical payload round-trip, `invalid-*` rejection, and active-module coverage over `fixtures/payloads/`.
- Swift fixture round-trip.
- TypeScript fixture parity and supported wire validation: `bun test src/modern/state/payload-contracts.test.ts` verifies registry-corpus parity and Zod validation for the four explicitly supported iOS wire families.
- Backend ingestion and DB migration/RLS: `./gradlew :chronicle-server:test --tests 'com.openlattice.chronicle.contract.*'`.
- Android boundary/app proof: `./gradlew :collection-contracts:testDebugUnitTest :app:testResearchDebugUnitTest` with JDK 21 and the Android SDK configured.

The local aggregate command for the implemented `chronicle-models` gates is `scripts/check-domain-contracts.sh`.

## Review-Fixes Gate

After proof commands pass, every tranche must run a review-fixes pass that checks:

- Owner boundaries.
- Generated/manual overlap.
- Compatibility adapters.
- Retired ID handling.
- Unknown value behavior.
- Fixture parity.
- Rollback instructions.
- No accidental code changes outside tranche scope.
