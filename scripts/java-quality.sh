#!/usr/bin/env bash
set -euo pipefail
spec_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
exec python3 "$spec_root/scripts/java-quality.py" "$@"
