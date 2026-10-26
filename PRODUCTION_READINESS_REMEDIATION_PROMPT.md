# SolarEase Production Readiness Remediation

You are responsible for taking the SolarEase Android app and FastAPI backend from an internal demo to a production-ready customer product.

Work directly in this repository. Preserve unrelated working-tree changes. Do not ship, deploy, publish, or change cloud resources. Make each change testable, add or repair tests for behavior you change, and report the files changed, test results, remaining risks, and migration steps.

## Objective

Resolve the release blockers below before treating any build as production-ready. Do not mask unfinished work with static values, success messages, or simulated results. Remove or hide customer-facing actions that are not implemented.

## Release blockers

### 1. Secure Android release packaging and local data

- Configure a real release signing flow. A release build must never use the debug signing configuration.
- Enable R8/resource shrinking for release builds and verify that a release build still works.
- Remove global cleartext traffic permission. Add a restrictive network security configuration only where an explicitly justified exception is required.
- Replace plaintext `SharedPreferences` storage for tokens and sensitive data with encrypted, per-user storage.
- Disable cloud backup and device-transfer backup for authentication material and TNEB records. Apply explicit backup exclusion rules.
- Remove release logging of phone numbers, Firebase identifiers, tokens, cookies, raw API errors, raw TNEB responses, and AI requests/responses.
- Narrow the `FileProvider` paths to the exact directories that need sharing.

Relevant files:

