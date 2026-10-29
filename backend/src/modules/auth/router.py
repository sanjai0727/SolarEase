from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session
import logging

from src.core.database import get_db
from src.modules.auth.schemas import (
    TokenResponse,
    UserResponse,
    UserUpdateRequest,
    RefreshTokenRequest,
    FirebaseTokenRequest,
    ResolveIdentifierRequest,
    ResolveIdentifierResponse,
    CheckUsernameResponse,
)
from src.modules.auth.repository import AuthRepository
from src.modules.auth.service import AuthService
from src.modules.auth.dependencies import get_current_user, get_verified_firebase_token, verify_app_check
from src.modules.auth.models import User
from src.core.security import create_access_token, create_refresh_token

router = APIRouter(prefix="/auth", tags=["auth"])
logger = logging.getLogger("auth_router")


def get_auth_service(
    db: Session = Depends(get_db),
) -> AuthService:
    repository = AuthRepository(db)
    return AuthService(repository)


@router.post(
    "/firebase",
    response_model=TokenResponse
)
async def verify_firebase(
    request: FirebaseTokenRequest,
    firebase_claims: dict = Depends(get_verified_firebase_token),
    service: AuthService = Depends(get_auth_service),
    _app_check: dict | None = Depends(verify_app_check),
):
    firebase_uid = firebase_claims.get("uid")
    phone_number = firebase_claims.get("phone_number")
    email = firebase_claims.get("email")
    provider = firebase_claims.get("firebase", {}).get("sign_in_provider")

    logger.info(f"verify_firebase called for UID: {bool(firebase_uid)}, Provider: {provider}, is_signup: {request.is_signup}")

    if phone_number:
        user = service.sync_phone_user(
            firebase_uid=firebase_uid,
            phone_number=phone_number,
            username=request.username,
            name=request.name,
            is_signup=request.is_signup
        )
    elif email:
        username = request.username
        name = request.name or firebase_claims.get("name")
        user = service.sync_social_user(
            firebase_uid=firebase_uid,
            email=email,
            username=username,
            name=name,
            is_signup=request.is_signup
        )
    else:
        logger.error(f"Firebase token missing phone and email. Claims: {list(firebase_claims.keys())}")
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
    service: AuthService = Depends(get_auth_service),
):
    token_payload = getattr(current_user, "_token_payload", None)
    return service.logout(current_user, token_payload)


@router.get(
    "/me",
    response_model=UserResponse
)
def get_me(
    current_user: User = Depends(get_current_user),
):
    return current_user


@router.patch(
    "/me",
    response_model=UserResponse
)
def update_me(
    request: UserUpdateRequest,
    current_user: User = Depends(get_current_user),
    service: AuthService = Depends(get_auth_service),
):
    return service.update_profile(current_user, request)


@router.post(
    "/resolve-identifier",
    response_model=ResolveIdentifierResponse
)
def resolve_identifier(
    request: ResolveIdentifierRequest,
    service: AuthService = Depends(get_auth_service),
):
    email = service.resolve_identifier(request.identifier)
    return ResolveIdentifierResponse(email=email)


@router.get(
    "/check-username",
    response_model=CheckUsernameResponse
)
def check_username(
    username: str,
    service: AuthService = Depends(get_auth_service),
):
    result = service.check_username(username)
    return CheckUsernameResponse(**result)