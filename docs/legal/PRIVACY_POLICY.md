# Privacy Policy — SolarEase

**Document Status:** DRAFT — NOT FOR PUBLICATION — REQUIRES INDEPENDENT LEGAL REVIEW  
**Effective Date:** [PLACEHOLDER: Effective Date, e.g., 1st November 2026]  
**Last Updated:** [PLACEHOLDER: Date of Last Update, e.g., 23rd October 2026]  
**Version:** 1.0 (Draft)

---

> ### IMPORTANT LEGAL NOTICE
> **THIS PRIVACY POLICY IS A WORKING DRAFT PREPARED SPECIFICALLY FOR THE SOLAREASE ANDROID APPLICATION. IT HAS NOT BEEN APPROVED BY QUALIFIED LEGAL COUNSEL. DO NOT PUBLISH, DISTRIBUTE, OR INCORPORATE THIS DOCUMENT INTO THE APPLICATION OR PRODUCTION ENVIRONMENT WITHOUT FORMAL REVIEW AND SIGN-OFF BY AN ADVOCATE OR DATA PRIVACY COUNSEL QUALIFIED IN THE REPUBLIC OF INDIA UNDER THE DIGITAL PERSONAL DATA PROTECTION ACT, 2023 (DPDP ACT) AND THE INFORMATION TECHNOLOGY ACT, 2000.**

---

## 1. Introduction and Scope

**[PLACEHOLDER: Legal Business Entity Name, e.g., SolarEase Technologies Private Limited]** (referred to as **"Company"**, **"we"**, **"us"**, or **"our"**) operates the **SolarEase** Android mobile application (the **"App"** or **"Platform"**). We respect your privacy and are committed to protecting your personal data in compliance with the **Digital Personal Data Protection Act, 2023 (DPDP Act)**, the **Information Technology Act, 2000**, and the **Information Technology (Reasonable Security Practices and Procedures and Sensitive Personal Data or Information) Rules, 2011 (SPDI Rules)**.

This Privacy Policy explains how we collect, process, store, disclose, transfer, and protect your personal data when you download, install, access, or use the SolarEase mobile application. It also describes your statutory privacy rights as a Data Principal under Indian law and how you can exercise them.

By downloading, installing, registering for, or using the Platform, you acknowledge that you have read this Privacy Policy and give your free, specific, informed, unconditional, and unambiguous consent to the collection and processing of your personal data as described herein. **If you do not agree with this Privacy Policy, please do not use the Platform and uninstall the App from your device.**

---

## 2. Definitions Under Applicable Law

In this Privacy Policy, unless the context otherwise requires, capitalized terms shall have the meanings ascribed to them under the Digital Personal Data Protection Act, 2023:
- **"Data Principal"** means the individual to whom the personal data relates (i.e., you, the User).
- **"Data Fiduciary"** means the Company, which alone or in conjunction with other persons determines the purpose and means of the processing of personal data.
- **"Data Processor"** means any person or entity that processes personal data on behalf of the Data Fiduciary (e.g., cloud hosting, analytics, or AI service providers).
- **"Personal Data"** means any data about an individual who is identifiable by or in relation to such data.
- **"Processing"** means an automated or manual operation or set of operations performed on personal data, such as collection, recording, organization, structuring, storage, adaptation, retrieval, consultation, use, alignment, sharing, erasure, or destruction.

---

## 3. Personal Data We Collect & How We Collect It

We collect only such personal data as is reasonably necessary to fulfill the specific purposes of the SolarEase platform (principle of data minimization). Based on the actual architecture of the SolarEase Android app, the data we collect is categorized below:

### 3.1 Account & Identity Data
When you register, authenticate, or manage your user profile in SolarEase:
- **Mobile Number:** Collected during sign-up or login for sending verification One-Time Passwords (OTPs) via SMS.
- **Name / Username:** Provided voluntarily by you during profile creation or retrieved from your authenticated Google Account.
- **Email Address:** Provided voluntarily during registration or profile management, used for account recovery and customer support.
- **Google Account Identifiers:** When you sign in with Google, we receive your Google ID token, Firebase Authentication UID, and basic profile info (email and display name). We do not receive your Google password.

