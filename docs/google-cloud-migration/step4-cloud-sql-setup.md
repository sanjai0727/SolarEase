# Step 4: Cloud SQL Setup & Database Migration Report

This document records the setup of Google Cloud SQL for PostgreSQL and the execution of the Alembic migration chain for the SolarEase backend.

---

## 1. Cloud SQL Instance Specifications

| Parameter | Configuration |
| :--- | :--- |
| **Instance ID** | `solarease-postgres` |
| **GCP Project** | `solarease-bf39c` |
| **Region / Zone** | `asia-south1-c` (Mumbai) |
| **Database Engine** | PostgreSQL 16 (`POSTGRES_16`) |
| **Cloud SQL Edition** | Enterprise |
| **Machine Tier** | `db-f1-micro` (Shared Core, 614.4 MiB RAM) |
| **Storage** | 10 GiB SSD (Auto-expanding) |
| **Primary Public IP** | `34.47.205.122` |
| **Connection Name** | `solarease-bf39c:asia-south1:solarease-postgres` |
| **Database Name** | `solarease` |
| **Application User** | `solarease_app` |

---

## 2. Alembic Migration Execution

The complete linear 8-step migration chain was executed against the new Cloud SQL database:

```text
INFO  [alembic.runtime.migration] Context impl PostgresqlImpl.
INFO  [alembic.runtime.migration] Will assume transactional DDL.
INFO  [alembic.runtime.migration] Running upgrade  -> 1234567890ab, create_users_table
INFO  [alembic.runtime.migration] Running upgrade 1234567890ab -> 2345678901bc, create_otp_verifications_table
INFO  [alembic.runtime.migration] Running upgrade 2345678901bc -> 345678901cde, Add firebase_uid to users
INFO  [alembic.runtime.migration] Running upgrade 345678901cde -> c335b955cd61, Add email and make mobile_number nullable
INFO  [alembic.runtime.migration] Running upgrade c335b955cd61 -> 9f120ed4f6b6, Rename full_name to username
INFO  [alembic.runtime.migration] Running upgrade 9f120ed4f6b6 -> 6934c1f034cb, Add house_address to users
INFO  [alembic.runtime.migration] Running upgrade 6934c1f034cb -> ffad56f9be91, add_profile_fields_and_username_unique
INFO  [alembic.runtime.migration] Running upgrade ffad56f9be91 -> 1686b036f6fc, Add lens module appliances table
```

### Verified Schema & Head
- **Active Alembic Head:** `1686b036f6fc`
- **Tables Verified in Cloud SQL:**
  1. `users` (12 columns: `id [uuid]`, `mobile_number`, `username`, `is_verified`, `created_at`, `updated_at`, `firebase_uid`, `email`, `house_address`, `name`, `latitude`, `longitude`)
  2. `appliances` (11 columns: `id [uuid]`, `user_id [uuid FK]`, `appliance_name`, `appliance_type`, `power_rating_watts`, `daily_usage_hours`, `quantity`, `monthly_consumption_kwh`, `estimated_monthly_cost`, `created_at`, `updated_at`)
  3. `otp_verifications` (8 columns: `id [uuid]`, `mobile_number`, `otp_hash`, `expires_at`, `attempt_count`, `verified`, `created_at`, `updated_at`)
  4. `alembic_version` (`version_num = '1686b036f6fc'`)

---

## 3. Google Cloud Run Integration

### Cloud SQL Unix Domain Socket Connection
- Cloud Run service `solarease-backend` was updated to attach the Cloud SQL instance:
  ```bash
  gcloud run services update solarease-backend \
      --region=asia-south1 \
      --add-cloudsql-instances=solarease-bf39c:asia-south1:solarease-postgres \
      --project=solarease-bf39c
  ```
- **Socket Path:** `/cloudsql/solarease-bf39c:asia-south1:solarease-postgres`
- **Database Connection URI:**
  ```text
  postgresql+psycopg://solarease_app:<PASSWORD>@/solarease?host=/cloudsql/solarease-bf39c:asia-south1:solarease-postgres
  ```
- **Deployed Cloud Run Revision:** `solarease-backend-00002-fsn` (Serving 100% traffic)
- **IAM Permission:** Granted `roles/cloudsql.client` to Cloud Run service account `612667385200-compute@developer.gserviceaccount.com`.

---

## 4. End-to-End Verification

A live database read/write transaction was performed through Cloud Run via the Unix socket:
1. `POST https://solarease-backend-612667385200.asia-south1.run.app/auth/send-otp`  
   -> **Status 200 OK**: `{"success": true, "message": "OTP request accepted. Pending Firebase integration."}`
2. Direct Cloud SQL query confirmed row was inserted into `users` (`id: 4703c4fb-b6fc-41e6-83bd-bcd33aedadb4`, `username: gcp_smoke_test`, `created_at: 2026-09-24 20:09:39`).
3. Smoke test user cleaned up; database is in pristine state.

---

## 5. Rollback & Production Preservation

* **Render PostgreSQL Database:** Completely untouched.
* **Android Base URL:** Continues to target `https://solarease-backend.onrender.com/`.
