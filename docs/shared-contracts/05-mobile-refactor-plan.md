# Mobile Refactor Plan

This plan keeps mobile runtime behavior native while moving duplicated contract values to generated or shared sources.

## Android

### Current evidence

- `chronicle/settings.gradle` includes composite builds for `chronicle-api` and `chronicle-models`.
- `chronicle/collection-base/build.gradle` depends on `chronicle-models` and also on Android storage/runtime libraries such as Room and SQLCipher.
- Android collection modules already align with many `CollectionModuleId` values and backend upload tables.

### Target boundary

Android keeps `chronicle-models` as the shared model dependency. The refactor does not replace this with generated TypeScript/Swift-style artifacts.

Android platform-owned code remains:

- WorkManager scheduling.
- Android sensor adapters.
- Room and SQLCipher persistence.
- Android permissions and foreground/background runtime logic.
- Existing collection module Gradle module boundaries.

Shared contract ownership remains:

- `CollectionModuleId`, `AndroidSensorType`, `SensorCollectionModules`, defaults, privacy classes, consent triggers, and data disposition in `chronicle-models`.
- HTTP API paths and request/response wire shapes in `chronicle-api/chronicle.yaml`.

### Android migration order

1. Freeze current domain contract inventory from `chronicle-models`.
2. Add deterministic domain export proof without changing Android runtime code.
3. Add Android tests that prove existing collection modules consume active `CollectionModuleId` values correctly.
4. Add backend/DB matrix tests before changing local storage boundaries.
5. Split Android-only storage concerns away from pure collection contract consumption in a later tranche.
6. Consolidate repeated Gradle setup only after contract generation and fixture parity are stable.

Status: complete. `chronicle/collection-contracts` is the storage-free contract-consumption layer, `collection-base` retains Room/SQLCipher and re-exports contracts for compatibility, `ContractBoundaryTest` enforces the dependency boundary, and `chronicle/gradle/collection-library.gradle` owns common collection-library build configuration.

### Android compatibility guards

- Existing queued uploads must continue decoding.
- Room migrations must remain app-version compatible.
- SQLCipher storage behavior must not change during contract-generation tranches.
- Retired module IDs must stay readable but inactive.
- No Android collection module should switch upload path or payload value without OpenAPI and fixture proof.

## iOS

### Current evidence

- `chronicle-ios/chronicle/Utilities/ApiUtils.swift` hard-codes base URL constants, route segments, and endpoint builders.
- `chronicle-ios/chronicle/SensorReader/Sensor.swift` duplicates SensorKit enum values and maps them to `SRSensor`.
- `chronicle-ios/chronicle/Models/IOSDevice.swift` defines an API payload with a Jackson-style `@class` property.
- `chronicle-ios/chronicle/ScreenTime/ScreenTimeUsagePayload.swift` defines local Screen Time payload records, enums, envelopes, filtering, and date encoding.

### Target boundary

iOS should replace hand-written DTOs, contract enums, module IDs, and API paths with generated Swift contract files.

iOS platform-owned code remains:

- SensorKit adapters.
- Screen Time framework integration.
- CoreData or local persistence.
- Apple permission flows.
- Background delivery behavior.
- UI presentation and copy.

### iOS migration order

1. Generate Swift domain constants from `chronicle-models` without replacing call sites.
2. Generate Swift API DTOs and route builders from `chronicle-api/chronicle.yaml`.
3. Add fixture tests for `IOSDevice`, SensorKit payloads, Screen Time payloads, UUIDs, and date encoding.
4. Move hard-coded base URL configuration out of static constants into environment/configuration injection while preserving current defaults.
5. Replace `ApiUtils.swift` path construction with generated route builders behind compatibility wrappers.
6. Replace local SensorKit contract enums with generated values while keeping the native `SRSensor` adapter hand-written.
7. Replace hand-written upload payload DTOs only when fixture parity proves identical wire output.

Status: complete. `chronicle-ios/scripts/generate-ios-contracts.py` generates `ChronicleDomainContracts.swift`, `ChronicleRoutes.swift`, and `ChronicleWireDTOs.swift`. Enrollment, SensorKit, Screen Time, and user-identification uploads encode generated wire DTOs; local Apple-framework and persistence records map to those DTOs at the API boundary. ApiUtils compatibility wrappers, native SensorKit adapters, injected server configuration, and existing queued payload storage remain platform-owned.

### iOS compatibility guards

- Do not change `@class` wire values without a backend compatibility adapter and fixture proof.
- Do not change ISO timestamp precision or timezone behavior without fixture proof.
- Do not change UUID string casing/format.
- Keep compatibility wrappers for old route-building call sites until the migration is complete.
- Existing app installs must continue uploading queued Screen Time and SensorKit payloads.

## Shared Mobile Proofs

Both mobile platforms must pass:

- Contract value freshness checks.
- Fixture round-trip parity against JVM canonical fixtures.
- OpenAPI path compatibility checks.
- Backend ingestion fixture tests.
- Rollback verification showing older app payloads still ingest successfully.