### 3.2 Electricity Utility Data (TNEB / TNPDCL)
To analyze your electricity consumption and recommend rooftop solar capacity:
- **Consumer Service Number / EB Number:** Your electricity connection identifier (e.g., TNEB / TANGEDCO consumer number).
- **Billing History & Tariff Details:** Retrieved from public consumer portals (such as `tnebltd.gov.in`) upon your request, including:
  - Consumer name and registered service address;
  - Tariff category (e.g., Domestic Tariff 1A);
  - Sanctioned electrical load (in kW), supply phase (Single-Phase or Three-Phase), and distribution circle;
  - Historical bi-monthly electricity bills, meter reading values, units consumed (kWh), and dues/payment status.
- **STRICT DATA MINIMIZATION NOTICE (Aadhaar & PAN Stripping):** If utility portal queries return sensitive identity numbers such as an **Aadhaar** or **Permanent Account Number (PAN)**, **SolarEase programmatically removes, strips, and discards these fields immediately before saving or caching.** Neither your Aadhaar number nor your PAN is ever stored, cached on your device, or saved in our backend databases.

### 3.3 Geolocation & Address Information
- **Installation Address:** Street address, postal code, and city provided during profile setup to determine rooftop solar feasibility.
- **Device GPS Coordinates (Latitude & Longitude):** With your explicit runtime permission (`ACCESS_COARSE_LOCATION` and `ACCESS_FINE_LOCATION`), we access your device coordinates solely to:
  - Reverse-geocode and autofill your installation address;
  - Calculate precise local solar insolation, Global Horizontal Irradiance (GHI), and regional weather patterns for system sizing;
  - Discover and display verified local solar EPC installers and service providers nearest to your location.
- *You can grant or revoke location permissions at any time through your Android device settings. If revoked, you can enter your address manually.*

### 3.4 Daily Meter Readings & Consumption Logs
- **Manual Meter Entries:** Daily cumulative meter readings (in kWh) entered to track consumption patterns between billing cycles.
- **Meter Photographs:** If you use the camera to photograph your electricity meter for record-keeping.
- **Baseline Calibration Data:** Starting meter readings derived from your latest electricity bill to measure ongoing daily household consumption.

### 3.5 Solar Lens: Appliance Photographs, Videos & AI Audit
When you use the SolarEase Solar Lens feature to catalog household appliances:
- **Room Photographs:** Up to four (4) room photographs captured via device camera or selected from device gallery.
- **Appliance Video Sweeps:** Short video recordings of rooms and electrical appliances (if recorded or uploaded).
- **Extracted Appliance Inventory:** Appliance types (e.g., refrigerator, air conditioner, ceiling fan, washing machine), detected quantities, and readable nameplate power ratings (in Watts).
- *Photographs and videos are transmitted securely to our cloud backend for visual analysis by Google Gemini AI vision models to extract visible appliance inventories and estimate connected load. Media files are handled in accordance with Section 7.*

### 3.6 Solar Sizing & Technical Assessment Preferences
- **Rooftop Characteristics:** Available rooftop area (in square feet or square meters), roof mounting type (flat concrete roof, sloping tin sheet, elevated structure).
- **System Preferences:** Chosen solar system architecture (On-Grid, Off-Grid, or Hybrid), customer category (Residential, Commercial, Institutional), and preference for DCR (Domestic Content Requirement) vs. Non-DCR solar modules.

### 3.7 Vendor Inquiries, Site Surveys & Order Details
- **Survey & Quote Requests:** When you request a physical site survey or formal quotation, we process your contact name, phone number, installation address, selected system capacity, and preferred survey dates to transmit to the selected solar EPC vendor.
- **Order & Receipt Data:** Order identifiers, selected hardware packages, advance/token payment receipts, and generated PDF invoices.

### 3.8 Technical, Device & Log Data
- **Device Identifiers & Diagnostics:** Android OS version, device model (e.g., HONOR 200 Pro, Vivo), screen resolution, app version code, network type, IP address, and application crash reports collected through Firebase Crashlytics to diagnose performance and installation compatibility.

---

## 4. Lawful Grounds and Purposes of Processing

Under Section 6 and Section 7 of the DPDP Act, 2023, we process your personal data under the lawful grounds of **User Consent** and **Legitimate Uses**, for the following specific purposes:

