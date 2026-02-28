#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

cd "$ROOT_DIR"

./gradlew --write-verification-metadata sha256 help --console=plain

if ! git diff --exit-code -- gradle/verification-metadata.xml >/dev/null; then
  echo "Dependency verification gate failed: gradle/verification-metadata.xml is out of date." >&2
  echo "Run './gradlew --write-verification-metadata sha256 help' and commit gradle/verification-metadata.xml." >&2
  git --no-pager diff -- gradle/verification-metadata.xml
  exit 1
fi

echo "Dependency verification gate passed."
