from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from src.core.database import get_db

from src.modules.auth.schemas import (
    SendOtpRequest,
    SendOtpResponse,
    VerifyOtpRequest,
    TokenResponse,
    UserResponse,
    RefreshTokenRequest,
    FirebaseTokenRequest,
    FirebaseVerificationResponse,
    FirebaseSyncResponse,
)

from src.modules.auth.repository import AuthRepository
from src.modules.auth.service import AuthService
from src.modules.auth.dependencies import get_current_user, get_verified_firebase_token
from src.modules.auth.models import User
from src.core.security import create_access_token, create_refresh_token


router = APIRouter(prefix="/auth", tags=["auth"])


def get_auth_service(
    db: Session = Depends(get_db),
) -> AuthService:

    repository = AuthRepository(db)

    return AuthService(repository)


@router.post(
    "/send-otp",
    response_model=SendOtpResponse
)
async def send_otp(
    request: SendOtpRequest,
    service: AuthService = Depends(get_auth_service),
):
    return await service.send_otp(request)


@router.post(
    "/verify-otp",
    response_model=TokenResponse
)
async def verify_otp(
    request: VerifyOtpRequest,
    service: AuthService = Depends(get_auth_service),
):
    return await service.verify_otp(request)


@router.post(
    "/firebase",
    response_model=TokenResponse
)
async def verify_firebase(
    firebase_claims: dict = Depends(get_verified_firebase_token),
    service: AuthService = Depends(get_auth_service),
):
    import logging
    logger = logging.getLogger("auth_router")
    
    firebase_uid = firebase_claims.get("uid")
    phone_number = firebase_claims.get("phone_number")
    email = firebase_claims.get("email")
    provider = firebase_claims.get("firebase", {}).get("sign_in_provider")
    
    logger.info(f"verify_firebase called. UID exists: {bool(firebase_uid)}, Phone: {bool(phone_number)}, Email: {bool(email)}, Provider: {provider}")
    
    if phone_number:
        user = service.sync_phone_user(firebase_uid, phone_number)
    elif email:
        name = firebase_claims.get("name")
        user = service.sync_social_user(firebase_uid, email, full_name=name)
    else:
        logger.error(f"HTTP 400 Bad Request: Firebase token missing both phone number and email. Available claims keys: {list(firebase_claims.keys())}")
        from fastapi import HTTPException, status
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Firebase token must contain either a phone number or an email address."
        )

    access_token = create_access_token(
        user.id,
        user.mobile_number
    )

    refresh_token = create_refresh_token(
        user.id,
        user.mobile_number
    )

    user_response = UserResponse.model_validate(user)

    return TokenResponse(
        access_token=access_token,
        refresh_token=refresh_token,
        user=user_response,
    )


@router.post(
    "/refresh",
    response_model=TokenResponse
)
def refresh_token(
    request: RefreshTokenRequest,
    service: AuthService = Depends(get_auth_service),
):
    return service.refresh_token(request)


@router.post("/logout")
def logout(
    current_user: User = Depends(get_current_user),
):
    return {
        "success": True,
        "message": "Successfully logged out."
    }


@router.get(
    "/me",
    response_model=UserResponse
)
def get_me(
    current_user: User = Depends(get_current_user),
):
    return current_user


@router.get("/session")
def get_session(
    current_user: User = Depends(get_current_user),
):
    return {
        "success": True,
        "message": "Session endpoint placeholder."
    }