- `app/build.gradle.kts`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/java/com/example/core/session/UserSession.kt`
- `app/src/main/java/com/example/feature/home/ui/modules/TnebBillCache.kt`
- `app/src/main/res/xml/data_extraction_rules.xml`
- `app/src/main/res/xml/backup_rules.xml`

### 2. Repair authentication and session lifecycle

- Fix phone-number signup. A new verified Firebase phone user must be able to create a matching SolarEase account.
- Keep Firebase and SolarEase backend sessions consistent. Do not open Home merely because Firebase has a user while the backend session is absent or expired.
- Implement refresh-token rotation, server-side revocation, logout invalidation, and client-side recovery from 401 responses.
- Remove the redundant Firebase ID token from the request body; use the Authorization header only.
- Initialize and verify Firebase App Check for protected backend operations.
- Make the unfinished `/auth/send-otp`, `/auth/verify-otp`, and `/auth/session` endpoints either complete and secure or remove them from the public API.

Relevant files:

- `app/src/main/java/com/example/feature/auth/ui/AuthViewModel.kt`
- `app/src/main/java/com/example/MainActivity.kt`
- `app/src/main/java/com/example/core/network/AuthInterceptor.kt`
- `antigravity-backend/src/modules/auth/router.py`
- `antigravity-backend/src/modules/auth/service.py`
- `antigravity-backend/src/modules/auth/dependencies.py`
- `antigravity-backend/src/core/security.py`

### 3. Make meter and billing data reliable

- Establish a single authoritative synchronization model between the app and backend.
- On TNEB connection, create or update the backend baseline. On launch and refresh, download server state and reconcile it with pending local mutations.
- Implement an idempotent local outbox with retry, backoff, state visibility, and conflict resolution. Never ignore an unsuccessful HTTP response.
- Correct historical readings through a date-specific endpoint. Do not send them to the `update today` endpoint.
- Validate reading dates: reject dates before the active baseline and future readings unless explicitly supported.
- Prevent multiple active billing cycles at the database level and serialize rollover operations.
- Use decimal-safe values for money and document the tariff effective date/version. Respect the selected tariff instead of always applying LT-1A rules.
- Remove hardcoded bill/savings/ROI fallbacks. Show unavailable or needs-review states when source data is incomplete.

Relevant files:

- `app/src/main/java/com/example/feature/home/ui/HomeViewModel.kt`
- `app/src/main/java/com/example/feature/home/ui/modules/MeterReadingCache.kt`
- `app/src/main/java/com/example/feature/home/ui/modules/TnebBillDashboard.kt`
- `antigravity-backend/src/modules/meter/service.py`
- `antigravity-backend/src/modules/meter/models.py`
- `antigravity-backend/src/modules/meter/schemas.py`

### 4. Make Solar Lens truthful, safe, and performant

- Do not claim YOLO, verified appliance identification, or accurate wattage unless the shipped model actually provides those results.
- Generic ML Kit object candidates must be labeled as unclassified candidates and require user review before affecting solar sizing or financial calculations.
- Either implement photo/video scanning with a validated local model or remove those entry points. The current methods return empty results.
- Add bounded streaming uploads, client/server file size limits, MIME and content validation, cancellation, timeouts, and useful failure states.
- Do not load an entire video into memory on either client or server.
- Shut down camera executors and cancel analysis work when the screen is disposed.
- Add device tests for camera permission denial, rotation, repeated open/close, no detections, large media, and cancellation.

Relevant files:

- `app/src/main/java/com/example/feature/lens/detector/LensScanDetector.kt`
- `app/src/main/java/com/example/feature/lens/detector/YoloApplianceDetector.kt`
- `app/src/main/java/com/example/feature/lens/detector/MlKitApplianceDetector.kt`
- `app/src/main/java/com/example/feature/lens/ui/LensViewModel.kt`
- `app/src/main/java/com/example/feature/lens/ui/LensScreen.kt`
- `antigravity-backend/src/modules/lens/router.py`
- `antigravity-backend/src/modules/lens/service.py`

### 5. Protect AI and TNEB integrations

- Require suitable authentication and App Check for billable AI operations.
- Add per-user and IP-aware quotas, request-size limits, rate limiting, abuse monitoring, and graceful 429 behavior.
- Constrain chatbot query length and allowed mode/language values.
- Stop logging raw customer questions and raw model responses.
- Treat TNEB data as highly sensitive: minimize collected fields, never persist Aadhaar/PAN unless essential and approved, encrypt retained data, define retention/deletion behavior, and scope every cache to a user.
- Restrict the TNEB WebView to approved domains, preserve TLS failures, and handle third-party DOM changes without presenting fabricated bill data.
- Confirm that the TNEB workflow has the required user consent and approved integration approach before release.

Relevant files:

- `antigravity-backend/src/modules/chatbot/router.py`
- `antigravity-backend/src/modules/chatbot/schemas.py`
- `antigravity-backend/src/modules/chatbot/service.py`
- `antigravity-backend/src/core/middleware.py`
- `app/src/main/java/com/example/feature/home/ui/modules/TnebBillStatusWebView.kt`
- `app/src/main/java/com/example/feature/home/ui/modules/TnebBillModels.kt`
- `app/src/main/java/com/example/feature/home/ui/modules/ChatHistoryStorage.kt`
- `app/src/main/java/com/example/feature/home/ui/modules/BillCalculatorHistoryStorage.kt`

### 6. Make deployment repeatable and safe

- Add a CI pipeline that runs Android lint, unit tests, relevant device tests, backend unit tests, integration tests against disposable PostgreSQL, formatting, static analysis, dependency scanning, and release assembly.
- Make backend tests runnable from a clean environment. Fix the SQLite/PostgreSQL pool configuration conflict and use a dedicated test dependency set.
- Pin production Python dependencies with reproducible lock files.
- Run database migrations as a controlled deployment step before application rollout, with rollback and compatibility procedures.
- Move production settings and secrets to managed runtime configuration. Fail startup safely when required settings are absent.
- Replace wildcard CORS and production `DEBUG=True` defaults with explicit environment-specific configuration.
- Add meaningful readiness checks for database, Firebase, and critical dependencies.
- Add monitoring, structured redacted logs, crash reporting, alerting, and rollback documentation.

Relevant files:

- `antigravity-backend/cloudbuild.yaml`
- `antigravity-backend/Dockerfile`
- `antigravity-backend/requirements.txt`
- `antigravity-backend/src/core/config.py`
- `antigravity-backend/src/core/database.py`
- `antigravity-backend/src/core/middleware.py`

## Remove, hide, or implement customer-facing placeholders

Do not leave inert controls in production. Either fully implement each item with error handling and tests, or remove/hide it behind a disabled rollout flag.

- Facebook sign-in.
- Payment, download, PDF, and share actions in the TNEB screens.
- Solar Panel “Coming Soon” screen.
- Language setting, biometric lock, and notification settings that currently only persist booleans.
- Analytics navigation and the orphan `LensDetails` route.
- Mock dashboard, system details, installation, performance, financial, environmental, assessment, and order flows.
- Simulated survey and installation actions.
- Unused YOLO scaffold, unused dependencies, duplicate legacy source trees, committed binary artifacts, and obsolete UI-flow archives.

## Definition of done

A release candidate is acceptable only when all of the following are true:

1. It is release-signed with a private release key, shrinking enabled, no cleartext traffic, and no plaintext storage or backup of sensitive data.
2. New phone and Google users can sign up, sign in, restart, refresh sessions, sign out, and have revoked sessions rejected.
3. Meter data remains correct through offline usage, retries, app restarts, multiple devices, server failures, and conflicts.
4. Solar Lens labels uncertainty honestly and cannot turn generic detections into fabricated solar recommendations.
5. AI and media endpoints are authenticated, quota-limited, bounded, monitored, and do not log private content.
6. TNEB data is minimized, encrypted, scoped to the current user, removable, and excluded from backups.
7. No visible production control is a no-op, mock flow, or “coming soon” feature.
8. CI blocks release on failed Android/backend tests, lint, security checks, migration validation, and release assembly.
9. Deployment applies migrations safely and has monitoring, rollback, and incident procedures.

## Required final report

After implementation, provide:

- A priority-ordered list of resolved issues and any intentionally deferred items.
- The exact migrations, configuration values, and release steps required.
- Test commands and their results.
- A list of removed or hidden unfinished features.
- Remaining risks that need a product, security, legal, or operations decision.
