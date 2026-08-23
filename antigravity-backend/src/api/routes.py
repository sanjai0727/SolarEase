from fastapi import APIRouter
from src.modules.auth.router import router as auth_router
from src.modules.chatbot.router import router as chatbot_router

router = APIRouter()
router.include_router(auth_router)
router.include_router(chatbot_router)

@router.get("/")
def read_root():
    return {"message": "Welcome to SolarEase AntiGravity Backend"}

@router.get("/health")
def health_check():
    return {"status": "ok", "message": "Service is healthy"}
