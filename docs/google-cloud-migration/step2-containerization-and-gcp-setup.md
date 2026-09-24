# Step 2: Containerization & Google Cloud Architecture Setup

This document details the containerization and Google Cloud target architecture for the SolarEase backend migration from Render to Google Cloud.

---

## 1. Containerization Architecture

### Dockerfile (`antigravity-backend/Dockerfile`)
The backend is packaged into a slim, secure, multi-stage production container:
- **Base Image:** `python:3.11-slim`
- **System Packages:** `ca-certificates` (secure TLS trust bundle) and `curl` (for container healthcheck probes).
- **Dependency Caching:** `requirements.txt` is installed before copying source code, ensuring efficient layer caching.
- **Port Binding:** Automatically reads the dynamic `$PORT` environment variable injected by Google Cloud Run (defaults to `8080`).
- **Healthcheck Probe:** Built-in `HEALTHCHECK` pinging `http://localhost:${PORT:-8080}/healthz`.
- **Startup Command:** `exec uvicorn src.main:app --host 0.0.0.0 --port ${PORT:-8080} --workers 1`

### Docker Security (`antigravity-backend/.dockerignore`)
Guarantees that sensitive and unnecessary files never enter the build context:
- `.env` and `.env.*`
- `firebase-adminsdk.json` and all `*service-account*.json` files
- `venv/`, `.venv/`, and Python cache artifacts (`__pycache__`, `*.pyc`)
- Local debug dumps and scratch logs (`alembic_out.txt`, `inspect_output.txt`)

---

## 2. Google Cloud Target Architecture

| Component | Target Service | Configuration / Specs |
| :--- | :--- | :--- |
| **Project** | Google Cloud Project | `solarease-bf39c` (matches Firebase project) |
| **Region** | `asia-south1` (Mumbai) | Lowest latency for Indian domestic users (~20ms) |
| **Compute** | Google Cloud Run (Fully Managed) | 1 vCPU, 1 GiB RAM (required for in-memory NumPy vector embeddings), 300s timeout |
| **Registry** | Google Artifact Registry | Docker format repository: `solarease-backend` |
| **Database** | Google Cloud SQL for PostgreSQL | PostgreSQL 15 / 16 (Tier: `db-custom-1-3840` or `db-f1-micro`) |
| **Secrets** | Google Cloud Secret Manager | `DATABASE_URL`, `JWT_SECRET_KEY`, `GEMINI_API_KEY`, `OPENROUTER_API_KEY` |
| **Authentication** | Firebase Admin SDK | Application Default Credentials (ADC) / Secret Manager |

```mermaid
graph TD
    subgraph Client
        AndroidApp[SolarEase Mobile App]
    end

    subgraph GoogleCloudPlatform [Google Cloud: solarease-bf39c (asia-south1)]
        CloudRun[Google Cloud Run: solarease-backend<br/>1 vCPU / 1 GiB RAM]
        ArtifactRegistry[Artifact Registry: solarease-backend]
        CloudSQL[(Cloud SQL PostgreSQL 15/16)]
        SecretManager[Secret Manager]
    end

    subgraph ExternalAPIs
        FirebaseAuth[Firebase Auth]
        Gemini[Google Gemini 3.6 & Embeddings]
        OpenRouter[OpenRouter Nemotron Vision]
    end

    AndroidApp -->|HTTPS REST| CloudRun
    CloudRun -->|Unix Socket /cloudsql/...| CloudSQL
    CloudRun -->|Inject at Boot| SecretManager
    CloudRun -->|ADC / Token Verification| FirebaseAuth
    CloudRun -->|Chatbot RAG| Gemini
    CloudRun -->|Appliance Analysis| OpenRouter
    ArtifactRegistry -.->|Image Pull| CloudRun
```

---

## 3. Database Connectivity Strategy

### Cloud Run to Cloud SQL via Unix Domain Socket
Google Cloud Run automatically provides a built-in proxy daemon when Cloud SQL instance is linked.
- **Connection Scheme:**
  ```text
  postgresql+psycopg://<DB_USER>:<DB_PASSWORD>@/<DB_NAME>?host=/cloudsql/<PROJECT_ID>:<REGION>:<INSTANCE_NAME>
  ```
- **Driver:** `psycopg` (v3) natively supports Unix domain sockets via the `host` query parameter.
- **Connection Pooling:** `pool_size=10, max_overflow=20, pool_pre_ping=True` in `src/core/database.py` remains active.

---

## 4. Deployment Execution Guide

### Prerequisites
1. Log in to Google Cloud CLI:
   ```bash
   gcloud auth login
   ```
2. Set the active project:
   ```bash
   gcloud config set project solarease-bf39c
   ```

### Option A: Automated One-Step Deploy (Recommended)
Run the PowerShell or Bash script from `antigravity-backend/`:
- **Windows (PowerShell):**
  ```powershell
  cd antigravity-backend
  .\scripts\deploy_cloud_run.ps1
  ```
- **Linux / macOS / Cloud Shell:**
  ```bash
  cd antigravity-backend
  chmod +x scripts/deploy_cloud_run.sh
  ./scripts/deploy_cloud_run.sh
  ```

### Option B: Cloud Build Direct Trigger
```bash
cd antigravity-backend
gcloud builds submit --config cloudbuild.yaml .
```

---

## 5. Verification & Health Probes

Once deployed, the following endpoints verify operational readiness:
- `GET https://<CLOUD_RUN_URL>/healthz` -> `{"status": "healthy"}`
- `GET https://<CLOUD_RUN_URL>/health` -> `{"status": "ok", "message": "Service is healthy"}`
- `GET https://<CLOUD_RUN_URL>/` -> `{"message": "Welcome to SolarEase AntiGravity Backend"}`
