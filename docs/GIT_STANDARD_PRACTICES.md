# SolarEase Engineering Git Standards & Team Workflow

This document outlines the mandatory Git standards and branch governance for the **SolarEase** engineering team.

---

## 1. Branching Model: GitHub Flow / Trunk-Based

We use a single source of truth branch: `main`.

```
                    feature/SOLAR-101-auth-flow
                     o───o───o
                    /         \  (Pull Request + CI)
───o───────────────o───────────o───────────────────────────o─── [main]
                                \                         /
                                 o───o (release/v1.2.0)  /
                                  \                     /
                                   Tag: v1.2.0 ─────────
```

### Core Rules
1. **Never commit directly to `main`**: All changes enter `main` exclusively through Pull Requests with at least 1 approving code review and passing CI.
2. **Ephemeral Branches Only**:
   - Feature branches: `feature/<ticket-or-description>` (e.g. `feature/meter-history`, `feature/bill-calculator`).
   - Bugfix branches: `fix/<ticket-or-description>` (e.g. `fix/ocr-bounding-box`).
   - Release branches: `release/vX.Y.Z` (temporary branch used only to stabilize release builds, cut the tag, and merge back into `main`).
3. **No Long-Lived Version Branches**: Branches like `app-version-0` or `app-version-1` are forbidden. Releases are tracked permanently via **Annotated Git Tags**.

---

## 2. Conventional Commits Standard

Every commit and Pull Request title must strictly adhere to the Conventional Commits specification:

$$\text{<type>}(\text{<scope>}): \text{<subject>}$$

### Types
| Type | Usage |
|---|---|
| `feat` | A new user-facing feature or capability |
| `fix` | A bug fix or runtime defect resolution |
| `refactor` | Code restructuring without feature addition or bug fix |
| `perf` | Performance optimization (e.g., memory reduction, rendering speed) |
| `test` | Adding or correcting unit, integration, or Robolectric tests |
| `build` | Gradle, AGP, dependency catalog, or toolchain changes |
| `ci` | GitHub Actions workflow, runner, or pipeline modifications |
| `chore` | Maintenance tasks, repository hygiene, or cleanup |
| `docs` | Documentation updates (README, architecture, ADRs) |

### Scope (Optional but Recommended)
Standard scopes: `auth`, `meter`, `lens`, `home`, `assessment`, `order`, `profile`, `network`, `storage`, `ui`.

### Subject Formatting Rules
- **Imperative mood**: Use "add" (not "added" / "adds"), "fix" (not "fixed"), "update" (not "updates").
- **Length limit**: Maximum **50 characters** for the first line.
- **Lowercase**: Do not start subject with a capital letter.
- **No trailing period**: Never end the subject with a `.`.

---

## 3. Semantic Versioning (SemVer) & Release Tagging

Version numbers follow $\text{MAJOR}.\text{MINOR}.\text{PATCH}$ (e.g. `1.1.0`):
- **MAJOR**: Breaking architectural changes, incompatible database migrations, or public API overhauls.
- **MINOR**: New backward-compatible features added (e.g., Solar Lens, Energy Details module).
- **PATCH**: Backward-compatible bug fixes, security patches, or build optimizations.

### Creating an Annotated Release Tag
Never use lightweight tags (`git tag v1.0.0`). Always create annotated tags with release notes:

```bash
# 1. Ensure working directory is clean and tests pass
./gradlew testDebugUnitTest
./gradlew lintDebug

# 2. Create annotated tag
git tag -a v1.2.0 -m "Release v1.2.0: Summary of features and fixes"

# 3. Push tag to origin
git push origin v1.2.0
```

---

## 4. How We Enforce These Standards

1. **GitHub Branch Protection**:
   - Require PR before merging into `main`.
   - Require CI status checks to pass (`lint-pr-title`, `Build & Unit Tests`).
   - Require squash merging to keep git history linear and clean.
2. **Automated CI PR Linter**:
   - Every Pull Request title is automatically validated against Conventional Commits via GitHub Actions.
3. **Local Developer Git Hook**:
   - Run once to activate local commit message validation:
     ```bash
     chmod +x .git/hooks/commit-msg
     ```