| Category of Personal Data | Specific Lawful Purpose of Processing |
| :--- | :--- |
| **Mobile Number & OTP** | Account creation, identity verification, multi-factor authentication, and critical security alerts. |
| **Google Sign-In Profile** | Seamless user authentication and session management via Firebase Authentication. |
| **EB Service Number & Bill Records** | Retrieving utility tariffs, analyzing historical energy consumption, and establishing baseline usage. |
| **GPS Location & House Address** | Estimating local solar insolation (GHI), optimizing solar module sizing, and identifying nearby solar installers. |
| **Daily Meter Readings** | Calibrating daily household energy usage, identifying peak load patterns, and projecting upcoming utility bills. |
| **Solar Lens Photos & Videos** | AI-driven computer vision detection of visible appliances, calculating connected load (kW), and daytime vs. nighttime profiles. |
| **Rooftop & System Preferences** | Generating customized solar PV capacity recommendations, cost-benefit analyses, and PM Surya Ghar subsidy estimates. |
| **Vendor Requests & Orders** | Transmitting site survey requests and quote specifications to authorized third-party solar EPC contractors chosen by you. |
| **Device Diagnostics & Crash Logs** | Ensuring application stability, debugging native library compatibility (including 16 KB page-size support), and security defense. |

---

## 5. Device Permissions Requested by the Android App

The SolarEase Android app requests runtime permissions strictly aligned with app functionality. We do not access permissions without your explicit prompt:

1. **`android.permission.CAMERA`:**  
   *Purpose:* To enable you to capture photographs or video sweeps of rooms and appliances in Solar Lens, or to capture electricity meter readings.  
   *Optionality:* Marked with `required=false` in the manifest. You may upload photos from your device gallery instead of taking new ones.
2. **`android.permission.ACCESS_FINE_LOCATION` & `ACCESS_COARSE_LOCATION`:**  
   *Purpose:* To identify your installation rooftop coordinates for solar irradiance simulation and local vendor matching.  
   *Optionality:* Marked with `required=false` in the manifest. If denied, you can manually type your installation address and city.
3. **`android.permission.INTERNET`:**  
   *Purpose:* Mandatory network access to communicate with the SolarEase cloud backend, retrieve utility bills, process Solar Lens AI audits, and synchronize account settings.

---

## 6. How Your Data Is Processed, Shared & Disclosed

We do not sell, rent, monetize, or trade your personal data. We disclose your data strictly to authorized Data Processors and third-party partners as described below:

### 6.1 Cloud Hosting and Infrastructure Providers
- **Google Cloud Platform (GCP) / Cloud Run:** Our backend services, database instances (PostgreSQL / Cloud SQL), and encrypted file stores are hosted on Google Cloud servers located in **[PLACEHOLDER: Cloud Server Region, e.g., Mumbai / Delhi, India]**.
- **Firebase Services (Google LLC):** We use Firebase Authentication for identity token verification and Firebase App Check to protect our backend APIs against abuse and reverse-engineering.

### 6.2 AI Processing Providers (Solar Lens)
- **Google Gemini API / Vertex AI:** When you perform a Solar Lens audit, uploaded appliance photographs or video frames are transmitted via secure HTTPS endpoints to Google's generative AI vision models (`gemini-2.5-flash` / `gemini-3.8-flash`) to identify visible electrical appliances, model labels, and nameplate power ratings.
- Google processes these media files under enterprise data protection terms. Media uploaded via API endpoints is not used to train Google's public foundation models without permission.

### 6.3 Third-Party Solar EPC Installers & Vendors
- If and only when you explicitly submit a **Site Survey Request** or **Request a Quote** on the Platform, we share your contact name, verified mobile number, installation address, rooftop area, and preliminary solar sizing recommendation with the specific independent solar installer or EPC vendor selected by you.
- Vendors are independent third parties and are bound by confidentiality obligations to use your contact details solely for contacting you regarding the requested solar installation.

### 6.4 Third-Party Financing & Banking Partners
- If you voluntarily apply for a solar financing loan or EMI scheme through partner banks or Non-Banking Financial Companies (NBFCs) featured on the Platform, your application details will be transferred directly to the designated financial institution upon your explicit instruction.

