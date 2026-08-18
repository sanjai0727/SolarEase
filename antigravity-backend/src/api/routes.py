from fastapi import APIRouter
from src.modules.auth.router import router as auth_router

router = APIRouter()
router.include_router(auth_router)

@router.get("/")
def read_root():
    return {"message": "Welcome to SolarEase AntiGravity Backend"}

@router.get("/health")
def health_check():
    return {"status": "ok", "message": "Service is healthy"}
