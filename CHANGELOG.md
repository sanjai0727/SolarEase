# Changelog

All notable changes to the SolarEase Android project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]
### Added
- Enterprise-grade CI/CD pipeline definition (`.github/workflows/android-ci.yml`).
- Repository hygiene configurations: `.editorconfig`, `.gitattributes`, `lint.xml`, `keystore.properties.example`.
- Architecture and system design documentation (`docs/ARCHITECTURE.md`).

### Changed
- Standardized release signing to prevent fallback to debug keystore in production builds.
- Added night-mode resource coverage and dimension definitions.

### Security
- Hardened `.gitignore` to prevent committing signing keys, editor caches, and personal configurations.
- Sanitized root repository by untracking bloated binary and documentation blobs.

## [2.0.0] - 2026-10-28
### Added
- Offline meter reading scanner with edge LCD detection and reticle cropping.
- Dynamic TNEB bill calculator and tariff estimation engine.
- SolarLens multi-photo appliance audit report generation.
- Full bilingual Q&A retrieval chatbot for electricity advisory.

## [1.0.0] - 2026-08-15
### Added
- Initial release with consumer number lookup, customer category mapping, and baseline quote flow.

