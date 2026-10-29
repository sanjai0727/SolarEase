# SolarEase: Proxmox-First Hybrid Cloud Architecture Blueprint
**Target: Enterprise Resilience with Near-$0/Month Cloud Spend**  
**Role:** Senior Principal DevOps & Platform Architect  
**Scope:** Android Client (`app`), FastAPI Backend (`backend`), PostgreSQL, Server-Side Gemini API, Vector DB, and Automated Workflows.

---

## 1. Executive Strategy & Cost Optimization Architecture

SolarEase is an end-to-end residential solar adoption platform consisting of:
1. **Client**: Android App (Jetpack Compose, Kotlin, Firebase Auth, GeoTagging).
2. **Backend**: FastAPI modular monolith (`auth`, `assessment`, `survey`, `documents`, `orders`, `installation`, `notifications`, `support`).
3. **Data**: PostgreSQL (relational), KYC & site survey media (unstructured), and Solar Subsidy & Tariff knowledge (vector).
4. **AI & Intelligence**: Server-side Gemini 1.5 API (Bill OCR, rooftop inspection, subsidy matching).

### The Inverted Hybrid Cloud Philosophy
Instead of hosting compute in Google Cloud and paying hundreds of dollars in idle baseline fees (Cloud SQL, GKE, Load Balancers, Cloud VPN), **Proxmox VE runs 100% of steady-state compute, data, state, CI/CD runners, and storage.**

**Google Cloud is strictly maintained at scale-to-zero:**
- **Compute**: Cloud Run (`min-instances = 0`, $0 when idle).
- **Networking**: Cloudflare Free Tier terminates SSL/DDoS; no GCP Cloud Load Balancer ($18+/mo) or Cloud VPN ($30+/mo).
- **AI/ML**: Serverless Gemini 1.5 Flash API (pure pay-per-token, $0 when not called).
- **Storage**: Nightly encrypted differential backups to Google Cloud Storage (GCS) Archive ($0.0012/GB/mo).

```
                             [ Android Client (SolarEase App) ]
                                              │
                                              ▼
                        [ Cloudflare Edge (Free Tier CDN / WAF / DNS) ]
                                              │
                                              ▼
                   [ Cloudflare Tunnel (`cloudflared` - No Public IP) ]
                                              │
                        ┌─────────────────────┴─────────────────────┐
                        │ (99.9% Traffic)                           │ (DR / Outage Failover)
                        ▼                                           ▼
         ┌───────────────────────────────┐           ┌───────────────────────────────┐
         │     PROXMOX VE (Primary)      │           │      GOOGLE CLOUD (GCP)       │
         │  • K3s Cluster on Talos/Debian│           │  • Cloud Run (Min Inst: 0)    │
         │  • FastAPI (SolarEase Monolith│           │  • Serverless Gemini API      │
         │  • CloudNativePG (Postgres)   │           │  • GCS Archive (Disaster Rec) │
         │  • Qdrant (CPU Vector DB)     │           │  • Zero-idle compute cost     │
         │  • MinIO (S3 Docs & Photos)   │           │                               │
         │  • Redis Semantic Cache       │           │                               │
         │  • Temporal / n8n Automation  │           │                               │
         │  • VictoriaMetrics + Grafana  │           │                               │
         │  • GitHub Self-Hosted Runners │           │                               │
         └───────────────────────────────┘           └───────────────────────────────┘
```

---

## 2. Foundation: AI/ML Data & Logic Layer (No Local GPU)

Because local GPU compute is excluded, running AI/ML cost-effectively requires offloading inference to **Serverless Gemini API** while keeping all **data parsing, vector search, semantic caching, and orchestration local on Proxmox**.

