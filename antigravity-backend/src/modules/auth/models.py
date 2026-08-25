from sqlalchemy.orm import Mapped, mapped_column
from sqlalchemy import String, Boolean, DateTime
from src.core.database import Base
from src.core.mixins import UUIDMixin, TimestampMixin

class User(Base, UUIDMixin, TimestampMixin):
    __tablename__ = "users"

    mobile_number: Mapped[str | None] = mapped_column(String(10), unique=True, index=True, nullable=True)
    email: Mapped[str | None] = mapped_column(String(255), unique=True, index=True, nullable=True)
    firebase_uid: Mapped[str | None] = mapped_column(String(255), unique=True, index=True, nullable=True)
    name: Mapped[str | None] = mapped_column(String(255), nullable=True)
    username: Mapped[str | None] = mapped_column(String(255), unique=True, nullable=True)
    house_address: Mapped[str | None] = mapped_column(String(500), nullable=True)
    latitude: Mapped[float | None] = mapped_column(nullable=True)
    longitude: Mapped[float | None] = mapped_column(nullable=True)
    is_verified: Mapped[bool] = mapped_column(Boolean, default=False)

class OTPVerification(Base, UUIDMixin, TimestampMixin):
    __tablename__ = "otp_verifications"

    mobile_number: Mapped[str] = mapped_column(String(10), index=True, nullable=False)
    otp_hash: Mapped[str] = mapped_column(String, nullable=False)
    expires_at: Mapped[DateTime] = mapped_column(DateTime(timezone=True), nullable=False)
    attempt_count: Mapped[int] = mapped_column(default=0)
    verified: Mapped[bool] = mapped_column(Boolean, default=False)
