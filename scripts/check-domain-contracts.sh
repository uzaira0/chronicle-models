#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

scripts/generate-domain-contracts.py --check
scripts/verify-domain-contract-fixtures.py
