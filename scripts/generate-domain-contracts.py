#!/usr/bin/env python3
"""Generate Chronicle domain contracts from chronicle-models source.

The generated JSON is intentionally deterministic: no timestamps, host paths, or
Git metadata. It is a bridge artifact for web/iOS/backend parity checks; the
Kotlin and Java model files remain the canonical source.
"""

from __future__ import annotations

import argparse
import json
import re
import sys
from pathlib import Path
from typing import Any


REPO_ROOT = Path(__file__).resolve().parents[1]
DEFAULT_OUTPUT = REPO_ROOT / "generated/domain-contracts/chronicle-domain-contracts.json"
FIXTURE_REGISTRY = REPO_ROOT / "fixtures/payloads/registry.json"
FIXTURE_REGISTRY_SCHEMA_VERSION = "chronicle-fixture-registry/v1"
FAMILY_NAME_PATTERN = re.compile(r"^[a-z][a-z0-9]*(-[a-z0-9]+)*$")

SOURCE_FILES = {
    "collectionModuleId": "src/main/kotlin/com/openlattice/chronicle/collection/CollectionModuleId.kt",
    "collectionPrivacyClass": "src/main/kotlin/com/openlattice/chronicle/collection/CollectionPrivacyClass.kt",
    "sensorCollectionModules": "src/main/kotlin/com/openlattice/chronicle/collection/SensorCollectionModules.kt",
    "androidSensorType": "src/main/kotlin/com/openlattice/chronicle/android/AndroidSensorType.kt",
    "iosSensorType": "src/main/kotlin/com/openlattice/chronicle/sensorkit/SensorType.kt",
    "studyFeature": "src/main/kotlin/com/openlattice/chronicle/study/StudyFeature.kt",
    "participantDataType": "src/main/kotlin/com/openlattice/chronicle/study/ParticipantDataType.kt",
    "participationStatus": "src/main/java/com/openlattice/chronicle/data/ParticipationStatus.java",
    "studyLifecycleStatus": "src/main/kotlin/com/openlattice/chronicle/study/StudyLifecycleStatus.kt",
    "consentTrigger": "src/main/kotlin/com/openlattice/chronicle/collection/ConsentTrigger.kt",
    "collectionDataDisposition": "src/main/kotlin/com/openlattice/chronicle/collection/CollectionDataDisposition.kt",
    # Not Kotlin/Java source: the reviewed payload fixture registry embedded as
    # contracts.fixtureRegistry (never used with read_source()).
    "fixtureRegistry": "fixtures/payloads/registry.json",
}


def read_source(key: str) -> str:
    return (REPO_ROOT / SOURCE_FILES[key]).read_text(encoding="utf-8")


def strip_comments(text: str) -> str:
    text = re.sub(r"/\*.*?\*/", "", text, flags=re.S)
    text = re.sub(r"//.*", "", text)
    return text


def enum_body(text: str, enum_name: str) -> str:
    match = re.search(rf"enum\s+(?:class\s+)?{re.escape(enum_name)}[^\{{]*\{{(.*)\n\}}", text, flags=re.S)
    if not match:
        raise ValueError(f"Unable to find enum body for {enum_name}")
    body = match.group(1)
    body = re.split(r"\n\s*(?:public\s+)?companion\s+object\s*\{", body, maxsplit=1)[0]
    return strip_comments(body)


def parse_simple_kotlin_enum(source_key: str, enum_name: str) -> list[str]:
    body = enum_body(read_source(source_key), enum_name)
    values: list[str] = []
    for token in re.findall(r"\b([A-Za-z][A-Za-z0-9_]*)\b\s*(?:,|;|\(|$)", body):
        if token in {"public", "val", "get", "JsonValue"}:
            continue
        if token and token not in values:
            values.append(token)
    return values


