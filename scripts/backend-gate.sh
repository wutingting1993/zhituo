#!/usr/bin/env bash
# Run from the business project's root. Arguments identify Java source directories.
set -euo pipefail
spec_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
if [[ $# -eq 0 ]]; then echo 'Usage: backend-gate.sh SOURCE_DIR [SOURCE_DIR ...]' >&2; exit 2; fi
[[ -f pom.xml ]] || { echo 'Run from a Maven project root containing pom.xml.' >&2; exit 2; }
# CI is read-only: reject formatting drift rather than changing source.
bash "$spec_root/scripts/java-quality.sh" check "$@"
bash "$spec_root/scripts/java-quality.sh" format-check "$@"
mvn -B -Dmaven.repo.local="$spec_root/.tool-cache/m2" clean compile
python3 "$spec_root/scripts/architecture-check.py" .
