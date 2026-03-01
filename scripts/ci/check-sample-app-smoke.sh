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

./gradlew sampleSmoke --stacktrace --console=plain

echo "Sample app smoke gate passed (Android/Desktop/Web)."
