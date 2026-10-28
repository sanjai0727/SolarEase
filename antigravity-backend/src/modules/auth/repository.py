from sqlalchemy.orm import Session
from sqlalchemy import select, update, delete, func
from datetime import datetime, timezone
from typing import Optional

from src.modules.auth.models import User, OTPVerification

class AuthRepository:
    def __init__(self, db: Session):
        self.db = db

    def get_user_by_mobile(self, mobile_number: str) -> Optional[User]:
        return self.db.scalar(select(User).where(User.mobile_number == mobile_number))

    def get_user_by_email(self, email: str) -> Optional[User]:
        clean_email = email.strip().lower()
        return self.db.scalar(select(User).where(func.lower(User.email) == clean_email))

    def get_user_by_username(self, username: str) -> Optional[User]:
        clean_username = username.strip().lower()
        return self.db.scalar(select(User).where(func.lower(User.username) == clean_username))

    def get_user_by_id(self, user_id) -> Optional[User]:
        return self.db.scalar(select(User).where(User.id == user_id))

    def get_user_by_firebase_uid(self, uid: str) -> Optional[User]:
        return self.db.scalar(select(User).where(User.firebase_uid == uid))


    def link_firebase_uid(self, user: User, uid: str) -> User:
        user.firebase_uid = uid
        self.db.commit()
        self.db.refresh(user)
        return user

    def create_user(
        self, 
        mobile_number: Optional[str] = None, 
        username: Optional[str] = None, 
        is_verified: bool = False, 
        firebase_uid: Optional[str] = None,
        email: Optional[str] = None,
        name: Optional[str] = None
    ) -> User:
        user = User(
            mobile_number=mobile_number, 
            username=username, 
            is_verified=is_verified, 
            firebase_uid=firebase_uid,
            email=email,
            name=name
        )
        self.db.add(user)
        self.db.commit()
        self.db.refresh(user)
        return user

    def update_user_verification(self, user: User, is_verified: bool) -> User:
        user.is_verified = is_verified
        self.db.commit()
        self.db.refresh(user)
        return user

    def cleanup_old_otps(self, mobile_number: str) -> None:
        """Deletes expired or already verified OTPs for a given number to keep the table clean."""
        now = datetime.now(timezone.utc)
        stmt = delete(OTPVerification).where(
            (OTPVerification.mobile_number == mobile_number) & 
            ((OTPVerification.verified == True) | (OTPVerification.expires_at < now))
        )
        self.db.execute(stmt)
        self.db.commit()

    def invalidate_active_otps(self, mobile_number: str) -> None:
        """Marks any currently active unverified OTPs as expired."""
        now = datetime.now(timezone.utc)
        stmt = update(OTPVerification).where(
            (OTPVerification.mobile_number == mobile_number) & 
            (OTPVerification.verified == False) & 
            (OTPVerification.expires_at > now)
        ).values(expires_at=now)
        self.db.execute(stmt)
        self.db.commit()

    def create_otp_verification(self, mobile_number: str, otp_hash: str, expires_at: datetime) -> OTPVerification:
        otp_record = OTPVerification(
            mobile_number=mobile_number,
            otp_hash=otp_hash,
            expires_at=expires_at,
            attempt_count=0,
            verified=False
        )
        self.db.add(otp_record)
        self.db.commit()
        self.db.refresh(otp_record)
        return otp_record

    def get_active_otp(self, mobile_number: str) -> Optional[OTPVerification]:
        now = datetime.now(timezone.utc)
        return self.db.scalar(
            select(OTPVerification)
            .where(
                (OTPVerification.mobile_number == mobile_number) & 
                (OTPVerification.verified == False) & 
                (OTPVerification.expires_at > now)
            )
            .order_by(OTPVerification.created_at.desc())
        )

    def increment_otp_attempts(self, otp_record: OTPVerification) -> OTPVerification:
        otp_record.attempt_count += 1
        self.db.commit()
        self.db.refresh(otp_record)
        return otp_record

    def mark_otp_verified(self, otp_record: OTPVerification) -> OTPVerification:
        otp_record.verified = True
        self.db.commit()
        self.db.refresh(otp_record)
        return otp_record

    def revoke_token(self, jti: str, user_id: str, expires_at: datetime, token_type: str = "refresh"):
        from src.modules.auth.models import RevokedToken
        revoked = RevokedToken(
            jti=jti,
            user_id=str(user_id),
            expires_at=expires_at,
            token_type=token_type
        )
        self.db.add(revoked)
        self.db.commit()
        return revoked

    def is_token_revoked(self, jti: str) -> bool:
        from src.modules.auth.models import RevokedToken
        stmt = select(RevokedToken).where(RevokedToken.jti == jti)
        return self.db.scalar(stmt) is not None