def parse_java_enum(source_key: str, enum_name: str) -> list[str]:
    text = strip_comments(read_source(source_key))
    match = re.search(rf"enum\s+{re.escape(enum_name)}\s*\{{(.*?)\}}", text, flags=re.S)
    if not match:
        raise ValueError(f"Unable to find Java enum body for {enum_name}")
    body = re.split(r";", match.group(1), maxsplit=1)[0]
    return [token.strip() for token in body.split(",") if token.strip()]


def parse_privacy_classes() -> list[dict[str, Any]]:
    body = enum_body(read_source("collectionPrivacyClass"), "CollectionPrivacyClass")
    classes: list[dict[str, Any]] = []
    for match in re.finditer(r"\b([A-Z][A-Z0-9_]*)\s*\(\s*(true|false)\s*\)", body):
        classes.append(
            {
                "name": match.group(1),
                "defaultEnabled": match.group(2) == "true",
            }
        )
    if not classes:
        raise ValueError("No CollectionPrivacyClass values parsed")
    return classes


def parse_collection_modules(privacy_defaults: dict[str, bool]) -> list[dict[str, Any]]:
    body = enum_body(read_source("collectionModuleId"), "CollectionModuleId")
    modules: list[dict[str, Any]] = []
    pattern = re.compile(
        r"\b([A-Z][A-Z0-9_]*)\s*\(\s*"
        r'"([^"]+)"\s*,\s*'
        r"CollectionPrivacyClass\.([A-Z][A-Z0-9_]*)\s*,\s*"
        r"(true|false)"
        r"(?:\s*,\s*defaultEnabledOverride\s*=\s*(true|false))?"
        r"\s*\)",
        flags=re.S,
    )
    for match in pattern.finditer(body):
        enum_name = match.group(1)
        privacy_class = match.group(3)
        if privacy_class not in privacy_defaults:
            raise ValueError(f"{enum_name} references unknown privacy class {privacy_class}")
        default_override = None if match.group(5) is None else match.group(5) == "true"
        default_enabled = privacy_defaults[privacy_class] if default_override is None else default_override
        modules.append(
            {
                "enumName": enum_name,
                "id": match.group(2),
                "privacyClass": privacy_class,
                "active": match.group(4) == "true",
                "defaultEnabledOverride": default_override,
                "defaultEnabled": default_enabled,
            }
        )
    if not modules:
        raise ValueError("No CollectionModuleId values parsed")
    ids = [module["id"] for module in modules]
    if len(ids) != len(set(ids)):
        raise ValueError("Duplicate CollectionModuleId ids parsed")
    return modules


def parse_sensor_mappings(module_by_enum: dict[str, dict[str, Any]]) -> list[dict[str, Any]]:
    text = strip_comments(read_source("sensorCollectionModules"))
    android_sensor_types = set(parse_simple_kotlin_enum("androidSensorType", "AndroidSensorType"))
    mappings: list[dict[str, Any]] = []
    pattern = re.compile(
        r"AndroidSensorType\.([A-Za-z][A-Za-z0-9_]*)\s+to\s+CollectionModuleId\.([A-Z][A-Z0-9_]*)"
    )
    for sensor_type, module_enum in pattern.findall(text):
        module = module_by_enum.get(module_enum)
        if module is None:
            raise ValueError(f"Sensor mapping references unknown module {module_enum}")
        mappings.append(
            {
                "androidSensorType": sensor_type,
                "collectionModuleId": module["id"],
                "collectionModuleEnum": module_enum,
                "active": module["active"],
                "displayOrder": len(mappings),
            }
        )
    if not mappings:
        raise ValueError("No Android sensor mappings parsed")
    mapped_sensor_types = {mapping["androidSensorType"] for mapping in mappings}
    if mapped_sensor_types != android_sensor_types:
        missing = sorted(android_sensor_types - mapped_sensor_types)
        extra = sorted(mapped_sensor_types - android_sensor_types)
        raise ValueError(f"Android sensor mapping drift: missing={missing}, extra={extra}")
    return mappings


