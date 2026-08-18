from fastapi.middleware.cors import CORSMiddleware
from fastapi import FastAPI
from src.core.config import settings

def setup_middleware(app: FastAPI):
    app.add_middleware(
        CORSMiddleware,
        allow_origins=settings.get_cors_origins,
        allow_credentials=True,
        allow_methods=["*"],
        allow_headers=["*"],
    )
