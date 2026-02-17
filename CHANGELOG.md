# Changelog

All notable changes to Meeseeks are documented in this file.

This project follows semantic versioning.

## [Unreleased]

### Added

- Platform-focused scheduler tests for JVM, JS, and Native adapter behavior.
- Contributor preflight validation for Android SDK and `CHROME_BIN`.
- Release workflow split: snapshot CI path and manual GA release workflow.

### Changed

- Tooling plugin migrated away from deprecated Kotlin Gradle APIs.
- Runtime precondition handling is explicit per platform with fail-fast validation.

### Fixed

- Periodic builder interval mapping correctness.
- Runtime API/ABI surface hygiene around internal SQLDelight enum leakage.

## [0.6.0-SNAPSHOT]

### Notes

- Active development snapshot toward `1.0.0`.

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
