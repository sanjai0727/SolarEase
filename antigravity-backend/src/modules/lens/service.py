from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.future import select
from fastapi import HTTPException
import uuid
from typing import List
from .models import Appliance
from .schemas import ApplianceCreate, ApplianceUpdate, AnalysisResponse
from google import genai
from google.genai import types
from src.core.config import settings
import json

class ApplianceService:
    @staticmethod
    async def create_appliance(db: AsyncSession, user_id: uuid.UUID, appliance_in: ApplianceCreate) -> Appliance:
        db_appliance = Appliance(
            user_id=user_id,
            **appliance_in.model_dump()
        )
        db.add(db_appliance)
        await db.commit()
        await db.refresh(db_appliance)
        return db_appliance

    @staticmethod
    async def get_appliances(db: AsyncSession, user_id: uuid.UUID) -> List[Appliance]:
        result = await db.execute(select(Appliance).where(Appliance.user_id == user_id))
        return result.scalars().all()

    @staticmethod
    async def get_appliance(db: AsyncSession, user_id: uuid.UUID, appliance_id: uuid.UUID) -> Appliance:
        result = await db.execute(select(Appliance).where(Appliance.id == appliance_id, Appliance.user_id == user_id))
        db_appliance = result.scalar_one_or_none()
        if not db_appliance:
            raise HTTPException(status_code=404, detail="Appliance not found")
        return db_appliance

    @staticmethod
    async def update_appliance(db: AsyncSession, user_id: uuid.UUID, appliance_id: uuid.UUID, appliance_in: ApplianceUpdate) -> Appliance:
        db_appliance = await ApplianceService.get_appliance(db, user_id, appliance_id)
        update_data = appliance_in.model_dump(exclude_unset=True)
        for field, value in update_data.items():
            setattr(db_appliance, field, value)
        await db.commit()
        await db.refresh(db_appliance)
        return db_appliance

    @staticmethod
    async def delete_appliance(db: AsyncSession, user_id: uuid.UUID, appliance_id: uuid.UUID) -> None:
        db_appliance = await ApplianceService.get_appliance(db, user_id, appliance_id)
        await db.delete(db_appliance)
        await db.commit()

class ApplianceAnalysisService:
    @staticmethod
    async def analyze_image(image_bytes: bytes, mime_type: str) -> AnalysisResponse:
        if not settings.GEMINI_API_KEY:
            raise HTTPException(status_code=500, detail="Gemini API key not configured")
        
        client = genai.Client(api_key=settings.GEMINI_API_KEY)
        
        prompt = """
        Analyze this image and identify the primary home appliance.
        Provide a JSON response with the following keys:
        - "detected_appliance": The name of the appliance (e.g. Refrigerator, Air Conditioner, Ceiling Fan). If none, say "Unknown".
        - "confidence": "High", "Medium", or "Low"
        - "suggested_power_watts": An estimated typical power rating in watts for this type of appliance (e.g. 150 for a standard refrigerator, 75 for a ceiling fan, 1500 for an AC). Make a reasonable guess based on the appliance type. If unknown, return 0.
        
        Return ONLY valid JSON, nothing else.
        """
        
        try:
            response = client.models.generate_content(
                model='gemini-2.5-flash',
                contents=[
                    prompt,
                    types.Part.from_bytes(data=image_bytes, mime_type=mime_type)
                ],
                config=types.GenerateContentConfig(
                    response_mime_type="application/json",
                    temperature=0.2
                )
            )
            
            result_text = response.text
            data = json.loads(result_text)
            
            return AnalysisResponse(
                detected_appliance=data.get("detected_appliance", "Unknown"),
                confidence=data.get("confidence", "Low"),
                suggested_power_watts=float(data.get("suggested_power_watts", 0.0))
            )
            
        except Exception as e:
            raise HTTPException(status_code=500, detail=f"Analysis failed: {str(e)}")