def parse_dispositions() -> list[dict[str, str]]:
    body = enum_body(read_source("collectionDataDisposition"), "CollectionDataDisposition")
    values = []
    for enum_name, wire_id in re.findall(r"\b([A-Z][A-Z0-9_]*)\s*\(\s*\"([^\"]+)\"\s*\)", body):
        values.append({"enumName": enum_name, "id": wire_id})
    if not values:
        raise ValueError("No CollectionDataDisposition values parsed")
    return values


def load_fixture_registry(known_module_ids: set[str]) -> dict[str, Any]:
    """Load and validate fixtures/payloads/registry.json (Tranche 3).

    A malformed registry fails generation: every family must be uniquely named
    kebab-case, reference only known collection module ids (or null), declare a
    payload schema version >= 1, and list fixture files that exist on disk under
    the family's own directory. Mirrors FixtureRegistry.parse in the JVM tests.
    """
    if not FIXTURE_REGISTRY.exists():
        raise ValueError(f"Missing fixture registry: {FIXTURE_REGISTRY}")
    registry = json.loads(FIXTURE_REGISTRY.read_text(encoding="utf-8"))

    if registry.get("schemaVersion") != FIXTURE_REGISTRY_SCHEMA_VERSION:
        raise ValueError(
            "Fixture registry schemaVersion must be "
            f"{FIXTURE_REGISTRY_SCHEMA_VERSION!r}: {registry.get('schemaVersion')!r}"
        )
    families = registry.get("families")
    if not isinstance(families, list) or not families:
        raise ValueError("Fixture registry must declare a non-empty 'families' array")

    names: list[str] = []
    for entry in families:
        if not isinstance(entry, dict):
            raise ValueError(f"Fixture family entry must be an object: {entry!r}")
        family = entry.get("family")
        if not isinstance(family, str) or not FAMILY_NAME_PATTERN.match(family):
            raise ValueError(f"Fixture family name must be kebab-case: {family!r}")
        names.append(family)

        if "collectionModuleId" not in entry:
            raise ValueError(f"Family {family!r} must declare collectionModuleId (id or null)")
        module_id = entry["collectionModuleId"]
        if module_id is not None and module_id not in known_module_ids:
            raise ValueError(f"Family {family!r} references unknown CollectionModuleId: {module_id!r}")

        version = entry.get("payloadSchemaVersion")
        if not isinstance(version, int) or isinstance(version, bool) or version < 1:
            raise ValueError(f"Family {family!r} payloadSchemaVersion must be an integer >= 1: {version!r}")

        if "jvmClass" not in entry:
            raise ValueError(f"Family {family!r} must declare jvmClass (FQCN or null)")
        jvm_class = entry["jvmClass"]
        if jvm_class is not None and (not isinstance(jvm_class, str) or not jvm_class.strip()):
            raise ValueError(f"Family {family!r} jvmClass must be null or a non-blank FQCN")

        fixture_files = entry.get("fixtureFiles")
        if not isinstance(fixture_files, list) or not fixture_files:
            raise ValueError(f"Family {family!r} must declare a non-empty fixtureFiles array")
        for path in fixture_files:
            if not isinstance(path, str) or not path.startswith(f"fixtures/payloads/{family}/"):
                raise ValueError(
                    f"Family {family!r} fixture file must live under fixtures/payloads/{family}/: {path!r}"
                )
            if not (REPO_ROOT / path).is_file():
                raise ValueError(f"Family {family!r} fixture file does not exist: {path}")

        for field in ("timeSemantics", "backendHandler", "backendTable"):
            value = entry.get(field)
            if not isinstance(value, str) or not value.strip():
                raise ValueError(f"Family {family!r} must declare a non-blank {field}")

        scoping = entry.get("scopingFields")
        if not isinstance(scoping, list) or not scoping:
            raise ValueError(f"Family {family!r} must declare a non-empty scopingFields array")

    duplicates = {name for name in names if names.count(name) > 1}
    if duplicates:
        raise ValueError(f"Duplicate fixture family names: {sorted(duplicates)}")

    return registry


