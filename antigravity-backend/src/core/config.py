from pydantic_settings import BaseSettings, SettingsConfigDict
from pydantic import AnyHttpUrl
import json

class Settings(BaseSettings):
    PROJECT_NAME: str = "SolarEase AntiGravity Backend"
    ENVIRONMENT: str = "development"
    DEBUG: bool = True
    DATABASE_URL: str
    CORS_ORIGINS: str = '["*"]'


    # JWT Config
    JWT_SECRET_KEY: str
    JWT_ALGORITHM: str = "HS256"

    # Firebase
    GOOGLE_APPLICATION_CREDENTIALS: str = "firebase-adminsdk.json"

    # Gemini
    GEMINI_API_KEY: str = ""

    # OpenRouter (temporary testing provider)
    OPENROUTER_API_KEY: str = ""

    # AI Provider configuration is removed as Solar Lens uses OpenRouter exclusively

    @property
    def get_cors_origins(self) -> list[str]:
        try:
            return json.loads(self.CORS_ORIGINS)
        except json.JSONDecodeError:
            return ["*"]

    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8", extra="ignore")

settings = Settings()