### 2.1 Server-Side Gemini API Integration (Pay-As-You-Go)
- **Model**: `gemini-1.5-flash` via Google AI Studio / Vertex AI pay-per-token API.
  - Slashes costs: ~$0.075 per 1M input tokens.
  - Used in SolarEase for:
    - **EB Tariff Bill OCR**: Extracting consumer number, monthly kWh units, and sanctioned load from electricity bill uploads.
    - **Rooftop Assessment**: Analyzing uploaded rooftop images for shade, obstacles, and structure type.
    - **Subsidy Q&A Assistant**: Chatbot in the `support` module answering customer queries on PM Surya Ghar Muft Bijli Yojana.
- **Cost Guardrails**: Hard spending caps set in Google Cloud Billing ($10/month hard alert).

### 2.2 Local Vector Database (Qdrant on K3s)
- **Engine**: **Qdrant** (Rust-based, low CPU and RAM footprint) or **pgvector** inside PostgreSQL.
- **Embeddings**: Generated using **FastEmbed** or **ONNX Runtime** (`bge-small-en-v1.5`) directly on Proxmox CPU.
  - Zero cloud API cost to vectorize solar subsidy documents, state policies, and technician manuals.
- **Storage**: Persistent Volumes mapped to Proxmox NVMe ZFS storage pools for <5ms semantic retrieval.

### 2.3 Semantic Caching with Redis
- SolarEase customers repeatedly ask the same subsidy and panel payback questions.
- A local **Redis** instance caches vector similarity matches (>95% threshold).
- **Result**: Up to 65% of customer support queries are answered instantly from the local cache without initiating an outbound Gemini API call.

### 2.4 Document & Photo Storage (MinIO)
- Customer KYC, rooftop photos, installation receipts, and quotation PDFs are stored in self-hosted **MinIO** (S3-compatible API).
- Configured with ZFS RAIDZ or 2-way mirror storage on Proxmox.
- Replaces AWS S3 / Cloud Storage standard pricing ($0.026/GB/mo + egress).

---

## 3. Platform Layer: Cloud, Edge, Containers & Storage

### 3.1 Edge Ingress via Cloudflare Zero Trust (Eliminating Static IP & VPN Costs)
- **No Port Forwarding**: Proxmox nodes run the `cloudflared` daemon in Kubernetes.
- The tunnel creates secure outbound-only TLS connections to Cloudflare edge nodes.
- **Features**: Free SSL/TLS termination, HTTP/3, DDoS mitigation, and edge caching for static assets.

### 3.2 Container Orchestration: K3s on Proxmox
- Deploy a 3-node lightweight **K3s** cluster (or single multi-core control+worker VM with ZFS snapshots for simple setups).
- OS: **Ubuntu 24.04 LTS Minimal** or **Talos Linux** (immutable, API-driven).
- Ingress Controller: **Traefik** (built into K3s) for path-based routing (`/api/v1/*` to FastAPI, `/storage/*` to MinIO).

### 3.3 Database Plane: PostgreSQL High Availability
- Deploy PostgreSQL using **CloudNativePG** operator inside K3s.
- Features:
  - Automatic failover, streaming replication between Proxmox storage pools.
  - Point-In-Time-Recovery (PITR).
  - Nightly compressed logical and physical WAL archiving directly to MinIO and GCS Coldline.

---

## 4. Build & Integrate: API & Workflow Automation

SolarEase relies on multi-step workflows spanning customer action, technician dispatch, and government subsidies.

### 4.1 Modular Monolith Service Mapping
The `backend` modules map cleanly to internal event streams:

```
[ Customer Submits Bill / Roof Photo ]
                │
                ▼
      [ FastAPI: `survey` ] ──► [ MinIO Storage ]
                │
                ▼ (Asynchronous Event)
      [ Temporal / n8n Workflow Engine ]
        ├── Step 1: Run OCR / Inspection via Gemini 1.5 Flash API
        ├── Step 2: Calculate solar capacity & savings in `assessment`
        ├── Step 3: Check eligibility in Qdrant (Subsidy Policy RAG)
        ├── Step 4: Write quote to PostgreSQL (`orders`)
        └── Step 5: Send push notification to Android app (`notifications`)
```

