#!/usr/bin/env bash
# ==============================================================================
# SolarEase - Cloud Run Deployment Script (Bash)
# Target Project: solarease-bf39c
# Region: asia-south1 (Mumbai)
# ==============================================================================

set -euo pipefail

PROJECT_ID="${1:-solarease-bf39c}"
REGION="${2:-asia-south1}"
SERVICE_NAME="${3:-solarease-backend}"

echo "=========================================="
echo " SolarEase Google Cloud Run Deployment    "
echo " Project: $PROJECT_ID                     "
echo " Region:  $REGION                         "
echo " Service: $SERVICE_NAME                   "
echo "=========================================="

# 1. Set active project
echo -e "\n[1/5] Setting gcloud project to $PROJECT_ID..."
gcloud config set project "$PROJECT_ID"

# 2. Enable Required APIs
echo -e "\n[2/5] Enabling necessary Google Cloud APIs..."
gcloud services enable \
    run.googleapis.com \
    artifactregistry.googleapis.com \
    cloudbuild.googleapis.com \
    secretmanager.googleapis.com \
    sqladmin.googleapis.com

# 3. Create Artifact Registry repository if not exists
echo -e "\n[3/5] Ensuring Artifact Registry repository exists..."
if ! gcloud artifacts repositories describe "$SERVICE_NAME" --location="$REGION" &>/dev/null; then
    echo "Creating repository $SERVICE_NAME in $REGION..."
    gcloud artifacts repositories create "$SERVICE_NAME" \
        --repository-format=docker \
        --location="$REGION" \
        --description="Docker repository for SolarEase backend"
fi

# 4. Deploy directly via Cloud Build source deploy
echo -e "\n[4/5] Building and deploying to Cloud Run..."
gcloud run deploy "$SERVICE_NAME" \
    --source . \
    --region "$REGION" \
    --platform managed \
    --allow-unauthenticated \
    --memory 1Gi \
    --cpu 1 \
    --timeout 300s \
    --set-env-vars "ENVIRONMENT=production,DEBUG=False,PROJECT_NAME=SolarEase Backend"

# 5. Output deployed URL
echo -e "\n[5/5] Retrieving Cloud Run URL..."
SERVICE_URL=$(gcloud run services describe "$SERVICE_NAME" --region="$REGION" --format="value(status.url)")
echo -e "\nService deployed successfully!"
echo "Deployed URL: $SERVICE_URL"
echo "Health Check: $SERVICE_URL/healthz"
