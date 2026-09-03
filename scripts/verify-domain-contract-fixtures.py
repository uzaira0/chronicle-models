#!/usr/bin/env python3
"""Verify the reviewed domain-contract fixture projection.

This is the first fixture parity gate for chronicle-models. Later Swift,
TypeScript, Android, and backend checks can consume the same fixture; this script
keeps the fixture aligned with the deterministic JVM-domain export.
"""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path
from typing import Any


REPO_ROOT = Path(__file__).resolve().parents[1]
CONTRACT_PATH = REPO_ROOT / "generated/domain-contracts/chronicle-domain-contracts.json"
FIXTURE_PATH = REPO_ROOT / "fixtures/domain-contracts/domain-contract-fixture.json"


def load_json(path: Path) -> dict[str, Any]:
    return json.loads(path.read_text(encoding="utf-8"))


def fixture_projection(contract: dict[str, Any]) -> dict[str, Any]:
    contracts = contract["contracts"]
    return {
        "schemaVersion": "chronicle-domain-fixture/v1",
        "sourceContractSchemaVersion": contract["schemaVersion"],
        "collectionModules": [
            {
                "id": module["id"],
                "enumName": module["enumName"],
                "active": module["active"],
                "privacyClass": module["privacyClass"],
                "defaultEnabled": module["defaultEnabled"],
                "activeDefaultEnabled": module["activeDefaultEnabled"],
                "androidSensorModule": module["androidSensorModule"],
            }
            for module in contracts["collectionModules"]
        ],
        "activeCollectionModuleIds": contracts["activeCollectionModuleIds"],
        "activeDefaultEnabledCollectionModuleIds": contracts["activeDefaultEnabledCollectionModuleIds"],
        "inactiveCollectionModuleIds": contracts["inactiveCollectionModuleIds"],
        "androidSensorModuleIds": contracts["androidSensorModuleIds"],
        "privacyClasses": contracts["privacyClasses"],
        "androidSensorMappings": contracts["androidSensorMappings"],
        "androidSensorTypes": contracts["androidSensorTypes"],
        "iosSensorTypes": contracts["iosSensorTypes"],
        "studyFeatures": contracts["studyFeatures"],
        "participantDataTypes": contracts["participantDataTypes"],
        "participationStatuses": contracts["participationStatuses"],
        "studyLifecycleStatuses": contracts["studyLifecycleStatuses"],
        "consentTriggers": contracts["consentTriggers"],
        "collectionDataDispositions": contracts["collectionDataDispositions"],
    }


def render_json(payload: dict[str, Any]) -> str:
    return json.dumps(payload, indent=2, sort_keys=False) + "\n"


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--update", action="store_true", help="rewrite the fixture projection")
    args = parser.parse_args()

    if not CONTRACT_PATH.exists():
        print(f"Missing generated contract artifact: {CONTRACT_PATH}", file=sys.stderr)
        print("Run: scripts/generate-domain-contracts.py", file=sys.stderr)
        return 1

    expected = render_json(fixture_projection(load_json(CONTRACT_PATH)))

    if args.update:
        FIXTURE_PATH.parent.mkdir(parents=True, exist_ok=True)
        FIXTURE_PATH.write_text(expected, encoding="utf-8")
        print(f"Wrote {FIXTURE_PATH.relative_to(REPO_ROOT)}")
        return 0

    if not FIXTURE_PATH.exists():
        print(f"Missing fixture projection: {FIXTURE_PATH}", file=sys.stderr)
        print("Run: scripts/verify-domain-contract-fixtures.py --update", file=sys.stderr)
        return 1

    current = FIXTURE_PATH.read_text(encoding="utf-8")
    if current != expected:
        print(f"Domain contract fixture projection is stale: {FIXTURE_PATH}", file=sys.stderr)
        print("Run: scripts/verify-domain-contract-fixtures.py --update", file=sys.stderr)
        return 1

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