### 4.2 Workflow Engine (Temporal or n8n)
- **Temporal.io** or self-hosted **n8n** running on K3s.
- Guarantees reliability: If the external Gemini API encounters rate-limits, Temporal retries with exponential backoff without dropping customer data.

---

## 5. DevOps: CI/CD, Observability & Disaster Recovery

### 5.1 Self-Hosted CI/CD Runners (Proxmox-Powered)
- Deploy **GitHub Actions Runner Controller (ARC)** or a persistent runner VM on Proxmox.
- **Android App CI (`/app`)**:
  - Compiles Kotlin code, runs unit tests, and packages release APK/AAB files using local multi-core CPU and Gradle cache.
  - Saves 100% of GitHub billable minutes for large Android builds.
- **Backend CI (`/backend`)**:
  - Runs linting (`ruff`), Alembic migrations verification, and builds Docker container images.
  - Pushes images to a local **Harbor** container registry on Proxmox (or Google Artifact Registry under free tier limits).

### 5.2 Observability Stack
- **Metrics**: **Prometheus** scraping K3s, FastAPI (`prometheus-fastapi-instrumentator`), and Proxmox Node Exporter, stored in **VictoriaMetrics** (super-efficient retention).
- **Dashboards**: **Grafana** showing API response times, active database connections, and memory consumption.
- **Logs**: **Vector.dev** shipping structured JSON logs from FastAPI to local **Grafana Loki**.
- **Alerts**: Alerts routed to Telegram or Slack for node downtime, high memory, or database connection pool exhaustion.

### 5.3 Near-$0 GCP Disaster Recovery & Scale-to-Zero Strategy
1. **Fallback Compute (Cloud Run)**:
   - Deploy `solarease-backend` container to **Google Cloud Run**.
   - Parameters:
     - `min-instances = 0`
     - `max-instances = 5`
     - `memory = 512Mi`, `cpu = 1`
   - When no traffic is routed to Cloud Run, **monthly cost is $0.00**.
2. **Automated Cloudflare Health Check Routing**:
   - Cloudflare monitors `https://api.yourdomain.com/health`.
   - If the Proxmox home/colo connection goes down (power/ISP outage), Cloudflare automatically routes traffic to Cloud Run.
3. **Database & Asset Backup**:
   - `pgBackRest` or `rclone` runs an automated nightly differential backup of PostgreSQL and MinIO to **Google Cloud Storage (Archive Tier)**.
   - 50 GB of archived backup costs **~$0.06/month**.

---

## 6. Implementation Runbook & Code Artifacts

### 6.1 Backend Production Containerization (`Dockerfile`)
Place in `backend/Dockerfile`:

```dockerfile
# Multi-stage build for minimal attack surface and small image size
FROM python:3.13-slim AS builder

WORKDIR /app
RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential libpq-dev && \
    rm -rf /var/lib/apt/lists/*

COPY requirements.txt .
RUN pip install --no-cache-dir --user -r requirements.txt

FROM python:3.13-slim AS runner

WORKDIR /app
RUN apt-get update && apt-get install -y --no-install-recommends \
    libpq5 curl && \
    rm -rf /var/lib/apt/lists/*

# Copy installed wheels from builder
COPY --from=builder /root/.local /root/.local
ENV PATH=/root/.local/bin:$PATH

COPY . .

# Run as non-root user for security
RUN useradd -m appuser && chown -R appuser:appuser /app
USER appuser

EXPOSE 8000
HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
  CMD curl -f http://localhost:8000/health || exit 1

CMD ["uvicorn", "src.main:app", "--host", "0.0.0.0", "--port", "8000", "--workers", "2"]
```

---

### 6.2 Proxmox K3s Core Stack Deployment (`docker-compose.yml` or K8s Manifest)

For single VM evaluation or migration to K8s:

