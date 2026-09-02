from fastapi import APIRouter, Depends, HTTPException, UploadFile, File
from sqlalchemy.orm import Session
from typing import List
import uuid

from src.core.database import get_db
from src.modules.auth.dependencies import get_verified_firebase_token
from src.modules.auth.models import User
from src.modules.auth.repository import AuthRepository

def get_user_from_firebase(
    firebase_claims: dict = Depends(get_verified_firebase_token),
    db: Session = Depends(get_db)
) -> User:
    firebase_uid = firebase_claims.get("uid")
    user = AuthRepository(db).get_user_by_firebase_uid(firebase_uid)
    if not user:
        raise HTTPException(status_code=404, detail="User not found.")
    return user
from .schemas import ApplianceCreate, ApplianceUpdate, ApplianceResponse, AnalysisResponse
from .service import ApplianceService, ApplianceAnalysisService

router = APIRouter(prefix="/lens", tags=["lens"])

@router.post("/appliances", response_model=ApplianceResponse)
def create_appliance(
    appliance_in: ApplianceCreate,
    current_user: User = Depends(get_user_from_firebase),
    db: Session = Depends(get_db)
):
    return ApplianceService.create_appliance(db, current_user.id, appliance_in)

@router.get("/appliances", response_model=List[ApplianceResponse])
def get_appliances(
    current_user: User = Depends(get_user_from_firebase),
    db: Session = Depends(get_db)
):
    return ApplianceService.get_appliances(db, current_user.id)

@router.get("/appliances/{appliance_id}", response_model=ApplianceResponse)
def get_appliance(
    appliance_id: uuid.UUID,
    current_user: User = Depends(get_user_from_firebase),
    db: Session = Depends(get_db)
):
    return ApplianceService.get_appliance(db, current_user.id, appliance_id)

@router.patch("/appliances/{appliance_id}", response_model=ApplianceResponse)
def update_appliance(
    appliance_id: uuid.UUID,
    appliance_in: ApplianceUpdate,
    current_user: User = Depends(get_user_from_firebase),
    db: Session = Depends(get_db)
):
    return ApplianceService.update_appliance(db, current_user.id, appliance_id, appliance_in)

@router.delete("/appliances/{appliance_id}", status_code=204)
def delete_appliance(
    appliance_id: uuid.UUID,
    current_user: User = Depends(get_user_from_firebase),
    db: Session = Depends(get_db)
):
    ApplianceService.delete_appliance(db, current_user.id, appliance_id)

@router.post("/analyze", response_model=AnalysisResponse)
async def analyze_appliance_image(
    image: UploadFile = File(...),
    current_user: User = Depends(get_user_from_firebase)
):
    if not image.content_type.startswith("image/"):
        raise HTTPException(status_code=400, detail="File provided is not an image")
    
    content = await image.read()
    return await ApplianceAnalysisService.analyze_image(content, image.content_type)
