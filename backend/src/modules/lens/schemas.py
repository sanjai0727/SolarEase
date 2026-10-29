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
    left: float = 0.0
    top: float = 0.0
    right: float = 1.0
    bottom: float = 1.0

class DetectedAppliance(BaseModel):
    appliance_type: str
    appliance_name: str
    quantity: int = 1
    power_rating_watts: Optional[float] = None
    confidence: float = 0.9
    evidence: Optional[str] = None

class Detection(BaseModel):
    id: str
    label: str
    confidence: float
    source: str
    bounding_box: BoundingBox
    power_rating_watts: Optional[float] = None
    quantity: Optional[int] = 1
    evidence: Optional[str] = None
    appliance_type: Optional[str] = None
    appliance_name: Optional[str] = None

class DetectionResult(BaseModel):
    detections: list[Detection]
    detected_appliances: Optional[list[DetectedAppliance]] = None