### 6.5 Statutory & Legal Disclosures
- We may disclose your personal data if required to do so by applicable Indian law, regulation, subpoena, court order, or written directive from law enforcement, tax authorities, or regulatory bodies (such as the Data Protection Board of India or DISCOM authorities).

---

## 7. Data Security and On-Device Protection

We implement rigorous technical, organizational, and physical security measures to safeguard your personal data:

1. **Hardware-Backed On-Device Encryption:** Sensitive session tokens (JWT access and refresh tokens), cached TNEB bill records, and meter baseline data are stored on your Android device using **`EncryptedSharedPreferences`** backed by the **Android Keystore system** utilizing 256-bit AES-GCM encryption.
2. **Encryption in Transit:** All communications between the SolarEase Android app, our backend APIs, Google Cloud services, and third-party utility portals are encrypted using **Transport Layer Security (TLS 1.2 / TLS 1.3)** with HTTPS. Plaintext HTTP connections are strictly prohibited.
3. **App Check & Rate Limiting:** Backend endpoints (including Solar Lens `/lens/analyze` and Chatbot `/chatbot/query`) are protected by Firebase App Check device attestation and strict IP/user rate limiting to prevent unauthorized scraping or automated abuse.
4. **Data Minimization in Code:** Automated stripping of sensitive national identifiers (Aadhaar, PAN) ensures our systems never store data beyond what is functionally required.

---

## 8. Data Retention and Deletion

1. **Active Account Data:** We retain your account credentials, installation address, utility records, and meter reading logs for as long as your account remains active on the Platform to provide continuous solar tracking and historical comparisons.
2. **Solar Lens Temporary Media:** Photographs and video files uploaded for Solar Lens analysis are retained in cloud storage for a maximum of **[PLACEHOLDER: Media Retention Period, e.g., 30 days / 90 days]** following analysis completion, after which the raw image files are permanently purged, while the structured text inventory of appliances (e.g., "1x Refrigerator 250W") is retained in your profile.
3. **Account Deletion:** You may request deletion of your account and associated personal data at any time via the in-app Profile screen or by sending a written request to **[PLACEHOLDER: Deletion Request Email, e.g., privacy@solarease.in]**. Upon receiving a verified request:
   - Your personal identity data, utility bills, meter readings, and appliance records will be permanently erased from our active databases within **[PLACEHOLDER: Deletion Timeline, e.g., 30 days]**.
   - Residual copies in encrypted system backups will be permanently overwritten in accordance with standard backup rotation cycles (not exceeding **[PLACEHOLDER: Backup Purge Timeline, e.g., 60 days]**).
   - We may retain anonymized, de-identified telemetry data (such as aggregated solar generation statistics without personal identifiers) for research and algorithm improvement.

---

## 9. Your Rights as a Data Principal

Under the Digital Personal Data Protection Act, 2023, you are entitled to the following statutory rights regarding your personal data:

1. **Right to Access Information:** You have the right to obtain a summary of personal data being processed by us, the processing activities carried out, and the identities of all third parties with whom your data has been shared.
2. **Right to Correction and Updating:** You have the right to correct, complete, and update inaccurate, incomplete, or out-of-date personal data stored in your profile (such as correcting your installation address or appliance inventory).
3. **Right to Erasure (Right to be Forgotten):** You have the right to request the deletion or erasure of your personal data where the purpose for which it was collected is no longer served or upon withdrawing your consent.
4. **Right to Withdraw Consent:** Where processing is based on your consent, you have the right to withdraw your consent at any time. Withdrawal of consent does not affect the lawfulness of processing carried out prior to withdrawal. If you withdraw consent for essential items (such as mobile number or location), you may no longer be able to use related features of the App.
5. **Right of Grievance Redressal:** You have the right to have your data protection complaints resolved through our Grievance Redressal Officer (detailed in Section 11) within statutory timeframes.
6. **Right to Nominate:** You have the right to nominate any other individual who, in the event of your death or incapacity, shall exercise your rights as a Data Principal.

To exercise any of these rights, please submit a written request to our Data Protection & Grievance Officer using the contact details in Section 11.

---

## 10. Children’s Privacy

