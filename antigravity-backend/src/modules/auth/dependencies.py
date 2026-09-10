from fastapi import Depends, HTTPException, status
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from sqlalchemy.orm import Session
from src.core.database import get_db
from src.core.security import decode_token
from src.modules.auth.repository import AuthRepository
from src.modules.auth.models import User
import uuid
from firebase_admin import auth as firebase_auth
import logging
from typing import Any, Optional
security = HTTPBearer()
firebase_security = HTTPBearer(auto_error=False)
logger = logging.getLogger(__name__)

def get_verified_firebase_token(
    credentials: Optional[HTTPAuthorizationCredentials] = Depends(firebase_security)
) -> dict[str, Any]:
    """
    Verifies a Firebase ID token sent in the Authorization header.
    Returns the decoded Firebase claims if valid.
    """
    if credentials is None or credentials.scheme.lower() != "bearer":
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Firebase authentication token missing.",
            headers={"WWW-Authenticate": "Bearer"},
        )

    token = credentials.credentials.strip()

    if not token:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Firebase authentication token missing.",
            headers={"WWW-Authenticate": "Bearer"},
        )

    try:
        return firebase_auth.verify_id_token(token, check_revoked=True)
    except firebase_auth.CertificateFetchError:
        logger.error("Firebase public-key retrieval failed during ID token verification.")
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="Firebase authentication is temporarily unavailable.",
            headers={"WWW-Authenticate": "Bearer"},
        )
    except (
        firebase_auth.ExpiredIdTokenError,
        firebase_auth.RevokedIdTokenError,
        firebase_auth.UserDisabledError,
        firebase_auth.InvalidIdTokenError,
    ) as error:
        logger.info("Firebase ID token rejected: %s", error.__class__.__name__)
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid or expired Firebase authentication token.",
            headers={"WWW-Authenticate": "Bearer"},
        )
    except Exception as error:
        logger.error("Firebase ID token verification failed: %s", error.__class__.__name__)
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid or expired Firebase authentication token.",
            headers={"WWW-Authenticate": "Bearer"},
        )

def get_current_user(
    credentials: HTTPAuthorizationCredentials = Depends(security),
    db: Session = Depends(get_db)
) -> User:
    token = credentials.credentials
    payload = decode_token(token)
    
    if payload.get("type") != "access":
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid token type. Access token required.",
            headers={"WWW-Authenticate": "Bearer"},
        )
    
    user_id_str = payload.get("sub")
    if not user_id_str:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Token payload missing subject identifier.",
            headers={"WWW-Authenticate": "Bearer"},
        )
        
    try:
        user_id = uuid.UUID(user_id_str)
    except ValueError:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid subject format.",
            headers={"WWW-Authenticate": "Bearer"},
        )

    repository = AuthRepository(db)
    user = repository.get_user_by_id(user_id)
    if not user:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="User not found or has been deleted.",
            headers={"WWW-Authenticate": "Bearer"},
        )
        
    return user
