from sqlalchemy.orm import Mapped, mapped_column, relationship
from sqlalchemy import String, Integer, Float, ForeignKey
from src.core.database import Base
from src.core.mixins import UUIDMixin, TimestampMixin
import uuid

class Appliance(Base, UUIDMixin, TimestampMixin):
    __tablename__ = "appliances"

    user_id: Mapped[uuid.UUID] = mapped_column(ForeignKey("users.id"), index=True, nullable=False)
    appliance_name: Mapped[str] = mapped_column(String(255), nullable=False)
    appliance_type: Mapped[str] = mapped_column(String(255), nullable=False)
    power_rating_watts: Mapped[float] = mapped_column(Float, nullable=False)
    daily_usage_hours: Mapped[float] = mapped_column(Float, nullable=False)
    quantity: Mapped[int] = mapped_column(Integer, default=1, nullable=False)
    monthly_consumption_kwh: Mapped[float] = mapped_column(Float, nullable=False)
    estimated_monthly_cost: Mapped[float] = mapped_column(Float, nullable=False)

    # Optional relationship if needed
    # user = relationship("User")
