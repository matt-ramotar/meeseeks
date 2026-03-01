#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

cd "$ROOT_DIR"

if [[ -z "${GRADLE_USER_HOME:-}" ]]; then
  DEFAULT_GRADLE_HOME="${HOME}/.gradle"
  if [[ -w "$DEFAULT_GRADLE_HOME" || ( ! -e "$DEFAULT_GRADLE_HOME" && -w "${HOME}" ) ]]; then
    export GRADLE_USER_HOME="$DEFAULT_GRADLE_HOME"
  else
    export GRADLE_USER_HOME="$ROOT_DIR/.gradle-home"
  fi
fi

mkdir -p "$GRADLE_USER_HOME"

OUTPUT_FILE="$(mktemp)"
trap 'rm -f "$OUTPUT_FILE"' EXIT

if ! ./gradlew \
  help \
  :runtime:dependencies \
  :sample:multiplatform:dependencies \
  :sample:androidApp:dependencies \
  :sample:desktopApp:dependencies \
  :sample:webApp:dependencies \
  :tooling:plugins:dependencies \
  :sample:multiplatform:commonizeNativeDistribution \
  resolveIdeDependenciesAll \
  --write-verification-metadata sha256 \
  --console=plain >"$OUTPUT_FILE" 2>&1; then
  cat "$OUTPUT_FILE"
  echo "Dependency verification gate failed: unable to refresh verification metadata." >&2
  exit 1
fi

if ! git diff --quiet -- gradle/verification-metadata.xml; then
  cat "$OUTPUT_FILE"
  echo "Dependency verification metadata drift detected in gradle/verification-metadata.xml." >&2
  echo "Run the metadata refresh command and commit the updated file:" >&2
  echo "./gradlew help :runtime:dependencies :sample:multiplatform:dependencies :sample:androidApp:dependencies :sample:desktopApp:dependencies :sample:webApp:dependencies :tooling:plugins:dependencies :sample:multiplatform:commonizeNativeDistribution resolveIdeDependenciesAll --write-verification-metadata sha256" >&2
  echo >&2
  git --no-pager diff --stat -- gradle/verification-metadata.xml >&2
  echo "Preview of verification metadata diff:" >&2
  git --no-pager diff -- gradle/verification-metadata.xml | sed -n '1,200p' >&2
  exit 1
fi

echo "Dependency verification gate passed."
