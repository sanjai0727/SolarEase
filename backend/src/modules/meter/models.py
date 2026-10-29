import uuid
from datetime import date
from sqlalchemy.orm import Mapped, mapped_column, relationship
from sqlalchemy import String, Float, ForeignKey, Boolean, Date, UniqueConstraint, Index
from src.core.database import Base
from src.core.mixins import UUIDMixin, TimestampMixin

class BillingCycle(Base, UUIDMixin, TimestampMixin):
    __tablename__ = "billing_cycles"

    user_id: Mapped[uuid.UUID] = mapped_column(ForeignKey("users.id"), index=True, nullable=False)
    cycle_id: Mapped[str] = mapped_column(String(100), index=True, nullable=False)
    baseline_date: Mapped[date] = mapped_column(Date, nullable=False)
    baseline_reading: Mapped[float] = mapped_column(Float, nullable=False)
    previous_billed_units: Mapped[float | None] = mapped_column(Float, nullable=True)
    tariff_code: Mapped[str | None] = mapped_column(String(100), nullable=True, default="LT-1A")
    service_connection_number: Mapped[str | None] = mapped_column(String(50), nullable=True)
    is_active: Mapped[bool] = mapped_column(Boolean, default=True)

    readings: Mapped[list["MeterReading"]] = relationship("MeterReading", back_populates="cycle_rel", cascade="all, delete-orphan")

    __table_args__ = (
        Index("ix_billing_cycles_user_is_active", "user_id", "is_active"),
    )


class MeterReading(Base, UUIDMixin, TimestampMixin):
    __tablename__ = "meter_readings"

    user_id: Mapped[uuid.UUID] = mapped_column(ForeignKey("users.id"), index=True, nullable=False)
    billing_cycle_id: Mapped[str] = mapped_column(String(100), index=True, nullable=False)
    reading_value: Mapped[float] = mapped_column(Float, nullable=False)
    reading_date: Mapped[date] = mapped_column(Date, nullable=False, index=True)
    daily_consumption: Mapped[float | None] = mapped_column(Float, nullable=True)
    notes: Mapped[str | None] = mapped_column(String(255), nullable=True)

    cycle_db_id: Mapped[uuid.UUID | None] = mapped_column(ForeignKey("billing_cycles.id"), nullable=True)
    cycle_rel: Mapped[BillingCycle | None] = relationship("BillingCycle", back_populates="readings")

    __table_args__ = (
        UniqueConstraint("user_id", "billing_cycle_id", "reading_date", name="uq_user_cycle_reading_date"),
    )
