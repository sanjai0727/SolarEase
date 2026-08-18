import uuid
from datetime import datetime, timedelta, timezone
from typing import Any
from jose import jwt, JWTError
from fastapi import HTTPException, status
from src.core.config import settings

def create_access_token(user_id: uuid.UUID, mobile_number: str | None = None) -> str:
    return create_token(user_id, mobile_number, "access", timedelta(minutes=15))

def create_refresh_token(user_id: uuid.UUID, mobile_number: str | None = None) -> str:
    return create_token(user_id, mobile_number, "refresh", timedelta(days=30))

def create_token(user_id: uuid.UUID, mobile_number: str | None, token_type: str, expires_delta: timedelta) -> str:
    now = datetime.now(timezone.utc)
    expire = now + expires_delta
    payload = {
        "sub": str(user_id),
        "mobile_number": mobile_number,
        "type": token_type,
        "iat": now,
        "exp": expire,
        "jti": str(uuid.uuid4())
    }
    encoded_jwt = jwt.encode(payload, settings.JWT_SECRET_KEY, algorithm=settings.JWT_ALGORITHM)
    return encoded_jwt

def decode_token(token: str) -> dict[str, Any]:
    try:
        payload = jwt.decode(token, settings.JWT_SECRET_KEY, algorithms=[settings.JWT_ALGORITHM])
        return payload
    except jwt.ExpiredSignatureError:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Token has expired.",
            headers={"WWW-Authenticate": "Bearer"},
        )
    except JWTError:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Could not validate credentials.",
            headers={"WWW-Authenticate": "Bearer"},
        )
