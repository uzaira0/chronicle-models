# Web, Backend, and DB Alignment

This document aligns generated domain/API types with web behavior, backend ingestion, and database ownership.

## Web

### Current evidence

- `chronicle-web/package.json` already defines OpenAPI generation scripts such as `generate:api-types` and `check:api-types`.
- `chronicle-web/src/modern/lib/study-constants.ts` manually defines study modules, collection module IDs, descriptors, default enablement, and sensor ordering.
- `chronicle-web/src/modern/state/study-operations-api.ts` manually defines API-adjacent operation types, consent/status values, settings types, and wrappers.
- `chronicle-web/src/modern/state/zod-schemas.ts` duplicates `ParticipationStatus` validation.

### Target boundary

Generated web-owned inputs:

- OpenAPI TypeScript from `chronicle-api/chronicle.yaml`.
- Domain constants and types from the `chronicle-models` contract export.

Web-owned presentation metadata:

- Labels.
- Descriptions.
- Sort/grouping choices for UI.
- Form layout.
- Help text.
- UI-specific validation messages.

### Web checks

- Manual `CollectionModuleId` unions must be replaced by generated domain values where safe.
- Web labels/descriptions must have exhaustive coverage for active generated module IDs.
- Zod schemas must be generated from or checked against canonical enum values.
- UI defaults must consume generated default enablement and privacy metadata.
- OpenAPI generated API types remain sourced from `chronicle-api/chronicle.yaml`.

Status: complete. `chronicle-contracts.ts` supplies domain values and `scripts/check-domain-contracts.ts` verifies its generated content hash. Labels and descriptions remain web-owned with exhaustive active-module tests.

## Backend

### Current evidence

- `chronicle-server/src/main/kotlin/com/openlattice/chronicle/controllers/StudyV4Controller.kt` exposes module upload endpoints and delegates to `StudyController`, which also directly ingests Android availability snapshots (UPSERT) and delegates iOS Screen Time to its dedicated upload owner; questionnaire ingestion goes through `SurveyController`/`SurveysService`.
- `chronicle-server/src/main/kotlin/com/openlattice/chronicle/services/upload/*UploadService.kt` owns ingestion behavior for collection streams.
- `chronicle-server/src/main/resources/db/migration/` owns Flyway migrations for collection tables and RLS policy setup.

### Required module contract matrix

Backend must maintain a matrix for every active `CollectionModuleId`:

| Field | Required meaning |
| --- | --- |
| Module ID | Stable ID from `chronicle-models`. |
| Active state | Active or reserved state from `chronicle-models`. |
| Upload endpoint | HTTP endpoint from `chronicle-api/chronicle.yaml` when applicable. |
| Upload handler | Backend service or controller method that ingests the module payload. |
| Database table | Backend-owned table or explicit no-table reason. |
| Idempotency key | Constraint-backed idempotency or explicitly documented logical dedup, verified by the matrix’s DB tests. Usage events use the client event ID index plus a duplicate sweep; Screen Time uses a NULL-safe capture key. |
| Study scope | Study UUID or equivalent scoping column. |
| Participant scope | Participant/user/device scoping column. |
| Timestamp column | Event time and ingestion time expectations. |
| RLS policy | Expected policy or explicit not-applicable reason. |
| Export eligibility | Whether module data is included in exports and under which data disposition rules. |
| Fixture family | Fixture file family used for ingestion and round-trip tests. |

Shared-row conventions established by the implemented matrix: `in_app_activity_class` and `device_lifecycle` are documented shared rows of `chronicle_usage_events` (activity-class column and lifecycle event types respectively), and `upload_telemetry` is the canonical no-upload exception entry (device-local diagnostics; no endpoint, table, RLS, or export row).

### Backend checks

- Every active module has an ingestion/storage/export/RLS row or an explicit documented exception.
- Every upload handler accepts canonical fixtures and rejects malformed fixtures.
- Only active modules have ingestion handlers; removed collector IDs are absent from generated contracts.
- Backend contract tests compare active `CollectionModuleId` values against handler/table/export/RLS matrix coverage.
- Implemented: `chronicle-server/src/test/kotlin/com/openlattice/chronicle/contract/CollectionModuleCoverageMatrixTest.kt` (static completeness, handler wiring, and export-lane checks; the companion DB test runs the full `db/migration` corpus on Testcontainers Postgres 16 and asserts tables, scoping/timestamp columns, idempotency constraints via `pg_constraint`, and RLS enable/force/policy via `pg_policies`).
- Implemented: `PayloadFixtureIngestionTest.kt` drives canonical valid and malformed fixtures through production upload services, including Screen Time captures stored in `ios_screen_time_usage` (one row per capture key), and proves idempotent re-ingestion.

## Database

### Current evidence

Flyway migrations define backend-owned collection tables including:

- `V24__add_battery_telemetry.sql`
- `V31__add_interaction_events.sql`
- `V32__add_interaction_exact_position.sql`
- `V34__add_app_audio_activity.sql`
- `V35__add_app_audio_content.sql`
- `V36__add_notification_activity.sql`
- `V38__add_sleep_events.sql`
- `V39__add_activity_recognition_events.sql`
- `V40__add_health_metrics.sql`
- `V41__add_connectivity_state_events.sql`
- `V42__add_app_network_usage.sql`
- `V43__add_device_settings.sql`
- `V45__add_device_settings_audio_brightness.sql`
- `V1__enable_row_level_security.sql` baseline RLS setup.

### DB ownership rules

The DB remains backend-owned. Clients consume API/domain contracts, not database schema.

Each module table must document:

- Owning backend service.
- RLS policy expectation.
- Idempotency key.
- Event timestamp column.
- Ingestion timestamp column.
- Study scoping column.
- Participant, user, or device scoping column.
- Export eligibility.
- Retention or deletion disposition behavior where applicable.
- Migration/rollback constraints.

### DB proof obligations

- Flyway migrations apply from empty database.
- RLS policies exist for tables that expose participant/study-scoped data.
- Idempotency behavior rejects duplicates or preserves deterministic upsert semantics.
- Export queries include every stored dataset in `DataDownloadService.COLLECTION_EXPORTS` plus usage events, Android data, app-usage surveys and upload diagnostics. `IOSScreenTime` exports the newest capture per app/domain window from `ios_screen_time_usage`; dedicated Android collection datasets and availability reports have their own export lanes. `CollectionModuleCoverageMatrixTest` derives eligibility from the complete lane map. See the [researcher data dictionary](../../../docs/PILOT-DATA.md#ios-screen-time) for Screen Time columns.
- Migration rollback guidance exists for each tranche even if Flyway rollback is operational rather than automated.
