from datetime import date, datetime
from typing import List, Optional
import uuid
from fastapi import HTTPException, status
from sqlalchemy.orm import Session
from sqlalchemy import select, and_, desc, update

from .models import BillingCycle, MeterReading
from .schemas import (
    BaselineCreate,
    MeterReadingCreate,
    MeterReadingUpdate,
    CurrentCycleConsumptionResponse,
    EstimatedBillResponse,
    GraphResponse,
    GraphDataPoint
)
from .calculator import calculate_tneb_bill

class MeterService:

    @staticmethod
    def set_or_update_baseline(db: Session, user_id: uuid.UUID, data: BaselineCreate) -> BillingCycle:
        # Check existing active cycle
        existing = db.execute(
            select(BillingCycle).where(
                and_(BillingCycle.user_id == user_id, BillingCycle.is_active == True)
            )
        ).scalar_one_or_none()

        if existing:
            # If same cycle and reading, return as-is or update details
            if existing.cycle_id == data.cycle_id and abs(existing.baseline_reading - data.baseline_reading) < 0.001:
                existing.tariff_code = data.tariff_code or existing.tariff_code
                existing.service_connection_number = data.service_connection_number or existing.service_connection_number
                if data.previous_billed_units is not None:
                    existing.previous_billed_units = data.previous_billed_units
                db.commit()
                db.refresh(existing)
                return existing
            else:
                # Deactivate all existing active cycles atomically
                db.execute(
                    update(BillingCycle).where(
                        and_(BillingCycle.user_id == user_id, BillingCycle.is_active == True)
                    ).values(is_active=False)
                )

        new_cycle = BillingCycle(
            user_id=user_id,
            cycle_id=data.cycle_id,
            baseline_date=data.baseline_date,
            baseline_reading=data.baseline_reading,
            previous_billed_units=data.previous_billed_units,
            tariff_code=data.tariff_code or "LT-1A",
            service_connection_number=data.service_connection_number,
            is_active=True
        )
        db.add(new_cycle)
        db.commit()
        db.refresh(new_cycle)
        return new_cycle

    @staticmethod
    def get_active_baseline(db: Session, user_id: uuid.UUID) -> Optional[BillingCycle]:
        return db.execute(
            select(BillingCycle).where(
                and_(BillingCycle.user_id == user_id, BillingCycle.is_active == True)
            )
        ).scalar_one_or_none()

    @staticmethod
    def submit_reading(db: Session, user_id: uuid.UUID, data: MeterReadingCreate) -> MeterReading:
        baseline = MeterService.get_active_baseline(db, user_id)
        if not baseline:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="No active billing cycle found. Please connect your TNEB bill first."
            )

        reading_date = data.reading_date or date.today()

        if reading_date < baseline.baseline_date:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail=f"Reading date ({reading_date.isoformat()}) cannot be earlier than baseline date ({baseline.baseline_date.isoformat()})."
            )

        if reading_date > date.today():
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail=f"Reading date ({reading_date.isoformat()}) cannot be in the future."
            )

        if data.reading_value < 0:
            raise HTTPException(
                status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
                detail="Meter reading cannot be negative."
            )

        if data.reading_value < baseline.baseline_reading:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail=f"Current reading ({data.reading_value:,.1f}) cannot be lower than previous bill baseline ({baseline.baseline_reading:,.1f})."
            )

        # Check if reading already exists for this date in this cycle
        existing = db.execute(
            select(MeterReading).where(
                and_(
                    MeterReading.user_id == user_id,
                    MeterReading.billing_cycle_id == baseline.cycle_id,
                    MeterReading.reading_date == reading_date
                )
            )
        ).scalar_one_or_none()

        if existing:
            raise HTTPException(
                status_code=status.HTTP_409_CONFLICT,
                detail=f"A meter reading for {reading_date.isoformat()} already exists ({existing.reading_value:,.1f} kWh). Use the edit option to update today's reading."
            )

        # Get latest prior reading to calculate daily consumption and validate monotonic increase
        prior_reading = db.execute(
            select(MeterReading).where(
                and_(
                    MeterReading.user_id == user_id,
                    MeterReading.billing_cycle_id == baseline.cycle_id,
                    MeterReading.reading_date < reading_date
                )
            ).order_by(desc(MeterReading.reading_date))
        ).scalars().first()

        prev_value = prior_reading.reading_value if prior_reading else baseline.baseline_reading

        if data.reading_value < prev_value:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail=f"Meter reading ({data.reading_value:,.1f}) cannot decrease below the last recorded reading ({prev_value:,.1f})."
            )

        daily_kwh = max(0.0, data.reading_value - prev_value)

        reading = MeterReading(
            user_id=user_id,
            billing_cycle_id=baseline.cycle_id,
            reading_value=data.reading_value,
            reading_date=reading_date,
            daily_consumption=daily_kwh,
            notes=data.notes,
            cycle_db_id=baseline.id
        )
        db.add(reading)
        db.commit()
        db.refresh(reading)
        return reading

    @staticmethod
    def update_reading_by_date(db: Session, user_id: uuid.UUID, target_date: date, data: MeterReadingUpdate) -> MeterReading:
        baseline = MeterService.get_active_baseline(db, user_id)
        if not baseline:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="No active billing cycle found."
            )

        if target_date < baseline.baseline_date:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail=f"Reading date ({target_date.isoformat()}) cannot be earlier than baseline date ({baseline.baseline_date.isoformat()})."
            )

        if target_date > date.today():
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail=f"Reading date ({target_date.isoformat()}) cannot be in the future."
            )

        reading = db.execute(
            select(MeterReading).where(
                and_(
                    MeterReading.user_id == user_id,
                    MeterReading.billing_cycle_id == baseline.cycle_id,
                    MeterReading.reading_date == target_date
                )
            )
        ).scalar_one_or_none()

        if not reading:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail=f"No meter reading recorded for {target_date.isoformat()} to update."
            )

        if data.reading_value < 0:
            raise HTTPException(
                status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
                detail="Meter reading cannot be negative."
            )

        if data.reading_value < baseline.baseline_reading:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail=f"Updated reading cannot be lower than baseline ({baseline.baseline_reading:,.1f})."
            )

        # Check against previous reading
        prior_reading = db.execute(
            select(MeterReading).where(
                and_(
                    MeterReading.user_id == user_id,
                    MeterReading.billing_cycle_id == baseline.cycle_id,
                    MeterReading.reading_date < target_date
                )
            ).order_by(desc(MeterReading.reading_date))
        ).scalars().first()

        prev_value = prior_reading.reading_value if prior_reading else baseline.baseline_reading
        if data.reading_value < prev_value:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail=f"Updated reading ({data.reading_value:,.1f}) cannot be lower than previous reading ({prev_value:,.1f})."
            )

        reading.reading_value = data.reading_value
        reading.daily_consumption = max(0.0, data.reading_value - prev_value)
        if data.notes is not None:
            reading.notes = data.notes

        MeterService._recompute_cycle_consumptions(db, user_id, baseline)
        db.commit()
        db.refresh(reading)
        return reading

    @staticmethod
    def update_today_reading(db: Session, user_id: uuid.UUID, data: MeterReadingUpdate) -> MeterReading:
        return MeterService.update_reading_by_date(db, user_id, date.today(), data)

    @staticmethod
    def _recompute_cycle_consumptions(db: Session, user_id: uuid.UUID, baseline: BillingCycle):
        readings = db.execute(
            select(MeterReading).where(
                and_(
                    MeterReading.user_id == user_id,
                    MeterReading.billing_cycle_id == baseline.cycle_id
                )
            ).order_by(MeterReading.reading_date)
        ).scalars().all()

        prev_val = baseline.baseline_reading
        for r in readings:
            r.daily_consumption = max(0.0, r.reading_value - prev_val)
            prev_val = r.reading_value

    @staticmethod
    def get_today_reading(db: Session, user_id: uuid.UUID) -> Optional[MeterReading]:
        baseline = MeterService.get_active_baseline(db, user_id)
        if not baseline:
            return None

        return db.execute(
            select(MeterReading).where(
                and_(
                    MeterReading.user_id == user_id,
                    MeterReading.billing_cycle_id == baseline.cycle_id,
                    MeterReading.reading_date == date.today()
                )
            )
        ).scalar_one_or_none()

    @staticmethod
    def get_readings(db: Session, user_id: uuid.UUID, cycle_id: Optional[str] = None) -> List[MeterReading]:
        query = select(MeterReading).where(MeterReading.user_id == user_id)
        if cycle_id:
            query = query.where(MeterReading.billing_cycle_id == cycle_id)
        else:
            baseline = MeterService.get_active_baseline(db, user_id)
            if baseline:
                query = query.where(MeterReading.billing_cycle_id == baseline.cycle_id)

        query = query.order_by(MeterReading.reading_date.asc())
        return list(db.execute(query).scalars().all())

    @staticmethod
    def get_current_consumption(db: Session, user_id: uuid.UUID) -> CurrentCycleConsumptionResponse:
        baseline = MeterService.get_active_baseline(db, user_id)
        if not baseline:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="No active billing cycle found. Please connect your TNEB bill first."
            )

        latest_reading = db.execute(
            select(MeterReading).where(
                and_(
                    MeterReading.user_id == user_id,
                    MeterReading.billing_cycle_id == baseline.cycle_id
                )
            ).order_by(desc(MeterReading.reading_date))
        ).scalars().first()

        today_reading = MeterService.get_today_reading(db, user_id)
        is_today_logged = today_reading is not None

        if latest_reading:
            units_consumed = max(0.0, latest_reading.reading_value - baseline.baseline_reading)
            latest_val = latest_reading.reading_value
            latest_dt = latest_reading.reading_date
            days_elapsed = max(1, (latest_dt - baseline.baseline_date).days)
        else:
            units_consumed = 0.0
            latest_val = None
            latest_dt = None
            days_elapsed = max(1, (date.today() - baseline.baseline_date).days)

        avg_daily = round(units_consumed / days_elapsed, 1)
        tariff = baseline.tariff_code or "LT-1A"
        bill_calc = calculate_tneb_bill(units_consumed, tariff_code=tariff)

        return CurrentCycleConsumptionResponse(
            billing_cycle_id=baseline.cycle_id,
            baseline_reading=baseline.baseline_reading,
            baseline_date=baseline.baseline_date,
            latest_reading=latest_val,
            latest_reading_date=latest_dt,
            units_consumed=round(units_consumed, 1),
            days_elapsed=days_elapsed,
            avg_units_per_day=avg_daily,
            estimated_bill=bill_calc["total_estimated_bill"],
            is_today_logged=is_today_logged,
            today_reading=today_reading.reading_value if today_reading else None,
            tariff_code=tariff
        )

    @staticmethod
    def get_estimated_bill(db: Session, user_id: uuid.UUID) -> EstimatedBillResponse:
        consumption = MeterService.get_current_consumption(db, user_id)
        bill_dict = calculate_tneb_bill(consumption.units_consumed, tariff_code=consumption.tariff_code or "LT-1A")
        return EstimatedBillResponse(
            units=bill_dict["units"],
            free_units=bill_dict["free_units"],
            chargeable_units=bill_dict["chargeable_units"],
            energy_charges=bill_dict["energy_charges"],
            fixed_charges=bill_dict["fixed_charges"],
            electricity_tax=bill_dict["electricity_tax"],
            total_estimated_bill=bill_dict["total_estimated_bill"],
            breakdown=bill_dict["breakdown"]
        )

    @staticmethod
    def get_graph_data(db: Session, user_id: uuid.UUID) -> GraphResponse:
        baseline = MeterService.get_active_baseline(db, user_id)
        if not baseline:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="No active billing cycle found."
            )

        readings = MeterService.get_readings(db, user_id, baseline.cycle_id)

        points: List[GraphDataPoint] = []
        # Add baseline point as starting anchor
        points.append(
            GraphDataPoint(
                date=baseline.baseline_date.strftime("%d %b"),
                reading_value=baseline.baseline_reading,
                units_consumed=0.0,
                daily_units=0.0,
                is_baseline=True
            )
        )

        for r in readings:
            consumed = max(0.0, r.reading_value - baseline.baseline_reading)
            points.append(
                GraphDataPoint(
                    date=r.reading_date.strftime("%d %b"),
                    reading_value=r.reading_value,
                    units_consumed=round(consumed, 1),
                    daily_units=r.daily_consumption,
                    is_baseline=False
                )
            )

        return GraphResponse(
            billing_cycle_id=baseline.cycle_id,
            baseline_reading=baseline.baseline_reading,
            baseline_date=baseline.baseline_date.isoformat(),
            points=points,
            total_points=len(points)
        )
