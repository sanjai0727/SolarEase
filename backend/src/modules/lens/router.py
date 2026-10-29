from fastapi import APIRouter, Depends, HTTPException, UploadFile, File, Form, status
from sqlalchemy.orm import Session
from typing import List
import uuid

from src.core.database import get_db
from src.modules.auth.dependencies import get_verified_firebase_token, get_current_user, verify_app_check
from src.modules.auth.models import User
from src.modules.auth.repository import AuthRepository
from src.core.ratelimit import rate_limit_lens
from .schemas import ApplianceCreate, ApplianceUpdate, ApplianceResponse, DetectionResult
from .service import ApplianceService, ApplianceAnalysisService

router = APIRouter(prefix="/lens", tags=["lens"])

@router.post("/appliances", response_model=ApplianceResponse)
def create_appliance(
    appliance_in: ApplianceCreate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    return ApplianceService.create_appliance(db, current_user.id, appliance_in)

@router.get("/appliances", response_model=List[ApplianceResponse])
def get_appliances(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    return ApplianceService.get_appliances(db, current_user.id)

@router.get("/appliances/{appliance_id}", response_model=ApplianceResponse)
def get_appliance(
    appliance_id: uuid.UUID,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    return ApplianceService.get_appliance(db, current_user.id, appliance_id)

@router.patch("/appliances/{appliance_id}", response_model=ApplianceResponse)
def update_appliance(
    appliance_id: uuid.UUID,
    appliance_in: ApplianceUpdate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    return ApplianceService.update_appliance(db, current_user.id, appliance_id, appliance_in)

@router.delete("/appliances/{appliance_id}", status_code=204)
def delete_appliance(
    appliance_id: uuid.UUID,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    ApplianceService.delete_appliance(db, current_user.id, appliance_id)

MAX_IMAGE_SIZE = 10 * 1024 * 1024  # 10MB
MAX_VIDEO_SIZE = 25 * 1024 * 1024  # 25MB

@router.post("/analyze", response_model=DetectionResult)
async def analyze_appliance_media(
    image: UploadFile = File(...),
    source: str = Form("PHOTO"),
    _firebase_token: dict = Depends(get_verified_firebase_token),
    _app_check: dict | None = Depends(verify_app_check),
    _rate_limit: None = Depends(rate_limit_lens),
):
    content_type = image.content_type or ""
    if not (content_type.startswith("image/") or content_type.startswith("video/")):
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST, 
            detail=f"Unsupported file type: {content_type}. Expected image/* or video/*"
        )

    max_limit = MAX_VIDEO_SIZE if content_type.startswith("video/") else MAX_IMAGE_SIZE

    # Read in bounded chunks to prevent memory exhaustion
    chunks = []
    total_bytes = 0
    while True:
        chunk = await image.read(64 * 1024)
        if not chunk:
            break
        total_bytes += len(chunk)
        if total_bytes > max_limit:
            limit_mb = max_limit // (1024 * 1024)
            raise HTTPException(
                status_code=status.HTTP_413_REQUEST_ENTITY_TOO_LARGE,
                detail=f"Uploaded media exceeds maximum permitted size of {limit_mb}MB."
            )
        chunks.append(chunk)

    content = b"".join(chunks)
    return await ApplianceAnalysisService.analyze_media(content, content_type, source)
