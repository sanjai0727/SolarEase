# SolarEase Legal, Privacy, and Compliance Suite

**Effective Date**: October 28, 2026  
**Governing Laws**: Digital Personal Data Protection Act, 2023 (DPDP Act, India); Consumer Protection Act, 2019 & Consumer Protection (E-Commerce) Rules, 2020; Information Technology Act, 2000 & IT Rules, 2021.

---

## 1. Statutory Corporate Information & Grievance Redressal
In compliance with Rule 5(3)(a) of the Consumer Protection (E-Commerce) Rules, 2020 and Section 8(9) of the DPDP Act, 2023:
- **Operating Entity**: SolarEase Energy Technologies Private Limited
- **Registered Address**: Tech Innovation Hub, Guindy Industrial Estate, Chennai, Tamil Nadu 600032, India
- **Customer Support**: support@solarease.app | +91 44 2852 9000 (Mon–Fri, 9:00 AM – 6:00 PM IST)
- **Grievance Redressal Officer**:
  - Name: Priya Rangarajan
  - Designation: Grievance Officer & Data Protection Officer (DPO)
  - Email: grievance@solarease.app / privacy@solarease.app
  - Address: Same as registered address above
  - Statutory SLA: Acknowledgment within 48 hours; resolution within 30 days of receipt.

---

## 2. Privacy Policy (DPDPA 2023 & GDPR Aligned)

### 2.1 Notice of Personal Data Collected & Purpose of Processing
SolarEase collects personal data only for specific, lawful purposes:
1. **Identity & Authentication**: Mobile number, email, and Google/Firebase UID to authenticate accounts and prevent unauthorized account access.
2. **Location & Geospatial Telemetry**: Coarse and fine GPS coordinates (`latitude`, `longitude`) used strictly to calculate local solar irradiance, panel tilt, azimuth, and sunlight hours.
3. **Utility Electricity Billing Information**: TNEB service connection number (`serviceno`) and consumer billing records to calculate tariff slab progression under Tamil Nadu Electricity Board (TNEB) LT-1A tariffs.
4. **Appliance & Visual Video Sweeps (SolarLens)**: Camera photos and video sweeps of household appliances processed via Google Vertex AI / Gemini Vision API strictly to estimate connected load and recommend solar capacity.
5. **Billing and Transaction Records**: Order reference IDs, payment method tokens, and installation addresses for order execution and receipt generation.

### 2.2 Data Retention & Storage Policy
- Customer personal data is stored in localized databases within India (`asia-south1`).
- Media files uploaded for SolarLens OCR are processed transiently in streaming memory buffers and not retained in permanent databases after analysis completion.
- Session tokens are valid for 15 minutes (access) and 30 days (refresh with rotation). Revoked tokens are purged after expiry.
- Customers may request complete account deletion and data erasure via **Profile > Privacy & Permissions > Delete Account** or by contacting `privacy@solarease.app`.

### 2.3 User Rights as Data Principal
Under the Digital Personal Data Protection Act, 2023 (DPDP Act), you have the right to:
- **Right to Access Information**: View all collected personal data.
- **Right to Correction & Erasure**: Correct inaccurate data or request deletion of data no longer required for statutory/contractual purposes.
- **Right of Grievance Redressal**: Lodge complaints with our Grievance Officer or escalate to the Data Protection Board of India.
- **Right to Nominate**: Nominate a representative in the event of death or incapacity.

---

## 3. Terms of Service

### 3.1 Eligibility & Account Responsibility
By installing, registering, or using SolarEase, you represent that you are at least 18 years old and competent to contract under the Indian Contract Act, 1872. You agree to maintain the security of your device credentials.

### 3.2 Service Scope & Independent Analytical Status
SolarEase is an independent analytical application designed to assist consumers in solar capacity planning and energy monitoring.
- **Government Non-Affiliation**: SolarEase is **not** affiliated with, authorized by, sponsored by, or an agent of the Tamil Nadu Generation and Distribution Corporation (TANGEDCO), the Tamil Nadu Electricity Board (TNEB), the Ministry of New and Renewable Energy (MNRE), or any governmental authority.
- **Tariff & Savings Estimates**: All financial estimates, bill comparisons, and solar generation models are simulated analytical estimates based on published LT-1A tariff schedules and standard weather data. They do not constitute a financial guarantee or legal contract with your electricity utility.

### 3.3 Solar Vendor & Equipment Warranties
Vendor quotations, site surveys, and equipment (solar PV panels, inverters, bi-directional meters) are supplied by certified empaneled solar vendors. Warranties (typically 25-year panel performance, 5–10 year inverter) are backed directly by respective original equipment manufacturers (OEMs).

---

## 4. Refund & Cancellation Policy

In compliance with Consumer Protection (E-Commerce) Rules, 2020:
1. **Pre-Dispatch Cancellation (100% Full Refund)**: You may cancel your solar system order at any time before physical hardware dispatch or installation technician mobilization. A 100% full refund of the net amount paid will be issued.
2. **Refund Timeline**: Approved refunds are credited back to the original payment source (UPI, Net Banking, or Credit/Debit Card) within 5 to 7 banking days.
3. **Post-Dispatch & Installation**: Once hardware has been delivered to your site or site survey civil work has commenced, cancellations are subject to deduction of actual freight and site mobilization charges.

---

## 5. Cookie & Local Storage Policy

SolarEase operates on privacy-first storage principles:
1. **Mobile Application**: The Android mobile app does not use HTTP cookies. It uses hardware-encrypted local key-value storage (`EncryptedSharedPreferences` backed by `AndroidKeyStore` AES256-GCM) exclusively for local session state and cached electricity baseline readings.
2. **TNEB WebView Session Cookies**: When you query the official TNEB bill portal through the embedded WebView, session cookies (`JSESSIONID`) are issued directly by `tnebltd.gov.in` for the sole purpose of maintaining form state on the government web server. SolarEase does not track or sell these cookies.

---

## 6. Third-Party Sub-Processors & SDK Disclosures

In accordance with Google Play Developer Policy and Section 8 of the DPDP Act, the following sub-processors are utilized:
- **Google Cloud Platform / Cloud Run** (Infrastructure hosting, `asia-south1`, Mumbai): Hosts containerized backend services.
- **Firebase Authentication** (Google LLC): Authenticates mobile OTPs and social sign-in.
- **Google GenAI / Vertex AI** (Gemini 2.5 Flash / Gemini 3.8 Flash): Processes energy queries and vision OCR.
- **Google ML Kit** (On-device): Performs real-time camera text recognition and barcode scanning on your local device without transmitting images to cloud servers.
