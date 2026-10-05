from fastapi import APIRouter, Depends, Query, status
from sqlalchemy.orm import Session
from typing import List, Optional

from src.core.database import get_db
from src.modules.auth.dependencies import get_current_user
from src.modules.auth.models import User
from .schemas import (
    BaselineCreate,
    BaselineResponse,
    MeterReadingCreate,
    MeterReadingUpdate,
    MeterReadingResponse,
    CurrentCycleConsumptionResponse,
    EstimatedBillResponse,
    GraphResponse
)
from .service import MeterService

router = APIRouter(prefix="/meter", tags=["meter"])

@router.post("/baseline", response_model=BaselineResponse)
def set_baseline(
    data: BaselineCreate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """
    Sets or updates the baseline meter reading and date from the connected TNEB bill.
    If a new billing cycle is provided, previous cycles are archived.
    """
    return MeterService.set_or_update_baseline(db, current_user.id, data)


@router.get("/baseline/current", response_model=Optional[BaselineResponse])
def get_current_baseline(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """
    Returns the currently active billing cycle baseline.
    """
    return MeterService.get_active_baseline(db, current_user.id)


@router.post("/readings", response_model=MeterReadingResponse, status_code=status.HTTP_201_CREATED)
def submit_reading(
    data: MeterReadingCreate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """
    Submits a daily meter reading. Validates numeric value, non-negative,
    not lower than baseline, not lower than previous day, and checks for duplicates.
    """
    return MeterService.submit_reading(db, current_user.id, data)


@router.get("/readings/today", response_model=Optional[MeterReadingResponse])
def get_today_reading(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """
    Returns today's recorded meter reading if already entered.
    """
    return MeterService.get_today_reading(db, current_user.id)


@router.put("/readings/today", response_model=MeterReadingResponse)
def update_today_reading(
    data: MeterReadingUpdate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """
    Safely edits today's recorded meter reading.
    """
    return MeterService.update_today_reading(db, current_user.id, data)


@router.get("/readings", response_model=List[MeterReadingResponse])
def get_readings(
    cycle_id: Optional[str] = Query(None, description="Optional cycle id to filter"),
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """
    Retrieves all recorded meter readings for the current or specified billing cycle.
    """
    return MeterService.get_readings(db, current_user.id, cycle_id)


@router.get("/consumption/current", response_model=CurrentCycleConsumptionResponse)
def get_current_consumption(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """
    Calculates and returns current-cycle consumption metrics:
    units consumed since last bill, days elapsed, avg kWh/day, estimated bill.
    """
    return MeterService.get_current_consumption(db, current_user.id)


@router.get("/bill/estimate", response_model=EstimatedBillResponse)
def get_estimated_bill(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """
    Calculates estimated bill using exact TNEB LT-1A Domestic tariff slabs.
    """
    return MeterService.get_estimated_bill(db, current_user.id)


@router.get("/graph", response_model=GraphResponse)
def get_graph_data(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """
    Returns time-series meter reading data for the dashboard graph.
    """
    return MeterService.get_graph_data(db, current_user.id)
