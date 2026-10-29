# SolarEase: Full Security, Scalable System Design & 20-Point Legal/Compliance Audit

**Target System**: SolarEase (FastAPI Backend, Android Jetpack Compose Mobile App, PostgreSQL, Redis, Caddy, Google Cloud Run)  
**Date**: October 28, 2026  
**Auditor**: Antigravity Security & System Architecture Review  
**Governing Jurisdictions**: India (Primary: DPDP Act 2023, Consumer Protection Act 2019, IT Act 2000, IT Rules 2021, CCPA Dark Patterns 2023, BIS IS 19000:2022) and Global Standards (GDPR / CCPA / OWASP Top 10 Mobile & API).

---

# PART I: Complete 20-Point Legal & Compliance Audit

### 1. Missing or Inadequate Privacy Policy
- **Risk Level**: **CRITICAL**
- **Exact Location in Code**: [`app/src/main/java/com/example/feature/profile/ui/ProfileScreen.kt:2771`](file:///c:/Users/yashv/dev/SolarEase/app/src/main/java/com/example/feature/profile/ui/ProfileScreen.kt#L2771)
- **Why It Creates Legal Risk**: The mobile application previously contained only a single static text line (`"• Privacy Policy: Zero third-party tracker policy."`) without a comprehensive privacy notice. Under Section 5 and Section 6 of the Digital Personal Data Protection Act, 2023 (DPDP Act, India), data fiduciaries must present an itemized privacy notice detailing all categories of personal data collected, specific processing purposes, retention terms, and grievance redressal contacts before collecting consent. Failure attracts penalties of up to ₹250 Crores under Section 33 of the DPDP Act.
- **Specific Fix Required**: Host and link an exhaustive statutory Privacy Policy detailing all 5 data processing categories, data principal rights, retention periods, and DPO details.
- **Suggested Compliant Code / Action**: Created [`docs/LEGAL_AND_COMPLIANCE_SUITE.md`](file:///c:/Users/yashv/dev/SolarEase/docs/LEGAL_AND_COMPLIANCE_SUITE.md#2-privacy-policy-dpdpa-2023--gdpr-aligned) and updated in-app link in `ProfileScreen.kt`.
- **Relevant Laws**: DPDP Act 2023 (Sec 5, 6, 8, 33); IT Rules 2011 (Rule 4); GDPR (Art 13/14); California Consumer Privacy Act (CCPA/CPRA).

---

### 2. Missing or Inadequate Terms of Service
- **Risk Level**: **HIGH**
- **Exact Location in Code**: [`app/src/main/java/com/example/feature/profile/ui/ProfileScreen.kt:2770`](file:///c:/Users/yashv/dev/SolarEase/app/src/main/java/com/example/feature/profile/ui/ProfileScreen.kt#L2770)
- **Why It Creates Legal Risk**: Lacked formal contractual terms defining user rights, solar vendor installation liability, warranty limitations, dispute arbitration, and utility tariff disclaimer clauses. Without binding Terms of Service, the operator cannot enforce acceptable use policies, limit civil liability, or protect against disputes regarding vendor solar installations.
- **Specific Fix Required**: Formulate binding Terms of Service establishing consumer contract terms, vendor intermediation boundaries, warranty pass-throughs, and jurisdiction.
- **Suggested Compliant Code / Action**: Included comprehensive Terms of Service in [`docs/LEGAL_AND_COMPLIANCE_SUITE.md`](file:///c:/Users/yashv/dev/SolarEase/docs/LEGAL_AND_COMPLIANCE_SUITE.md#3-terms-of-service).
- **Relevant Laws**: Indian Contract Act 1872; Consumer Protection (E-Commerce) Rules 2020 (Rule 5); Information Technology (Intermediary Guidelines and Digital Media Ethics Code) Rules 2021 (Rule 3(1)(a)).

---

### 3. Missing or Unclear Refund Policy
- **Risk Level**: **HIGH**
- **Exact Location in Code**: [`app/src/main/java/com/example/feature/order/ui/PaymentOptionScreen.kt:145-160`](file:///c:/Users/yashv/dev/SolarEase/app/src/main/java/com/example/feature/order/ui/PaymentOptionScreen.kt#L145-L160)
- **Why It Creates Legal Risk**: The payment initiation screen permitted customers to pay sums up to several lakhs without disclosing cancellation rights, cooling-off periods, refund eligibility, or refund timelines. Rule 5(3)(a) of the Consumer Protection (E-Commerce) Rules, 2020 mandates unambiguous display of terms of contract, cancellation, refund, exchange, and chargeback procedures before payment.
- **Specific Fix Required**: Insert explicit refund terms above the checkout action button and provide a detailed 100% pre-dispatch refund guarantee policy.
- **Suggested Compliant Code**:
```kotlin
Text(
    text = "By proceeding with payment, you agree to our Terms of Service & Privacy Policy. 100% full refund is guaranteed if cancelled prior to hardware dispatch.",
    style = MaterialTheme.typography.bodySmall,
    color = MaterialTheme.colorScheme.onSurfaceVariant
)
```
- **Relevant Laws**: Consumer Protection Act 2019 (Sec 2(47), 18); Consumer Protection (E-Commerce) Rules 2020.

---

### 4. Missing Cookie Policy
- **Risk Level**: **MEDIUM**
- **Exact Location in Code**: [`app/src/main/java/com/example/feature/home/ui/modules/TnebBillStatusWebView.kt:53-58`](file:///c:/Users/yashv/dev/SolarEase/app/src/main/java/com/example/feature/home/ui/modules/TnebBillStatusWebView.kt#L53-L58)
- **Why It Creates Legal Risk**: The application interacts with external government web servers (`tnebltd.gov.in`) storing `JSESSIONID` cookies, and the backend issues bearer JWTs. While the native app does not set HTTP tracking cookies, inspecting and transmitting session cookies without disclosure creates compliance gaps under EU/UK ePrivacy regulations.
- **Specific Fix Required**: Publish a Cookie & Local Storage Policy clarifying that the native app uses hardware KeyStore encryption instead of cookies, while WebView cookies are ephemeral to government server sessions.
- **Suggested Compliant Code**: Documented in Section 5 of [`docs/LEGAL_AND_COMPLIANCE_SUITE.md`](file:///c:/Users/yashv/dev/SolarEase/docs/LEGAL_AND_COMPLIANCE_SUITE.md#5-cookie--local-storage-policy).
- **Relevant Laws**: EU ePrivacy Directive (Directive 2002/58/EC as amended by 2009/136/EC); UK PECR (Privacy and Electronic Communications Regulations).

---

### 5. Invalid Cookie Consent
- **Risk Level**: **LOW**
- **Exact Location in Code**: [`app/src/main/java/com/example/feature/home/ui/modules/TnebBillStatusWebView.kt:53-58`](file:///c:/Users/yashv/dev/SolarEase/app/src/main/java/com/example/feature/home/ui/modules/TnebBillStatusWebView.kt#L53-L58)
- **Why It Creates Legal Risk**: Reading `android.webkit.CookieManager.getInstance().getCookie(...)` for session diagnostics without prior user awareness could be interpreted as non-consensual state inspection.
- **Specific Fix Required**: Ensure all cookie processing is strictly confined to essential session connectivity with explicit disclosure in the storage policy.
- **Relevant Laws**: GDPR (Article 6(1)(a) & Article 7); ePrivacy Directive.

---

### 6. Invalid Form Consent (Bundled / Implicit Consent)
- **Risk Level**: **HIGH**
- **Exact Location in Code**: [`app/src/main/java/com/example/feature/order/ui/PaymentOptionScreen.kt`](file:///c:/Users/yashv/dev/SolarEase/app/src/main/java/com/example/feature/order/ui/PaymentOptionScreen.kt)
- **Why It Creates Legal Risk**: Previous onboarding and checkout forms allowed users to click "Pay" or "Submit" without displaying active consent check-wraps or policy links. Under Section 6(1) of India's DPDP Act, 2023, consent must be free, specific, informed, unconditional, and an unambiguous clear affirmative action. Implicit or bundled consent is legally void.
- **Specific Fix Required**: Add explicit affirmative consent notices linking directly to the Privacy Policy and Terms of Service prior to payment and data submission.
- **Suggested Compliant Code**: Added consent text above the checkout button in `PaymentOptionScreen.kt`.
- **Relevant Laws**: DPDP Act 2023 (Section 6(1), (2), (3)); GDPR (Art 7).

---

### 7. Undisclosed Analytics
- **Risk Level**: **MEDIUM**
- **Exact Location in Code**: [`app/src/main/java/com/example/feature/analytics/ui/AnalyticsScreen.kt:510-530`](file:///c:/Users/yashv/dev/SolarEase/app/src/main/java/com/example/feature/analytics/ui/AnalyticsScreen.kt#L510-L530)
- **Why It Creates Legal Risk**: The codebase computes user energy metrics, tariff analytics, and app event logs. If telemetry or performance data is transmitted off-device without disclosure in a privacy policy, it breaches Google Play User Data policies.
- **Specific Fix Required**: Fully disclose all analytical features and internal performance logging in the Privacy Policy.
- **Relevant Laws**: DPDP Act 2023; Google Play Developer Policy (User Data).

---

### 8. Undisclosed User Tracking
- **Risk Level**: **HIGH**
- **Exact Location in Code**: [`app/src/main/java/com/example/feature/profile/ui/ProfileScreen.kt:2771`](file:///c:/Users/yashv/dev/SolarEase/app/src/main/java/com/example/feature/profile/ui/ProfileScreen.kt#L2771)
- **Why It Creates Legal Risk**: The app stated `"Zero third-party tracker policy"`, but integrates Firebase Auth, Google Play Services, and Firebase App Check, which transmit device fingerprinting data (IP, Android ID, attestation tokens) to Google infrastructure. An absolute claim of "zero third-party tracking" while transmitting device identifiers constitutes a deceptive representation under Consumer Protection Act 2019.
- **Specific Fix Required**: Replace the inaccurate statement with an explicit third-party sub-processor list identifying Google Cloud and Firebase services.
- **Suggested Compliant Code**: Refactored in `ProfileScreen.kt:2770-2773` and [`docs/LEGAL_AND_COMPLIANCE_SUITE.md`](file:///c:/Users/yashv/dev/SolarEase/docs/LEGAL_AND_COMPLIANCE_SUITE.md#6-third-party-sub-processors--sdk-disclosures).
- **Relevant Laws**: Consumer Protection Act 2019 (Sec 2(28), 89); FTC Act (Sec 5).

---

### 9. Undisclosed Third-Party SDKs
- **Risk Level**: **MEDIUM**
- **Exact Location in Code**: [`app/build.gradle.kts:100-170`](file:///c:/Users/yashv/dev/SolarEase/app/build.gradle.kts#L100-L170)
- **Why It Creates Legal Risk**: App bundles Firebase Admin, Google ML Kit, Google GenAI SDK, Retrofit, OkHttp, Moshi, and certifi without an in-app Open Source License / Attribution notice. Failure to distribute licenses breaches Apache 2.0 and MIT copyright conditions.
- **Specific Fix Required**: Include Section 6 in the Compliance Suite and add an Open Source Licenses acknowledgment in settings.
- **Relevant Laws**: Apache License 2.0 (Clause 4); MIT License; Copyright Act 1957 (India).

---

### 10. Undisclosed External Tools & Cloud APIs
- **Risk Level**: **MEDIUM**
- **Exact Location in Code**: [`backend/src/modules/lens/service.py:119-132`](file:///c:/Users/yashv/dev/SolarEase/backend/src/modules/lens/service.py#L119-L132)
- **Why It Creates Legal Risk**: Video and photo sweeps of private domestic residences are transmitted to Google Vertex AI / Gemini Vision API in `asia-south1`. Without disclosing cloud AI analysis to consumers, users could claim unauthorized transmission of private residential imagery.
- **Specific Fix Required**: Provide explicit on-screen notice during SolarLens onboarding informing the user that photos/videos will be processed by Google Vertex AI for appliance identification.
- **Relevant Laws**: DPDP Act 2023 (Sec 5); Information Technology (Reasonable Security Practices and Procedures and Sensitive Personal Data or Information) Rules, 2011.

---

### 11. Collection of Unnecessary Data
- **Risk Level**: **MEDIUM**
- **Exact Location in Code**: [`app/src/main/AndroidManifest.xml:8`](file:///c:/Users/yashv/dev/SolarEase/app/src/main/AndroidManifest.xml#L8)
- **Why It Creates Legal Risk**: Requesting `android.permission.ACCESS_FINE_LOCATION` when rooftop solar estimation requires only city/pincode regional solar irradiance violates the Data Minimization Principle under Section 6(1) of DPDP Act 2023.
- **Specific Fix Required**: Ensure `ACCESS_COARSE_LOCATION` is offered as an alternative, and allow users to manually enter postal codes without requiring high-precision GPS.
- **Relevant Laws**: DPDP Act 2023 (Sec 6(1)); GDPR (Art 5(1)(c)).

---

### 12. Collection of Sensitive Personal Data
- **Risk Level**: **HIGH**
- **Exact Location in Code**: [`app/src/main/AndroidManifest.xml:9`](file:///c:/Users/yashv/dev/SolarEase/app/src/main/AndroidManifest.xml#L9) and [`backend/src/modules/lens/router.py:60`](file:///c:/Users/yashv/dev/SolarEase/backend/src/modules/lens/router.py#L60)
- **Why It Creates Legal Risk**: High-definition video sweeps of residential living areas and electricity billing records constitute sensitive behavioral data. Processing camera video sweeps without specific purpose limitation and security safeguards triggers heightened liability under IT Rules 2011 and DPDP Act 2023.
- **Specific Fix Required**: Implement zero-retention ephemeral processing for uploaded video sweeps on the backend and declare that video data is never stored permanently.
- **Suggested Compliant Code**: Confirmed in `docs/LEGAL_AND_COMPLIANCE_SUITE.md` Section 2.2.
- **Relevant Laws**: DPDP Act 2023 (Sec 8); IT (SPDI) Rules 2011.

---

### 13. Missing Accessibility Alt Text (Content Descriptions)
- **Risk Level**: **MEDIUM**
- **Exact Location in Code**: [`app/src/main/java/com/example/feature/order/ui/PaymentOptionScreen.kt:62,91`](file:///c:/Users/yashv/dev/SolarEase/app/src/main/java/com/example/feature/order/ui/PaymentOptionScreen.kt#L62-L91)
- **Why It Creates Legal Risk**: Decorative and action icons across payment, scan, and order screens set `contentDescription = null`, rendering screens unusable for blind and visually impaired consumers using Android TalkBack.
- **Specific Fix Required**: Provide descriptive, localization-friendly content descriptions for interactive and status icons.
- **Suggested Compliant Code**:
```kotlin
Icon(
    imageVector = Icons.Default.Payment,
    contentDescription = "Payment Methods Selection Header",
    tint = MaterialTheme.colorScheme.primary
)
```
- **Relevant Laws**: Rights of Persons with Disabilities Act, 2016 (India); WCAG 2.1 (Success Criterion 1.1.1 Non-text Content).

---

### 14. Insufficient Color Contrast
- **Risk Level**: **LOW**
- **Exact Location in Code**: [`app/src/main/java/com/example/feature/profile/ui/ProfileScreen.kt:2760-2775`](file:///c:/Users/yashv/dev/SolarEase/app/src/main/java/com/example/feature/profile/ui/ProfileScreen.kt#L2760-L2775)
- **Why It Creates Legal Risk**: Subdued footnote text using `fontSize = 11.sp` with `StitchOnSurfaceVariant` fails the 4.5:1 minimum contrast ratio against dark/light container surfaces under outdoor sunlight conditions.
- **Specific Fix Required**: Elevate text contrast to meet WCAG 2.1 Level AA (minimum 4.5:1 for normal text).
- **Relevant Laws**: Rights of Persons with Disabilities Act 2016; WCAG 2.1 (Success Criterion 1.4.3).

---

### 15. Missing Keyboard / Focus Navigation
- **Risk Level**: **LOW**
- **Exact Location in Code**: [`app/src/main/java/com/example/ui/components/SharedComponents.kt`](file:///c:/Users/yashv/dev/SolarEase/app/src/main/java/com/example/ui/components/SharedComponents.kt)
- **Why It Creates Legal Risk**: Custom selectable cards without explicit focus indicators cannot be navigated via hardware keyboards, directional pads, or switch-access assistive devices.
- **Specific Fix Required**: Ensure all `SelectableCard` elements implement `Modifier.focusable()` and clear focus-state border highlights.
- **Relevant Laws**: WCAG 2.1 (Success Criterion 2.1.1 Keyboard).

---

### 16. Fake or Misleading Reviews / Testimonials
- **Risk Level**: **HIGH**
- **Exact Location in Code**: [`app/src/main/java/com/example/feature/`](file:///c:/Users/yashv/dev/SolarEase/app/src/main/java/com/example/feature/)
- **Why It Creates Legal Risk**: Any unverified customer ratings or fictitious testimonials violate the Bureau of Indian Standards (BIS) standard **IS 19000:2022** on "Online Consumer Reviews — Principles and Requirements for their Collection, Moderation and Publication".
- **Specific Fix Required**: Verify that all published customer reviews originate from verified purchasers with transparent attribution and no fabricated quotes.
- **Relevant Laws**: Consumer Protection Act 2019 (Sec 2(47), 10); BIS Standard IS 19000:2022.

---

### 17. Dark UX Patterns
- **Risk Level**: **HIGH**
- **Exact Location in Code**: [`app/src/main/java/com/example/feature/profile/ui/ProfileScreen.kt`](file:///c:/Users/yashv/dev/SolarEase/app/src/main/java/com/example/feature/profile/ui/ProfileScreen.kt)
- **Why It Creates Legal Risk**: The Central Consumer Protection Authority (CCPA) issued the **Guidelines for Prevention and Regulation of Dark Patterns, 2023**, prohibiting practices such as *Forced Bundling*, *Confirm Shaming*, *Disguised Advertisements*, and *Interface Interference*.
- **Specific Fix Required**: Ensure account deletion, consent revocation, and opt-outs are directly accessible without artificial hurdles or deceptive phrasing.
- **Relevant Laws**: Guidelines for Prevention and Regulation of Dark Patterns, 2023 (CCPA, India).

---

### 18. Hidden Fees
- **Risk Level**: **HIGH**
- **Exact Location in Code**: [`app/src/main/java/com/example/feature/order/ui/PaymentOptionScreen.kt:108-124`](file:///c:/Users/yashv/dev/SolarEase/app/src/main/java/com/example/feature/order/ui/PaymentOptionScreen.kt#L108-L124)
- **Why It Creates Legal Risk**: Presenting a single "Gross Total" minus "Subsidy" without disclosing whether GST (12% to 18%) is included, or omitting DISCOM bi-directional net-metering synchronization fees, violates Section 5(3)(a) of Consumer Protection (E-Commerce) Rules 2020.
- **Specific Fix Required**: Display an explicit footnote confirming GST inclusion and disclosing potential DISCOM application fee variations.
- **Suggested Compliant Code**: Applied in `PaymentOptionScreen.kt:123-130`.
- **Relevant Laws**: Consumer Protection (E-Commerce) Rules 2020; Legal Metrology Act 2009.

---

### 19. Unlicensed Images, Trademarks, or Government Logos
- **Risk Level**: **CRITICAL**
- **Exact Location in Code**: [`app/src/main/java/com/example/feature/home/ui/modules/TnebBillStatusWebView.kt`](file:///c:/Users/yashv/dev/SolarEase/app/src/main/java/com/example/feature/home/ui/modules/TnebBillStatusWebView.kt)
- **Why It Creates Legal Risk**: Referencing the registered state utility trademark "TNEB" and scraping `tnebltd.gov.in` without an explicit non-affiliation disclaimer risks trademark infringement and statutory action under the Emblems and Names (Prevention of Improper Use) Act, 1950.
- **Specific Fix Required**: Display a prominent statutory disclaimer stating that SolarEase is an independent software application and is not authorized, affiliated with, or endorsed by TANGEDCO/TNEB.
- **Suggested Compliant Code**: Applied in `ProfileScreen.kt:2757-2764` and `LEGAL_AND_COMPLIANCE_SUITE.md`.
- **Relevant Laws**: Trade Marks Act 1999; Emblems and Names (Prevention of Improper Use) Act 1950.

---

### 20. Unsupported or Misleading Claims
- **Risk Level**: **CRITICAL**
- **Exact Location in Code**: [`app/src/main/java/com/example/feature/profile/ui/ProfileScreen.kt:2759`](file:///c:/Users/yashv/dev/SolarEase/app/src/main/java/com/example/feature/profile/ui/ProfileScreen.kt#L2759)
- **Why It Creates Legal Risk**: The app previously claimed `"Certified Standards: TNEB Smart Grid Compliance • REAP Telemetry v2.4"`. Claiming government or utility regulatory certification that has not been officially granted constitutes a Misleading Advertisement under Section 2(28) of the Consumer Protection Act, 2019, punishable with fines up to ₹50 Lakhs and operational bans under Section 89.
- **Specific Fix Required**: Completely remove the unverified certification claim and replace it with a factual analytical description.
- **Suggested Compliant Code**: Replaced in `ProfileScreen.kt:2757-2764`.
- **Relevant Laws**: Consumer Protection Act 2019 (Sec 2(28), 89); Advertising Standards Council of India (ASCI) Code.

---

# PART II: Scalable, Pragmatic System Design Architecture

Applying the core engineering philosophy:
> *"What problem forced us to add this? Start simple. Find the bottleneck. Add the smallest fix. Repeat."*

```
[Mobile App (Android)] 
         │ (HTTPS / TLS)
         ▼
[Caddy Reverse Proxy & SSL Gateway] (Port 80/443)
         │ (HTTP Keep-Alive, X-Real-IP)
         ▼
[FastAPI / Uvicorn Multi-Worker Backend] (Cloud Run / Docker)
    ├── (Rate Limiting: SHA-256 Token Hash / Evicted In-Memory / Redis)
    ├── (In-Memory Mini-Index Embedding Retrieval: <5ms vector matching)
    ├── (Transient Streaming Media Buffer: 10MB/25MB chunks)
         │
         ├──► [Redis 7 with Password Auth] ── Cache for Tariff Slabs & User Session Blacklist
         │
         ├──► [PostgreSQL 16] ── Connection Pool (pool_size=10, max_overflow=20)
         │       └── Tables: users, billing_cycles, meter_readings, appliances, revoked_tokens
         │       └── Indexes: users(username), billing_cycles(user_id, is_active), meter_readings(uq)
         │
         └──► [Google Vertex AI / Gemini 3.8 Flash] ── Vision OCR & Chatbot Assistant
```

### The 9 Real-World Bottleneck Resolutions

1. **One server can't keep up → Load balancing + Stateless Multi-Worker Backend**:
   - *Problem*: A single Python process blocks or exhausts CPU on high request volume.
   - *Minimal Solution*: Run Uvicorn with multiple workers (`--workers 2` or dynamic Cloud Run container autoscaling). All state (tokens, sessions) is stored in PostgreSQL and Redis, making backend instances 100% stateless.
2. **Every server hammers the database → Redis Caching**:
   - *Problem*: Repeatedly fetching the same static TNEB LT-1A tariff slab rates or active billing cycles strains database IOPS.
   - *Minimal Solution*: Cache tariff calculation rules in Redis with a 24-hour TTL.
3. **Reads keep growing → PostgreSQL Read Replicas**:
   - *Problem*: Analytics dashboard queries and meter reading graphs compete with transaction writes.
   - *Minimal Solution*: Route read-only queries (`GET /meter/graph`, `GET /meter/consumption/current`) to a PostgreSQL read replica using SQLAlchemy multi-bind engine when read volume exceeds 500 QPS.
4. **One database can't hold it all → Sharding / Partitioning**:
   - *Problem*: `meter_readings` grows by millions of rows per year.
   - *Minimal Solution*: PostgreSQL declarative partitioning by year/month on `reading_date` for `meter_readings`, avoiding complex application-level sharding until reaching tens of millions of rows.
5. **Photos and videos bloat the database → Object Storage (MinIO / S3 / GCS)**:
   - *Problem*: Storing binary photos and video sweeps in PostgreSQL BLOBs causes catastrophic table bloat, slow backups, and high memory consumption.
   - *Minimal Solution*: Stream uploads directly through FastAPI bounded chunks to MinIO/Cloud Storage, storing only the secure object URI in the database.
6. **Users far away wait forever → Global CDN / Edge Proxy (Cloudflare / Caddy)**:
   - *Problem*: Latency from regional network hops on mobile API calls.
   - *Minimal Solution*: Terminate TLS at the edge with Caddy or Cloudflare CDN, providing HTTP/2 multiplexing and caching static assets.
7. **Uploads make users wait on slow jobs → Background Workers / Async Queue**:
   - *Problem*: Calling Gemini Vision API on a 25MB video sweep blocks the HTTP request for 10–20 seconds.
   - *Minimal Solution*: Use FastAPI `BackgroundTasks` or a lightweight Redis queue (`RQ`/Celery) so the user receives an immediate `202 Accepted` and polls for results.
8. **Servers crash, regions disappear → High Availability & Multi-Zone Deployments**:
   - *Problem*: Data center node failure causes service outage.
   - *Minimal Solution*: Cloud Run multi-zone managed container orchestration with automatic health checks (`/healthz` probe interval 30s) and PostgreSQL automated daily backups.
9. **You can't see what's breaking → Structured Observability**:
   - *Problem*: Silent errors, unhandled exceptions, and unmonitored API latency.
   - *Minimal Solution*: Python `logging` with structured JSON output, readiness probes (`/ready`), and latency timing logs (`Gemini generation completed in X.XXs`).

---

# PART III: Verification & Validation Status

1. **Security Vulnerability Remediations**:
   - Rate limiting key collision fixed in [`backend/src/core/ratelimit.py`](file:///c:/Users/yashv/dev/SolarEase/backend/src/core/ratelimit.py).
   - Information disclosure sanitized in [`backend/src/main.py`](file:///c:/Users/yashv/dev/SolarEase/backend/src/main.py).
   - Redis password authentication enforced in [`docker-compose.prod.yml`](file:///c:/Users/yashv/dev/SolarEase/docker-compose.prod.yml).
   - Android WebView DOM script injection fixed in [`TnebBillStatusWebView.kt`](file:///c:/Users/yashv/dev/SolarEase/app/src/main/java/com/example/feature/home/ui/modules/TnebBillStatusWebView.kt).
   - Database indexes added in Alembic migration `f782a19b0d24` and SQLAlchemy models.
2. **Legal & Compliance Suite Created**:
   - Complete 20-point compliance suite published in [`docs/LEGAL_AND_COMPLIANCE_SUITE.md`](file:///c:/Users/yashv/dev/SolarEase/docs/LEGAL_AND_COMPLIANCE_SUITE.md).
   - Disclaimers and refund policies integrated into `ProfileScreen.kt` and `PaymentOptionScreen.kt`.
3. **Automated Validators**:
   - `validate-findings.cjs`: **PASS (8 findings valid)**
   - `validate-coverage-ledger.cjs`: **PASS (9 coverage units valid)**
