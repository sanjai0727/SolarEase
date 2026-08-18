from fastapi import Depends, HTTPException, status
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from sqlalchemy.orm import Session
from src.core.database import get_db
from src.core.security import decode_token
from src.modules.auth.repository import AuthRepository
from src.modules.auth.models import User
import uuid
import firebase_admin.auth
from typing import Any, Optional
from src.modules.auth.schemas import FirebaseTokenRequest
security = HTTPBearer()
firebase_security = HTTPBearer(auto_error=False)

def get_verified_firebase_token(
    request_body: Optional[FirebaseTokenRequest] = None,
    credentials: Optional[HTTPAuthorizationCredentials] = Depends(firebase_security)
) -> dict[str, Any]:
    """
    Verifies a Firebase ID token sent in the Authorization header.
    Returns the decoded Firebase claims if valid.
    """
    token = None
    if credentials:
        token = credentials.credentials
    elif request_body:
        token = request_body.firebase_id_token

    if not token:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Firebase authentication token missing.",
            headers={"WWW-Authenticate": "Bearer"},
        )

    try:
        decoded_token = firebase_admin.auth.verify_id_token(token)
        return decoded_token
    except Exception as e:
        import logging
        logger = logging.getLogger("firebase_diag")
        app = firebase_admin.get_app()
        logger.error(f"Firebase token verification failed. Exception: {e.__class__.__name__}, Message: {str(e)}")
        logger.error(f"Firebase Admin app name: {app.name}")
        logger.error(f"Firebase Project ID: {app.project_id}")
        
        # Try to extract unverified claims just for logging issuer/audience
        try:
            from jose import jwt
            unverified_claims = jwt.get_unverified_claims(token)
            logger.error(f"Token Issuer (iss): {unverified_claims.get('iss')}")
            logger.error(f"Token Audience (aud): {unverified_claims.get('aud')}")
        except Exception as jwt_e:
            logger.error(f"Could not extract unverified claims: {jwt_e}")

        # Catching generic Exception from firebase_admin.auth to prevent
        # exposing internal Firebase errors or stack traces to the client.
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