def build_contract() -> dict[str, Any]:
    privacy_classes = parse_privacy_classes()
    privacy_defaults = {entry["name"]: entry["defaultEnabled"] for entry in privacy_classes}
    collection_modules = parse_collection_modules(privacy_defaults)
    module_by_enum = {entry["enumName"]: entry for entry in collection_modules}
    sensor_mappings = parse_sensor_mappings(module_by_enum)
    sensor_module_ids = {mapping["collectionModuleId"] for mapping in sensor_mappings}
    fixture_registry = load_fixture_registry({module["id"] for module in collection_modules})

    modules_with_sensor_flag = []
    for module in collection_modules:
        enriched = dict(module)
        enriched["androidSensorModule"] = module["id"] in sensor_module_ids
        enriched["activeDefaultEnabled"] = bool(module["active"] and module["defaultEnabled"])
        modules_with_sensor_flag.append(enriched)

    return {
        "schemaVersion": "chronicle-domain-contracts/v1",
        "generatedBy": "scripts/generate-domain-contracts.py",
        "manualEdit": False,
        "sourceRepo": "chronicle-models",
        "sourceFiles": SOURCE_FILES,
        "contracts": {
            "collectionModules": modules_with_sensor_flag,
            "activeCollectionModuleIds": [
                module["id"] for module in modules_with_sensor_flag if module["active"]
            ],
            "activeDefaultEnabledCollectionModuleIds": [
                module["id"] for module in modules_with_sensor_flag if module["activeDefaultEnabled"]
            ],
            "inactiveCollectionModuleIds": [
                module["id"] for module in modules_with_sensor_flag if not module["active"]
            ],
            "androidSensorModuleIds": [
                module["id"] for module in modules_with_sensor_flag if module["androidSensorModule"]
            ],
            "privacyClasses": privacy_classes,
            "androidSensorTypes": parse_simple_kotlin_enum("androidSensorType", "AndroidSensorType"),
            "androidSensorMappings": sensor_mappings,
            "iosSensorTypes": parse_simple_kotlin_enum("iosSensorType", "SensorType"),
            "studyFeatures": parse_simple_kotlin_enum("studyFeature", "StudyFeature"),
            "participantDataTypes": parse_simple_kotlin_enum("participantDataType", "ParticipantDataType"),
            "participationStatuses": parse_java_enum("participationStatus", "ParticipationStatus"),
            "studyLifecycleStatuses": parse_simple_kotlin_enum("studyLifecycleStatus", "StudyLifecycleStatus"),
            "consentTriggers": parse_simple_kotlin_enum("consentTrigger", "ConsentTrigger"),
            "collectionDataDispositions": parse_dispositions(),
            "fixtureRegistry": fixture_registry,
        },
        "compatibility": {
            "unknownCollectionModuleIds": "Consumers that support tolerant settings decode should ignore unknown module ids.",
            "inactiveCollectionModuleIds": "Inactive ids are decode-compatible and must not be offered as active collection modules.",
            "generatedConsumers": "Generated consumers must preserve unknown-value handling appropriate to their platform boundary.",
        },
    }


def render_json(payload: dict[str, Any]) -> str:
    return json.dumps(payload, indent=2, sort_keys=False) + "\n"


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", type=Path, default=DEFAULT_OUTPUT)
    parser.add_argument("--check", action="store_true", help="fail if the generated artifact is stale")
    args = parser.parse_args()

    output = args.output if args.output.is_absolute() else REPO_ROOT / args.output
    rendered = render_json(build_contract())

    if args.check:
        if not output.exists():
            print(f"Missing generated contract artifact: {output}", file=sys.stderr)
            return 1
        current = output.read_text(encoding="utf-8")
        if current != rendered:
            print(f"Generated contract artifact is stale: {output}", file=sys.stderr)
            print("Run: scripts/generate-domain-contracts.py", file=sys.stderr)
            return 1
        return 0

    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(rendered, encoding="utf-8")
    print(f"Wrote {output.relative_to(REPO_ROOT)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
