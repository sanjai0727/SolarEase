## Summary of Changes
<!-- Provide a concise summary of the business and technical changes -->

## Conventional Commit Type & Scope
<!-- Format: <type>(<scope>): <subject> -->
<!-- Examples: feat(auth): add google sign-in fallback -->
<!--           fix(meter): resolve bounding box crop offset -->
<!--           chore(deps): bump compose BOM to 2024.10.00 -->
**PR Title**: 

- [ ] **Type**: `feat` | `fix` | `refactor` | `perf` | `test` | `build` | `ci` | `chore` | `docs`
- [ ] Subject is in imperative mood ("add", not "added" or "adds")
- [ ] Subject is $\le$ 50 characters

## Branch Standards
- [ ] Branch follows convention: `feature/<desc>`, `fix/<desc>`, or `release/vX.Y.Z`
- [ ] Branch branched off up-to-date `main`

## Quality & Production Readiness Checklist
- [ ] `./gradlew testDebugUnitTest` executed and passing (100% tests pass)
- [ ] `./gradlew lintDebug` executed with 0 errors
- [ ] No hardcoded credentials, API keys, or machine-specific paths committed
- [ ] All user-facing strings referenced via `strings.xml`
- [ ] Relevant documentation updated (`README.md`, `CHANGELOG.md`)

## Semantic Versioning (For Release PRs Only)
- [ ] `versionName` and `versionCode` bumped in `app/build.gradle.kts`
- [ ] Annotated release tag prepared (`vMAJOR.MINOR.PATCH`)

