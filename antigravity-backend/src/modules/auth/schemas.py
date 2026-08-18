from pydantic import BaseModel, Field
from uuid import UUID
from datetime import datetime


class SendOtpRequest(BaseModel):
    full_name: str = Field(
        ...,
        min_length=2,
        max_length=100
    )

    mobile_number: str = Field(
        ...,
        min_length=10,
        max_length=10,
        pattern=r"^[0-9]{10}$"
    )


class SendOtpResponse(BaseModel):
    success: bool
    message: str


class VerifyOtpRequest(BaseModel):
    access_token: str = Field(..., min_length=1)


class UserResponse(BaseModel):
    id: UUID
    full_name: str | None
    email: str | None = None
    mobile_number: str | None
    is_verified: bool
    created_at: datetime

    class Config:
        from_attributes = True


class TokenResponse(BaseModel):
    access_token: str
    refresh_token: str
    token_type: str = "Bearer"
    expires_in: int = 900
    user: UserResponse


class RefreshTokenRequest(BaseModel):
    refresh_token: str


class FirebaseTokenRequest(BaseModel):
    firebase_id_token: str = Field(..., min_length=1)


class FirebaseVerificationResponse(BaseModel):
    firebase_uid: str
    phone_number: str | None = None


class FirebaseSyncResponse(BaseModel):
    id: UUID
    firebase_uid: str
    email: str | None = None
    mobile_number: str | None
    is_verified: bool
    
    class Config:
        from_attributes = True