```yaml
version: "3.8"

services:
  solarease-db:
    image: postgres:16-alpine
    container_name: solarease-postgres
    restart: unless-stopped
    environment:
      POSTGRES_DB: solarease
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes:
      - /zfs/data/postgres:/var/lib/postgresql/data
    ports:
      - "127.0.0.1:5432:5432"

  solarease-redis:
    image: redis:7-alpine
    container_name: solarease-redis
    restart: unless-stopped
    command: redis-server --appendonly yes
    volumes:
      - /zfs/data/redis:/data

  solarease-minio:
    image: minio/minio:latest
    container_name: solarease-minio
    restart: unless-stopped
    command: server /data --console-address ":9001"
    environment:
      MINIO_ROOT_USER: ${MINIO_ROOT_USER}
      MINIO_ROOT_PASSWORD: ${MINIO_ROOT_PASSWORD}
    volumes:
      - /zfs/data/minio:/data
    ports:
      - "127.0.0.1:9000:9000"
      - "127.0.0.1:9001:9001"

  solarease-qdrant:
    image: qdrant/qdrant:v1.9.0
    container_name: solarease-qdrant
    restart: unless-stopped
    volumes:
      - /zfs/data/qdrant:/qdrant/storage
    ports:
      - "127.0.0.1:6333:6333"

  solarease-backend:
    build:
      context: ./backend
      dockerfile: Dockerfile
    container_name: solarease-backend
    restart: unless-stopped
    environment:
      DATABASE_URL: "postgresql+psycopg://postgres:${DB_PASSWORD}@solarease-db:5432/solarease"
      REDIS_URL: "redis://solarease-redis:6379/0"
      QDRANT_URL: "http://solarease-qdrant:6333"
      MINIO_ENDPOINT: "http://solarease-minio:9000"
      GEMINI_API_KEY: ${GEMINI_API_KEY}
      JWT_SECRET_KEY: ${JWT_SECRET_KEY}
    depends_on:
      - solarease-db
      - solarease-redis
      - solarease-qdrant
      - solarease-minio
    ports:
      - "127.0.0.1:8000:8000"

  cloudflared:
    image: cloudflare/cloudflared:latest
    container_name: solarease-tunnel
    restart: unless-stopped
    command: tunnel run
    environment:
      TUNNEL_TOKEN: ${CLOUDFLARE_TUNNEL_TOKEN}
    depends_on:
      - solarease-backend
```

---

### 6.3 Cloudflare Tunnel Configuration (`config.yml`)

```yaml
tunnel: solarease-hybrid-tunnel
credentials-file: /etc/cloudflared/credentials.json

ingress:
  # Route API requests to FastAPI backend
  - hostname: api.solarease.yourdomain.com
    service: http://solarease-backend:8000
  # Route S3 asset requests to MinIO (for customer KYC & photos)
  - hostname: storage.solarease.yourdomain.com
    service: http://solarease-minio:9000
  # Catch-all
  - service: http_status:404
```

---

### 6.4 Near-$0 GCP Cloud Run Terraform Definition (`main.tf`)

```hcl
terraform {
  required_providers {
    google = {
      source  = "hashicorp/google"
      version = "~> 5.0"
    }
  }
}

provider "google" {
  project = var.gcp_project_id
  region  = "us-central1"
}

# Scale-to-zero Fallback Service
resource "google_cloud_run_v2_service" "solarease_fallback" {
  name     = "solarease-api-fallback"
  location = "us-central1"

  template {
    scaling {
      min_instance_count = 0  # CRITICAL: 0 idle cost
      max_instance_count = 5
    }

    containers {
      image = "gcr.io/${var.gcp_project_id}/solarease-backend:latest"
      resources {
        limits = {
          cpu    = "1000m"
          memory = "512Mi"
        }
      }
      env {
        name  = "ENVIRONMENT"
        value = "production-fallback"
      }
      env {
        name  = "GEMINI_API_KEY"
        value = var.gemini_api_key
      }
    }
  }
}

# Coldline Disaster Recovery Bucket
resource "google_storage_bucket" "dr_backups" {
  name          = "${var.gcp_project_id}-solarease-dr-backups"
  location      = "US"
  storage_class = "ARCHIVE" # Lowest cost tier ($0.0012/GB/month)

  lifecycle_rule {
    condition {
      age = 90
    }
    action {
      type = "Delete"
    }
  }
}
```

