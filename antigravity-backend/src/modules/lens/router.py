from fastapi import APIRouter, Depends, HTTPException, UploadFile, File, Form
from sqlalchemy.orm import Session
from typing import List
import uuid

from src.core.database import get_db
from src.modules.auth.dependencies import get_verified_firebase_token, get_current_user
from src.modules.auth.models import User
from src.modules.auth.repository import AuthRepository
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

@router.post("/analyze", response_model=DetectionResult)
async def analyze_appliance_image(
    image: UploadFile = File(...),
    source: str = Form("PHOTO"),
    _firebase_token: dict = Depends(get_verified_firebase_token)
):
    import logging
    logger = logging.getLogger("lens_diag")
    logger.info("[LENS DIAG] 1. Lens request entered (Firebase verification succeeded)")
    logger.info(f"[LENS DIAG] 2. Image received: {image.filename}, type: {image.content_type}")

    if not image.content_type.startswith("image/"):
        raise HTTPException(status_code=400, detail="File provided is not an image")
    
    content = await image.read()
    logger.info(f"[LENS DIAG] 3. Image read completed. Byte size: {len(content)}")

    return await ApplianceAnalysisService.analyze_image(content, image.content_type, source)
