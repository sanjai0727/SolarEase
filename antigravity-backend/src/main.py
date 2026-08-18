import os
import certifi

# Fix TLS certificate verification by overriding system CA bundle env vars
os.environ["SSL_CERT_FILE"] = certifi.where()
os.environ["REQUESTS_CA_BUNDLE"] = certifi.where()
os.environ["CURL_CA_BUNDLE"] = certifi.where()

from contextlib import asynccontextmanager
from fastapi import FastAPI
import firebase_admin
import logging

from src.core.config import settings
from src.core.middleware import setup_middleware
from src.api.routes import router as api_router

logger = logging.getLogger(__name__)

@asynccontextmanager
async def lifespan(app: FastAPI):
    # Initialize Firebase Admin SDK
    try:
        firebase_admin.get_app()
    except ValueError:
        try:
            from firebase_admin import credentials
            # Uses GOOGLE_APPLICATION_CREDENTIALS explicitly from settings
            cred = credentials.Certificate(settings.GOOGLE_APPLICATION_CREDENTIALS)
            firebase_admin.initialize_app(cred)
            logger.info("Firebase Admin SDK initialized successfully.")
        except Exception as e:
            logger.error("Failed to initialize Firebase Admin SDK. Please check configuration.")
            raise e
    yield
    # Cleanup on shutdown (if any)

def create_app() -> FastAPI:
    app = FastAPI(
        title=settings.PROJECT_NAME,
        debug=settings.DEBUG,
        lifespan=lifespan
    )

    setup_middleware(app)

    # Include root router
    app.include_router(api_router)

    return app

app = create_app()
