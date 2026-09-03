# Domain Contract Catalog

This catalog names each shared concept, its canonical owner, required consumers, and compatibility rules.

## Ownership Summary

| Concept family | Canonical owner | Required consumers |
| --- | --- | --- |
| Collection module IDs and state | `chronicle-models/src/main/kotlin/com/openlattice/chronicle/collection/CollectionModuleId.kt` | Android, iOS generated contracts, web generated domain constants, backend contract tests |
| Privacy classes and defaults | `chronicle-models/src/main/kotlin/com/openlattice/chronicle/collection/CollectionPrivacyClass.kt`, `CollectionDefaults.kt`, module metadata in `CollectionModuleId.kt` | Android settings, iOS settings, web study configuration, backend/export policy tests |
| Android sensor types and mapping | `chronicle-models/src/main/kotlin/com/openlattice/chronicle/android/AndroidSensorType.kt`, `chronicle-models/src/main/kotlin/com/openlattice/chronicle/collection/SensorCollectionModules.kt` | Android collection modules, web configuration UI, backend Android sensor ingestion tests |
| iOS SensorKit sensor types | `chronicle-models/src/main/kotlin/com/openlattice/chronicle/sensorkit/SensorType.kt` | iOS generated contracts and native SensorKit adapter |
| Study feature IDs | `chronicle-models/src/main/kotlin/com/openlattice/chronicle/study/StudyFeature.kt` | Web feature UI, backend study behavior, mobile study settings |
| Participation/export/lifecycle/status enums | `chronicle-models/src/main/java/com/openlattice/chronicle/data/ParticipationStatus.java`, `StudyLifecycleStatus.kt`, `ParticipantDataType.kt` | Web operations state, backend study APIs, generated mobile API/domain types |
| Consent triggers and data disposition | `ConsentTrigger.kt`, `CollectionDataDisposition.kt` | Web consent/admin UI, backend enforcement/export, mobile participation flows |
| HTTP paths and request/response shapes | `chronicle-api/chronicle.yaml` | Web OpenAPI types, iOS generated route builders and API DTOs, backend controller compatibility tests |
| Database tables/RLS/export behavior | `chronicle-server/src/main/resources/db/migration/` and backend services | Backend ingestion, export jobs, contract tests only |

## Collection Module IDs

`CollectionModuleId` values are stable external IDs. The canonical states are:

| State | Source rule | Consumer behavior |
| --- | --- | --- |
| Active | `isActive = true` in `CollectionModuleId.kt` and included by `activeModules`. | May appear in generated active lists, settings UI, backend ingestion matrices, and export eligibility checks. |
| Reserved/inactive | Present with `isActive = false` and not marked retired. | May be decoded and documented but must not be enabled by defaults or UI unless a future ADR activates it. |
| Retired | Listed in `retiredIds` in `CollectionModuleId.kt`. | Decode-only. Must never be reintroduced into defaults, web module selectors, or backend active-module coverage as active. |

Active module metadata must include:

- Stable ID.
- Privacy class.
- Default enablement override when present.
- Platform applicability when derivable from mappings.
- Fixture family when payload upload exists.

## Privacy Classes and Defaults

Canonical source: `CollectionPrivacyClass.kt`, `CollectionDefaults.kt`, and per-module metadata in `CollectionModuleId.kt`.

Required exported fields:

- Privacy class ID.
- Default-enabled value.
- Module-specific default override.
- Policy note for whether consent or sensitive handling is expected.

Web may own display labels and explanatory copy but must prove exhaustive coverage for every active generated module ID.

## Android Sensor Types and Mapping

Canonical source:

- `chronicle-models/src/main/kotlin/com/openlattice/chronicle/android/AndroidSensorType.kt`
- `chronicle-models/src/main/kotlin/com/openlattice/chronicle/collection/SensorCollectionModules.kt`

Required exported fields:

- Android sensor type ID.
- Mapped collection module ID.
- Active/retired sensor module state.
- Display order contract for deterministic admin/configuration surfaces.

Android runtime adapters remain native. Generated web/iOS values must not imply that non-Android platforms support Android sensor collection.

## iOS SensorKit Sensor Types

Canonical source: `chronicle-models/src/main/kotlin/com/openlattice/chronicle/sensorkit/SensorType.kt`.

Required exported fields:

- Stable SensorKit string value.
- Mapped collection module when one exists.
- Fixture family for upload payloads when applicable.

iOS native adapter ownership remains in the iOS app. `chronicle-ios/chronicle/SensorReader/Sensor.swift` should eventually become a mapping layer from generated contract values to `SRSensor`, not a second source of truth.

## Study Feature IDs

Canonical source: `StudyFeature.kt`.

Required exported fields:

- Feature ID.
- Active status.
- Optional required platform or backend capability when defined by future contract metadata.

Consumers:

- Web study creation/configuration UI.
- Backend study configuration validation.
- Mobile study behavior and settings.

## Participation, Export, Lifecycle, and Status Enums

Canonical sources:

- `ParticipationStatus.java`
- `StudyLifecycleStatus.kt`
- `ParticipantDataType.kt`
- Related DTOs in `chronicle-models`

Rules:

- Generated consumers must tolerate unknown server values for server-controlled lifecycle/status fields unless the API marks the enum closed.
- Existing persisted or wire values must not be renamed.
- Web Zod schemas may validate UI forms but should be generated from or checked against canonical values.

## Consent Triggers and Data Disposition

Canonical sources:

- `ConsentTrigger.kt`
- `CollectionDataDisposition.kt`

Rules:

- Consent trigger IDs are policy-significant and stable.
- Data disposition IDs are export/deletion-policy-significant and stable.
- Backend enforcement remains backend-owned and must be tested against generated value sets.

## Upload Envelope Schema Versions and Fixture Names

Canonical owner: `chronicle-models`, with ingestion proof in `chronicle-server`.

Required catalog fields:

- Module ID.
- Payload family name.
- Current schema version.
- Fixture file names.
- Expected time field semantics.
- Required identity/scoping fields.
- Backend handler and table target.

Initial fixture families should cover:

- Android sensor data.
- iOS SensorKit data.
- Screen Time usage.
- User identification.
- Device settings.
- Battery telemetry.
- Interaction events.
- App audio activity and content.
- Notification activity.
- Sleep events.
- Activity recognition.
- Health metrics.
- Connectivity state.
- App network usage.
- Encrypted payloads.
- iOS device enrollment.

## Backward Compatibility

- Retired values stay decodable forever unless a separate data migration proves no persisted or queued payload can contain them.
- Unknown values from API responses must not crash generated clients.
- Generated consumers must expose active and all-known value lists separately.
- Wire-breaking value changes require an ADR update, fixture update, server compatibility adapter, mobile rollout plan, and rollback path.
