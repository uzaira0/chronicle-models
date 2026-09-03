# ADR: Shared Contract Boundaries

Status: Accepted and implemented

Date: 2026-07-02

## Context

Chronicle has shared domain concepts duplicated across Android, iOS, web, backend, and database layers. `chronicle-models` already contains the strongest source for collection module IDs, sensor mappings, privacy classes, defaults, lifecycle/status enums, and policy values. `chronicle-api/chronicle.yaml` already drives web TypeScript API generation. iOS and web still maintain manual contract copies, and backend/database coverage is implicit in controllers, upload services, and Flyway migrations.

The refactor needs hard ownership boundaries before any code changes.

## Decision

### `chronicle-models` owns domain contracts

`chronicle-models` is the canonical owner for:

- Domain IDs such as `CollectionModuleId`, study feature IDs, consent triggers, data disposition IDs, and lifecycle/status enum values.
- Domain DTO semantics where values are not specifically HTTP-path ownership.
- Collection module metadata: active/reserved/retired state, privacy class, default enablement, sensor-module mapping, and fixture names.
- Enum stability rules, including decode-only retired values and unknown-value handling expectations.
- Payload fixture names and schema-version registry for cross-language parity.

Evidence: `CollectionModuleId.kt`, `SensorCollectionModules.kt`, `AndroidSensorType.kt`, `SensorType.kt`, `CollectionPrivacyClass.kt`, `CollectionDefaults.kt`, `CollectionDataDisposition.kt`, `ConsentTrigger.kt`, and `StudyFeature.kt`.

### `chronicle-api/chronicle.yaml` owns HTTP contracts

`chronicle-api/chronicle.yaml` is the canonical owner for:

- HTTP paths.
- Request and response wire shapes.
- Generated TypeScript API DTOs for web.
- Generated Swift API DTOs and route builders for iOS.
- OpenAPI validation and compatibility review for endpoint changes.

Evidence: `chronicle-web/package.json` already has `generate:api-types` and `check:api-types`; `chronicle-ios/chronicle/Utilities/ApiUtils.swift` currently duplicates route builders that should be generated from OpenAPI.

### Backend owns database schema and enforcement

The database schema remains backend-owned. Flyway migrations, RLS policies, idempotency handling, timestamp columns, study/participant scoping, and export eligibility are not shared directly to clients.

Backend must add contract tests that validate database and ingestion behavior against active collection module contracts from `chronicle-models`.

Evidence: backend upload services under `chronicle-server/src/main/kotlin/com/openlattice/chronicle/services/upload/`, upload endpoints in `StudyV4Controller.kt`, and Flyway migrations under `chronicle-server/src/main/resources/db/migration/`.

### Platform runtime remains native

Platform runtime concerns stay owned by each platform:

- Android: WorkManager, Room, SQLCipher, Android sensors, Android permissions, and local scheduling.
- iOS: SensorKit, Screen Time, CoreData, Apple permissions, background delivery, and native framework adapters.
- Web: UI state, presentation labels, page routes, Zod form validation at the UI boundary, and design metadata.
- Backend: ingestion services, persistence, RLS enforcement, export jobs, and operational monitoring.

Shared contracts define values and shapes. They do not replace platform runtime implementations.

## Non-Goals

- No Kotlin Multiplatform adoption in this tranche.
- No database schema sharing to clients.
- No wire-breaking renames.
- No forced rewrite of Android collection modules.
- No migration away from OpenAPI generation for web API DTOs.
- No generated UI copy, labels, or descriptions; presentation remains app-owned.
- No automatic activation of reserved or retired collection module IDs.

## Compatibility Rules

- Stable wire values must not be renamed without a compatibility adapter and explicit migration plan.
- Retired IDs remain decodable but must not appear in default active lists or enablement UI.
- Generated clients must preserve unknown-value safety for expandable server-controlled enums.
- Jackson polymorphic markers such as `@class` require fixture coverage before generation changes.
- Date, UUID, and timestamp encoding must be fixture-tested across JVM, Swift, and TypeScript.
- Backend database migrations must be forward-compatible with existing mobile app installs and queued uploads.

## Consequences

The first implementation tranches should build export/generation and test surfaces, not refactor runtime behavior. Android storage cleanup and Gradle consolidation are deferred until generated domain contracts and fixture parity are stable.
