from fastapi import APIRouter, Depends, HTTPException, UploadFile, File
from sqlalchemy.ext.asyncio import AsyncSession
from typing import List
import uuid

from src.core.database import get_db
from src.modules.auth.dependencies import get_current_user
from src.modules.auth.models import User
from .schemas import ApplianceCreate, ApplianceUpdate, ApplianceResponse, AnalysisResponse
from .service import ApplianceService, ApplianceAnalysisService

router = APIRouter(prefix="/lens", tags=["lens"])

@router.post("/appliances", response_model=ApplianceResponse)
async def create_appliance(
    appliance_in: ApplianceCreate,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db)
):
    return await ApplianceService.create_appliance(db, current_user.id, appliance_in)

@router.get("/appliances", response_model=List[ApplianceResponse])
async def get_appliances(
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db)
):
    return await ApplianceService.get_appliances(db, current_user.id)

@router.get("/appliances/{appliance_id}", response_model=ApplianceResponse)
async def get_appliance(
    appliance_id: uuid.UUID,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db)
):
    return await ApplianceService.get_appliance(db, current_user.id, appliance_id)

@router.patch("/appliances/{appliance_id}", response_model=ApplianceResponse)
async def update_appliance(
    appliance_id: uuid.UUID,
    appliance_in: ApplianceUpdate,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db)
):
    return await ApplianceService.update_appliance(db, current_user.id, appliance_id, appliance_in)

@router.delete("/appliances/{appliance_id}", status_code=204)
async def delete_appliance(
    appliance_id: uuid.UUID,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db)
):
    await ApplianceService.delete_appliance(db, current_user.id, appliance_id)

@router.post("/analyze", response_model=AnalysisResponse)
async def analyze_appliance_image(
    image: UploadFile = File(...),
    current_user: User = Depends(get_current_user)
):
    if not image.content_type.startswith("image/"):
        raise HTTPException(status_code=400, detail="File provided is not an image")
    
    content = await image.read()
    return await ApplianceAnalysisService.analyze_image(content, image.content_type)