---

### 6.5 Nightly Database & Media Backup to GCS Archive (`backup.sh`)

```bash
#!/usr/bin/env bash
set -euo pipefail

TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
BACKUP_DIR="/zfs/backups/solarease"
GCS_BUCKET="gs://your-project-solarease-dr-backups"

mkdir -p "${BACKUP_DIR}"

echo "[1/3] Dumping PostgreSQL database..."
docker exec solarease-postgres pg_dump -U postgres -Fc solarease | gzip > "${BACKUP_DIR}/db_${TIMESTAMP}.dump.gz"

echo "[2/3] Syncing MinIO media storage snapshot..."
rclone sync /zfs/data/minio/solarease-assets "${BACKUP_DIR}/media_snapshot"

echo "[3/3] Uploading compressed archive to Google Cloud Storage (Archive)..."
gsutil cp "${BACKUP_DIR}/db_${TIMESTAMP}.dump.gz" "${GCS_BUCKET}/db/"
gsutil -m rclone sync "${BACKUP_DIR}/media_snapshot" "${GCS_BUCKET}/media/"

# Retention: Delete local backups older than 7 days
find "${BACKUP_DIR}" -type f -name "*.gz" -mtime +7 -delete

echo "Backup complete. Cloud costs for this run: < $0.001."
```

---

## 7. Cost Comparison Matrix

| Infrastructure Layer | Standard All-Cloud Architecture | SolarEase Proxmox-First Architecture |
| :--- | :--- | :--- |
| **Compute & API** | Cloud Run / GKE 24/7 ($75 - $150/mo) | **$0** (Proxmox K3s VM) |
| **Database** | GCP Cloud SQL for Postgres ($60 - $120/mo) | **$0** (PostgreSQL on Proxmox ZFS NVMe) |
| **Object Storage** | Cloud Storage Standard ($20/mo) | **$0** (MinIO Local NVMe) |
| **Ingress & Networking** | GCP Load Balancer + Static IP ($25/mo) | **$0** (Cloudflare Tunnel + Free CDN) |
| **Vector DB** | Managed Pinecone / Zilliz ($70/mo) | **$0** (Qdrant on K3s CPU) |
| **CI/CD Build Minutes** | GitHub Actions paid minutes ($30/mo) | **$0** (Self-hosted runner on Proxmox) |
| **AI Processing** | Persistent Vertex AI Model ($200+/mo) | **~$1 - $3/mo** (Gemini 1.5 Flash Pay-per-token) |
| **Disaster Recovery** | Cross-region multi-cloud ($50/mo) | **~$0.10/mo** (GCS Archive Storage) |
| **Total Estimated Spend** | **$530 - $700 / month** | **~$1.10 - $3.50 / month** |

---

## 8. Summary Checklist for Deployment

1. [ ] **Proxmox**: Provision a Debian/Ubuntu or Talos VM with 8 vCPUs, 16 GB RAM, and ZFS-backed NVMe storage.
2. [ ] **Stack Deploy**: Launch `docker-compose.yml` or K3s manifests for PostgreSQL, Redis, Qdrant, MinIO, and FastAPI.
3. [ ] **Ingress**: Authenticate `cloudflared` to route `api.solarease.yourdomain.com` securely without router port forwarding.
4. [ ] **AI Config**: Configure `GEMINI_API_KEY` for server-side OCR and conversational solar support.
5. [ ] **CI/CD**: Connect GitHub Actions self-hosted runner to the SolarEase repo to build Android APKs and backend images locally.
6. [ ] **GCP Cloud Run**: Deploy the container with `min-instances: 0` as the emergency failover target.
7. [ ] **Disaster Recovery**: Schedule the cron job for `backup.sh` to mirror encrypted backups to GCS Archive.
