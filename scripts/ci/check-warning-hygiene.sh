#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
OUTPUT_FILE="$(mktemp)"
trap 'rm -f "$OUTPUT_FILE"' EXIT

cd "$ROOT_DIR"

if ! ./gradlew help --warning-mode all --console=plain >"$OUTPUT_FILE" 2>&1; then
  cat "$OUTPUT_FILE"
  echo "Warning hygiene gate failed: unable to run Gradle warning scan." >&2
  exit 1
fi

cat "$OUTPUT_FILE"

for pattern in \
  "Mokkery was compiled against Kotlin" \
  "Calling configuration method 'attributes(Action)' is deprecated for configuration 'kover'" \
  "Deprecated Gradle features were used in this build, making it incompatible with Gradle 9.0."; do
  if grep -Fq "$pattern" "$OUTPUT_FILE"; then
    echo "Warning hygiene gate failed: detected forbidden warning pattern: $pattern" >&2
    exit 1
  fi
done

echo "Warning hygiene gate passed."