The SolarEase Platform is intended strictly for adults who manage residential or commercial utility accounts and property. We do not knowingly collect, solicit, or process personal data from children under the age of eighteen (18) years, nor do we engage in behavioral tracking or targeted advertising directed at children, in compliance with Section 9 of the DPDP Act, 2023. If you believe that a child has provided us with personal data without parental consent, please contact our Grievance Officer immediately, and we will take prompt steps to delete such information.

---

## 11. Grievance Redressal Officer & Contact Information

In compliance with Section 19 of the Digital Personal Data Protection Act, 2023, and Rule 5(9) of the Information Technology (SPDI) Rules, 2011, the Company has designated a dedicated **Grievance Redressal Officer** to address privacy inquiries, data subject rights requests, and data protection concerns:

- **Name of Grievance Officer:** [PLACEHOLDER: Full Name of Designated Grievance Officer, e.g., Mr. Vignesh Sundaram]
- **Designation:** Grievance Officer & Head of Data Protection
- **Entity Name:** [PLACEHOLDER: Legal Business Entity Name, e.g., SolarEase Technologies Private Limited]
- **Postal Address:** [PLACEHOLDER: Physical Corporate Office Address in India, with Pin Code]
- **Email Address:** [PLACEHOLDER: Designated Grievance Email, e.g., grievance@solarease.in or privacy@solarease.in]
- **Telephone Helpline:** [PLACEHOLDER: Dedicated Telephone Number, India]
- **Office Hours:** Monday to Friday, 10:00 AM to 6:00 PM IST (excluding statutory holidays)

### Grievance Escalation
We are committed to acknowledging your privacy complaint within **[PLACEHOLDER: Acknowledgment Timeline, e.g., 48 hours]** and providing a resolution within **[PLACEHOLDER: Resolution Timeline, e.g., 30 calendar days]**. If your grievance is not resolved satisfactorily by our Grievance Officer within thirty (30) days, you may have the right to file a complaint before the **Data Protection Board of India** pursuant to the DPDP Act, 2023.

---

## 12. Updates and Amendments to this Privacy Policy

We may periodically update or amend this Privacy Policy to reflect enhancements in the SolarEase application, changes in our cloud infrastructure, or updates to Indian data protection laws and judicial precedents.

When modifications are made:
- We will update the **"Last Updated"** date at the top of this document;
- In the event of material changes affecting your rights or how your personal data is processed, we will provide conspicuous advance notice through an in-app modal dialogue, push notification, or email before the revised policy takes effect;
- Your continued use of the Platform after the effective date of an amended Privacy Policy constitutes your informed consent to the updated terms.

---

## Owner Review Checklist (Privacy Policy)

Before finalizing and publishing this Privacy Policy, the business owner and legal counsel must resolve the following questions and decisions:

- [ ] **1. Legal Entity & Officer Appointment:** Fill in the confirmed corporate legal entity name, corporate address, and designate the specific individual serving as the statutory Grievance Officer with verified email and phone contact details.
- [ ] **2. Cloud Storage Region:** Verify the exact Google Cloud Platform (GCP) data center region where user data and database instances reside (e.g., `asia-south1` / Mumbai or `asia-south2` / Delhi) to ensure data localization requirements under applicable regulations.
- [ ] **3. Solar Lens Media Retention Policy:** Establish an explicit business policy on how long room photographs and video sweeps are stored on Google Cloud Storage after AI inference (e.g., deleted immediately after processing, retained for 30 days for user re-audits, or kept until user account deletion).
- [ ] **4. Third-Party Vendor Agreements:** Ensure that Data Processing Agreements (DPAs) or vendor contracts with solar EPC installers and financial institutions include strict data confidentiality clauses prohibiting them from using customer contact information for unrelated telemarketing or selling leads to fourth parties.
- [ ] **5. In-App Consent Architecture:** Ensure that the first-time onboarding screen displays a clear consent checkbox linking to both the Privacy Policy and Terms & Conditions, and that user consent timestamps are recorded in the backend database as required by the DPDP Act, 2023.
- [ ] **6. In-App Account Deletion Flow:** Verify that the Android App provides an easily accessible "Delete My Account & Data" button within the Profile or Settings screen to comply with Google Play Developer Policy and Section 12 of the DPDP Act, 2023.

---
*End of Privacy Policy Draft.*
