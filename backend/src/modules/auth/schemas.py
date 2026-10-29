from pydantic import BaseModel, Field, field_validator, ConfigDict
from uuid import UUID
from datetime import datetime


class SendOtpRequest(BaseModel):
    username: str = Field(
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
    model_config = ConfigDict(from_attributes=True)

    id: UUID
    name: str | None = None
    username: str | None
    email: str | None = None
    mobile_number: str | None
    house_address: str | None = None
    latitude: float | None = None
    longitude: float | None = None
    is_verified: bool
    created_at: datetime


class UserUpdateRequest(BaseModel):
    name: str | None = Field(None, max_length=255)
    username: str | None = Field(None, min_length=3, max_length=30, pattern=r"^[a-zA-Z0-9_]{3,30}$")
    mobile_number: str | None = Field(None, min_length=10, max_length=10, pattern=r"^[0-9]{10}$")
    house_address: str | None = None
    latitude: float | None = None
    longitude: float | None = None

    @field_validator('username')
    @classmethod
    def validate_username_contains_letter(cls, v: str | None) -> str | None:
        if v is not None and not any(char.isalpha() for char in v):
            raise ValueError('Username must contain at least one letter')
        return v


class TokenResponse(BaseModel):
    access_token: str
    refresh_token: str
    token_type: str = "Bearer"
    expires_in: int = 900
    user: UserResponse


class RefreshTokenRequest(BaseModel):
    refresh_token: str


class FirebaseTokenRequest(BaseModel):
    firebase_id_token: str | None = None
    username: str | None = None
    name: str | None = None
    is_signup: bool = False


class FirebaseVerificationResponse(BaseModel):
    firebase_uid: str
    phone_number: str | None = None


class FirebaseSyncResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: UUID
    firebase_uid: str
    email: str | None = None
    mobile_number: str | None
    is_verified: bool


class ResolveIdentifierRequest(BaseModel):
    identifier: str = Field(..., min_length=1, max_length=255)


class ResolveIdentifierResponse(BaseModel):
    email: str


class CheckUsernameResponse(BaseModel):
    available: bool
    message: str