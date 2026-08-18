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
                full_name=request.full_name,
                is_verified=False,
            )

        return SendOtpResponse(
            success=True,
            message="OTP request accepted. Pending Firebase integration."
        )

    def sync_phone_user(
        self,
        firebase_uid: str,
        phone_number: str
    ):
        # Step 1: Find by firebase_uid
        user = self.repository.get_user_by_firebase_uid(firebase_uid)
        if user:
            if not user.is_verified:
                self.repository.update_user_verification(user, True)
            return user

        # Normalize phone number (strip non-digits, keep last 10)
        normalized_phone = re.sub(r'\D', '', phone_number)
        if len(normalized_phone) > 10:
            normalized_phone = normalized_phone[-10:]

        # Step 2: Find by normalized mobile_number
        user = self.repository.get_user_by_mobile(normalized_phone)
        if user:
            if user.firebase_uid and user.firebase_uid != firebase_uid:
                raise HTTPException(
                    status_code=status.HTTP_409_CONFLICT,
                    detail="Phone number is already linked to a different Firebase account."
                )
            self.repository.link_firebase_uid(user, firebase_uid)
            if not user.is_verified:
                self.repository.update_user_verification(user, True)
            return user

        # Step 3: Create new User
        user = self.repository.create_user(
            mobile_number=normalized_phone,
            firebase_uid=firebase_uid,
            is_verified=True
        )
        return user

    def sync_social_user(
        self,
        firebase_uid: str,
        email: str,
        full_name: str | None = None
    ):
        # Step 1: Find by firebase_uid
        user = self.repository.get_user_by_firebase_uid(firebase_uid)
        if user:
            # If they had an email update, we could update it, but let's keep it simple
            if not user.is_verified:
                self.repository.update_user_verification(user, True)
            return user

        # Step 2: Find by email
        user = self.repository.get_user_by_email(email)
        if user:
            if user.firebase_uid and user.firebase_uid != firebase_uid:
                raise HTTPException(
                    status_code=status.HTTP_409_CONFLICT,
                    detail="Email is already linked to a different Firebase account."
                )
            self.repository.link_firebase_uid(user, firebase_uid)
            if not user.is_verified:
                self.repository.update_user_verification(user, True)
            return user

        # Step 3: Create new User without mobile_number
        user = self.repository.create_user(
            email=email,
            full_name=full_name,
            firebase_uid=firebase_uid,
            is_verified=True
        )
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
                full_name=None,
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