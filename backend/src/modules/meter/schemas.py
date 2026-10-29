from pydantic import BaseModel, Field
from datetime import date, datetime
from typing import Optional, List
import uuid

class BaselineCreate(BaseModel):
    cycle_id: str = Field(..., description="Unique billing cycle identifier, e.g. BILL-20260901 or 2026-09")
    baseline_date: date = Field(..., description="Date of the previous bill baseline")
    baseline_reading: float = Field(..., ge=0.0, description="Meter reading at the close of previous bill")
    previous_billed_units: Optional[float] = Field(None, ge=0.0, description="Historical units billed in previous cycle")
    tariff_code: Optional[str] = Field("LT-1A", description="Tariff code")
    service_connection_number: Optional[str] = Field(None, description="TNEB consumer / service connection number")


class BaselineResponse(BaseModel):
    id: uuid.UUID
    cycle_id: str
    baseline_date: date
    baseline_reading: float
    previous_billed_units: Optional[float] = None
    tariff_code: Optional[str] = None
    service_connection_number: Optional[str] = None
    is_active: bool
    created_at: datetime
    updated_at: datetime

    class Config:
        from_attributes = True


class MeterReadingCreate(BaseModel):
    reading_value: float = Field(..., ge=0.0, description="Current cumulative meter reading in kWh")
    reading_date: Optional[date] = Field(None, description="Date of reading (defaults to today)")
    notes: Optional[str] = Field(None, max_length=255)


class MeterReadingUpdate(BaseModel):
    reading_value: float = Field(..., ge=0.0, description="Updated cumulative meter reading in kWh")
    notes: Optional[str] = Field(None, max_length=255)


class MeterReadingResponse(BaseModel):
    id: uuid.UUID
    billing_cycle_id: str
    reading_value: float
    reading_date: date
    daily_consumption: Optional[float] = None
    notes: Optional[str] = None
    created_at: datetime

    class Config:
        from_attributes = True


class SlabBreakdownItem(BaseModel):
    from_unit: float
    to_unit: float
    units_in_slab: float
    rate_per_unit: float
    amount: float


class EstimatedBillResponse(BaseModel):
    units: float
    free_units: float
    chargeable_units: float
    energy_charges: float
    fixed_charges: float = 0.0
    electricity_tax: float = 0.0
    total_estimated_bill: float
    breakdown: List[SlabBreakdownItem]


class CurrentCycleConsumptionResponse(BaseModel):
    billing_cycle_id: str
    baseline_reading: float
    baseline_date: date
    latest_reading: Optional[float] = None
    latest_reading_date: Optional[date] = None
    units_consumed: float
    days_elapsed: int
    avg_units_per_day: float
    estimated_bill: float
    is_today_logged: bool
    today_reading: Optional[float] = None
    tariff_code: Optional[str] = "LT-1A"


class GraphDataPoint(BaseModel):
    date: str
    reading_value: float
    units_consumed: float
    daily_units: Optional[float] = None
    is_baseline: bool = False


class GraphResponse(BaseModel):
    billing_cycle_id: str
    baseline_reading: float
    baseline_date: str
    points: List[GraphDataPoint]
    total_points: int
