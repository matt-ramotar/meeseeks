# Changelog

All notable changes to Meeseeks are documented in this file.

This project follows semantic versioning.

## [Unreleased]

## [1.0.1] - 2026-02-20

### Fixed

- Resolved iOS crash when logging task results by replacing `TaskResult.Type` serializer lookup in `TaskResultAdapter` with string-based encoding/decoding and compatibility handling for both JSON-quoted and plain stored values (issue #64).

## [1.0.0] - 2026-02-17

### Added

- Enforced KLib ABI validation in `:runtime` via `klibApiCheck`.
- Release workflow verification gate before Maven Central publish.
- Stable API surface and compatibility statement in docs.

### Changed

- `VERSION_NAME` and published coordinates moved from snapshot to `1.0.0`.
- Runtime module now uses strict explicit API mode.
- Preflight checks now validate real Android SDK component availability.
- `CHROME_BIN` preflight behavior is adaptive: warning when unset, hard failure only for invalid explicit paths.

### Fixed

- Removed internal `WorkerRegistration` leakage from public inline API signatures.
- iOS reschedule aggregation no longer forces both network and charging preconditions when only one is required.
- Removed unstable Kotlin compiler flag `-XXLanguage:+ImplicitSignedToUnsignedIntegerConversion`.
- Sample placeholder TODOs replaced with minimal working sample types.

## Release Notes Process

When preparing a release:

1. Move relevant entries from `Unreleased` into a new version section.
2. Keep entries grouped by `Added`, `Changed`, `Fixed`, `Removed`, `Security`.
3. Include concise migration notes when behavior changes are not backward compatible.
4. Link PRs/issues where possible.

Release notes template:

```markdown
## [<version>] - <yyyy-mm-dd>

### Added
- ...

### Changed
- ...

### Fixed
- ...

### Removed
- ...

### Security
- ...
```
