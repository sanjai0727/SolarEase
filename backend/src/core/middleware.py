from fastapi.middleware.cors import CORSMiddleware
from fastapi import FastAPI
from src.core.config import settings

def setup_middleware(app: FastAPI):
    origins = settings.get_cors_origins
    allow_wildcard = "*" in origins
    app.add_middleware(
        CORSMiddleware,
        allow_origins=origins,
        allow_credentials=not allow_wildcard,
        allow_methods=["GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"],
        allow_headers=["*"],
    )
