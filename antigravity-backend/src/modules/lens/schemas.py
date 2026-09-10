from pydantic import BaseModel, Field, ConfigDict
from typing import Optional
from datetime import datetime
import uuid

class ApplianceBase(BaseModel):
    appliance_name: str = Field(..., max_length=255)
    appliance_type: str = Field(..., max_length=255)
    power_rating_watts: float = Field(..., gt=0)
    daily_usage_hours: float = Field(..., ge=0, le=24)
    quantity: int = Field(default=1, ge=1)
    monthly_consumption_kwh: float = Field(..., ge=0)
    estimated_monthly_cost: float = Field(..., ge=0)

class ApplianceCreate(ApplianceBase):
    pass

class ApplianceUpdate(BaseModel):
    appliance_name: Optional[str] = Field(None, max_length=255)
    appliance_type: Optional[str] = Field(None, max_length=255)
    power_rating_watts: Optional[float] = Field(None, gt=0)
    daily_usage_hours: Optional[float] = Field(None, ge=0, le=24)
    quantity: Optional[int] = Field(None, ge=1)
    monthly_consumption_kwh: Optional[float] = Field(None, ge=0)
    estimated_monthly_cost: Optional[float] = Field(None, ge=0)

class ApplianceResponse(ApplianceBase):
    id: uuid.UUID
    user_id: uuid.UUID
    created_at: datetime
    updated_at: datetime

    model_config = ConfigDict(from_attributes=True)

class BoundingBox(BaseModel):
    left: float
    top: float
    right: float
    bottom: float

class Detection(BaseModel):
    id: str
    label: str
    confidence: float
    source: str
    bounding_box: BoundingBox

class DetectionResult(BaseModel):
    detections: list[Detection]
