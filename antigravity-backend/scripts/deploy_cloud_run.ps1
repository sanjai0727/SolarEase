# ==============================================================================
# SolarEase - Cloud Run Deployment Script (PowerShell)
# Target Project: solarease-bf39c
# Region: asia-south1 (Mumbai)
# ==============================================================================

param (
    [string]$ProjectId = "solarease-bf39c",
    [string]$Region = "asia-south1",
    [string]$ServiceName = "solarease-backend"
)

Write-Host "==========================================" -ForegroundColor Cyan
Write-Host " SolarEase Google Cloud Run Deployment    " -ForegroundColor Cyan
Write-Host " Project: $ProjectId                      " -ForegroundColor Cyan
Write-Host " Region:  $Region                         " -ForegroundColor Cyan
Write-Host " Service: $ServiceName                    " -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan

# 1. Set active project
Write-Host "`n[1/5] Setting gcloud project to $ProjectId..." -ForegroundColor Yellow
gcloud config set project $ProjectId

# 2. Enable Required APIs
Write-Host "`n[2/5] Enabling necessary Google Cloud APIs..." -ForegroundColor Yellow
gcloud services enable `
    run.googleapis.com `
    artifactregistry.googleapis.com `
    cloudbuild.googleapis.com `
    secretmanager.googleapis.com `
    sqladmin.googleapis.com

# 3. Create Artifact Registry repo if it doesn't exist
Write-Host "`n[3/5] Ensuring Artifact Registry repository exists..." -ForegroundColor Yellow
gcloud artifacts repositories describe $ServiceName --location=$Region 2>$null
if ($LASTEXITCODE -ne 0) {
    Write-Host "Creating repository $ServiceName in $Region..." -ForegroundColor Green
    gcloud artifacts repositories create $ServiceName `
        --repository-format=docker `
        --location=$Region `
        --description="Docker repository for SolarEase backend"
}

# 4. Deploy directly via Cloud Build source deploy
Write-Host "`n[4/5] Building and deploying to Cloud Run..." -ForegroundColor Yellow
gcloud run deploy $ServiceName `
    --source . `
    --region $Region `
    --platform managed `
    --allow-unauthenticated `
    --memory 1Gi `
    --cpu 1 `
    --timeout 300s `
    --set-env-vars "ENVIRONMENT=production,DEBUG=False,PROJECT_NAME=SolarEase Backend"

# 5. Retrieve deployed URL
Write-Host "`n[5/5] Retrieving Cloud Run URL..." -ForegroundColor Yellow
$serviceUrl = gcloud run services describe $ServiceName --region=$Region --format="value(status.url)"
Write-Host "`nService deployed successfully!" -ForegroundColor Green
Write-Host "Deployed URL: $serviceUrl" -ForegroundColor Cyan
Write-Host "Health Check: $serviceUrl/healthz" -ForegroundColor Cyan
