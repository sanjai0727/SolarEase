from fastapi import HTTPException, status
import re

from src.modules.auth.schemas import (
    SendOtpRequest,
    SendOtpResponse,
    VerifyOtpRequest,
    TokenResponse,
    UserResponse,
    RefreshTokenRequest,
)

from src.modules.auth.repository import AuthRepository

from src.core.security import (
    create_access_token,
    create_refresh_token,
    decode_token,
)


class AuthService:
    def __init__(self, repository: AuthRepository):
        self.repository = repository

    async def send_otp(
        self,
        request: SendOtpRequest
    ) -> SendOtpResponse:

        mobile = request.mobile_number

        # OTP generation and sending will be handled by Firebase.

        user = self.repository.get_user_by_mobile(mobile)

        if not user:
            self.repository.create_user(
                mobile_number=mobile,
                username=request.username,
                is_verified=False,
            )

        return SendOtpResponse(
            success=True,
            message="OTP request accepted. Pending Firebase integration."
        )

    def sync_phone_user(
        self,
        firebase_uid: str,
        phone_number: str,
        is_signup: bool = False
    ):
        # Step 1: Find by firebase_uid
        user = self.repository.get_user_by_firebase_uid(firebase_uid)

        # Normalize phone number (strip non-digits, keep last 10)
        normalized_phone = re.sub(r'\D', '', phone_number)
        if len(normalized_phone) > 10:
            normalized_phone = normalized_phone[-10:]

        if is_signup:
            if user:
                raise HTTPException(
                    status_code=status.HTTP_409_CONFLICT,
                    detail="This mobile number is already linked to an existing account. Please sign in instead."
                )
            existing_phone_user = self.repository.get_user_by_mobile(normalized_phone)
            if existing_phone_user:
                raise HTTPException(
                    status_code=status.HTTP_409_CONFLICT,
                    detail="This mobile number is already linked to an existing account. Please sign in instead."
                )
            # Step 3: Create new User
            return self.repository.create_user(
                mobile_number=normalized_phone,
                firebase_uid=firebase_uid,
                is_verified=True
            )
        else: # is_signup == False
            if user:
                if not user.is_verified:
                    self.repository.update_user_verification(user, True)
                return user

            # Allow fallback to phone matching
            existing_phone_user = self.repository.get_user_by_mobile(normalized_phone)
            if existing_phone_user:
                if existing_phone_user.firebase_uid and existing_phone_user.firebase_uid != firebase_uid:
                    raise HTTPException(
                        status_code=status.HTTP_409_CONFLICT,
                        detail="This mobile number is already linked to an existing account. Please sign in instead."
                    )
                self.repository.link_firebase_uid(existing_phone_user, firebase_uid)
                if not existing_phone_user.is_verified:
                    self.repository.update_user_verification(existing_phone_user, True)
                return existing_phone_user

            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail="No account found with this phone number. Please sign up first."
            )

    def _get_social_provider_name(self, provider: str | None) -> str:
        if provider == "google.com":
            return "Google"
        elif provider == "facebook.com":
            return "Facebook"
        return "social"

    def sync_social_user(
        self,
        firebase_uid: str,
        email: str,
        username: str | None = None,
        name: str | None = None,
        is_signup: bool = False,
        provider: str | None = None
    ):
        # Step 1: Find by firebase_uid
        user = self.repository.get_user_by_firebase_uid(firebase_uid)

        provider_name = self._get_social_provider_name(provider)
        conflict_msg = f"This {provider_name} account is already linked to an existing account. Please sign in instead."

        if is_signup:
            if user:
                raise HTTPException(
                    status_code=status.HTTP_409_CONFLICT,
                    detail=conflict_msg
                )
            existing_email_user = self.repository.get_user_by_email(email)
            if existing_email_user:
                raise HTTPException(
                    status_code=status.HTTP_409_CONFLICT,
                    detail=conflict_msg
                )
            # Step 3: Create new User without mobile_number
            return self.repository.create_user(
                email=email,
                username=username,
                firebase_uid=firebase_uid,
                is_verified=True,
                name=name
            )
        else: # is_signup == False
            if user:
                if username and user.username and username.lower() != user.username.lower():
                    raise HTTPException(
                        status_code=status.HTTP_401_UNAUTHORIZED,
                        detail="The username does not match this account. Please check your username and try again."
                    )
                if not user.is_verified:
                    self.repository.update_user_verification(user, True)
                return user

            # Allow fallback to email matching
            existing_email_user = self.repository.get_user_by_email(email)
            if existing_email_user:
                if username and existing_email_user.username and username.lower() != existing_email_user.username.lower():
                    raise HTTPException(
                        status_code=status.HTTP_401_UNAUTHORIZED,
                        detail="The username does not match this account. Please check your username and try again."
                    )
                if existing_email_user.firebase_uid and existing_email_user.firebase_uid != firebase_uid:
                    raise HTTPException(
                        status_code=status.HTTP_409_CONFLICT,
                        detail=conflict_msg
                    )
                self.repository.link_firebase_uid(existing_email_user, firebase_uid)
                if not existing_email_user.is_verified:
                    self.repository.update_user_verification(existing_email_user, True)
                return existing_email_user

            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail=f"No account found with this {provider_name} account. Please sign up first."
            )

    def update_profile(self, user, request):
        if request.name is not None:
            user.name = request.name
        if request.username is not None:
            from sqlalchemy import select
            from src.modules.auth.models import User
            existing_user = self.repository.db.scalar(
                select(User).where(User.username == request.username, User.id != user.id)
            )
            if existing_user:
                raise HTTPException(
                    status_code=status.HTTP_409_CONFLICT,
                    detail="This username is already taken."
                )
            user.username = request.username
        if request.mobile_number is not None:
            from sqlalchemy import select
            from src.modules.auth.models import User
            existing_mobile = self.repository.db.scalar(
                select(User).where(User.mobile_number == request.mobile_number, User.id != user.id)
            )
            if existing_mobile:
                raise HTTPException(
                    status_code=status.HTTP_409_CONFLICT,
                    detail="This mobile number is already linked to another account."
                )
            user.mobile_number = request.mobile_number
        if request.house_address is not None:
            user.house_address = request.house_address
        if request.latitude is not None:
            user.latitude = request.latitude
        if request.longitude is not None:
            user.longitude = request.longitude
            
        self.repository.db.commit()
        self.repository.db.refresh(user)
        return user

    async def verify_otp(
        self,
        request: VerifyOtpRequest
    ) -> TokenResponse:

        # Pending Firebase integration for token verification.
        raise HTTPException(
            status_code=status.HTTP_501_NOT_IMPLEMENTED,
            detail="Firebase integration pending."
        )

        user = self.repository.get_user_by_mobile(mobile)

        if not user:
            user = self.repository.create_user(
                mobile_number=mobile,
                username=None,
                is_verified=True,
            )
        else:
            if not user.is_verified:
                self.repository.update_user_verification(
                    user,
                    is_verified=True
                )

        # Create SolarEase's own JWT tokens.
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

    def refresh_token(
        self,
        request: RefreshTokenRequest
    ) -> TokenResponse:

        payload = decode_token(request.refresh_token)

        if payload.get("type") != "refresh":
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="Invalid token type. Refresh token required."
            )

        user_id_str = payload.get("sub")
        if not user_id_str:
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="Token payload missing subject identifier."
            )

        try:
            import uuid
            user_id = uuid.UUID(user_id_str)
        except ValueError:
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="Invalid subject format."
            )

        user = self.repository.get_user_by_id(user_id)

        if not user:
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="User associated with this token no longer exists."
            )

        # Issue a new access token.
        access_token = create_access_token(
            user.id,
            user.mobile_number
        )

        user_response = UserResponse.model_validate(user)

        return TokenResponse(
            access_token=access_token,
            refresh_token=request.refresh_token,
            user=user_response,
        )