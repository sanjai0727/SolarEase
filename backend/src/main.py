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

            if os.path.exists(settings.GOOGLE_APPLICATION_CREDENTIALS):
                cred = credentials.Certificate(
                    settings.GOOGLE_APPLICATION_CREDENTIALS
                )
                firebase_admin.initialize_app(cred)
                logger.info("Firebase Admin SDK initialized with certificate file.")
            else:
                firebase_admin.initialize_app()
                logger.info("Firebase Admin SDK initialized with Application Default Credentials.")

        except Exception as e:
            logger.error(
                "Failed to initialize Firebase Admin SDK. Please check configuration."
            )
            raise e

    yield


def create_app() -> FastAPI:
    app = FastAPI(
        title=settings.PROJECT_NAME,
        debug=settings.DEBUG,
        lifespan=lifespan
    )

    # Health and readiness checks
    @app.get("/healthz")
    @app.get("/health")
    async def health_check():
        return {
            "status": "healthy",
            "environment": settings.ENVIRONMENT,
            "version": "1.0.0"
        }

    @app.get("/ready")
    async def readiness_check():
        from src.core.database import SessionLocal
        from sqlalchemy import text
        db = SessionLocal()
        try:
            db.execute(text("SELECT 1"))
            return {"status": "ready", "database": "connected"}
        except Exception as e:
            logger.error("Database readiness check failed: %s", e)
            return {"status": "degraded", "database": "unavailable"}
        finally:
            db.close()

    setup_middleware(app)

    # Include API router
    app.include_router(api_router)

    return app


app = create_app()