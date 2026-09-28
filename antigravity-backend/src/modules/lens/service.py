from sqlalchemy.orm import Session
from sqlalchemy.future import select
from fastapi import HTTPException
import uuid
import json
import logging
from typing import List, Optional
from google import genai
from google.genai import types

from src.core.config import settings
from .models import Appliance
from .schemas import (
    ApplianceCreate,
    ApplianceUpdate,
    ApplianceResponse,
    DetectionResult,
    Detection,
    DetectedAppliance,
    BoundingBox,
)

logger = logging.getLogger(__name__)

GEMINI_MODEL = "gemini-3.8-flash"

LENS_UPLOAD_PROMPT = """You are the expert household energy-audit vision analyzer for SolarEase.
Analyze the supplied image or video sweep and identify all visible household electrical appliances.

INSTRUCTIONS:
1. Detect real, visible household electrical appliances.
2. Distinguish electrical appliances from non-electrical furniture, architectural fixtures, and general decor.
3. Consolidate duplicate appliances: If an appliance appears across multiple video frames or perspectives, report it only once with the correct consolidated quantity.
4. Extract visible manufacturer labels, model numbers, and nameplate power ratings (in Watts) ONLY when visually confirmed or clearly supported by the nameplate/label.
5. If the power rating is not visible or reliably inferable, set power_rating_watts to null. NEVER invent or hallucinate specifications.
6. Provide an accurate confidence score (0.0 to 1.0) and a concise visual evidence description noting location and identifying characteristics.
7. Where identifiable, map to standard appliance categories such as:
   - LED Tube Light / Bulb
   - Ceiling Fan / Table Fan
   - Television
   - Refrigerator
   - Air Conditioner
   - Washing Machine
   - Water Heater / Geyser
   - Microwave / Oven
   - Induction Cooktop / Stove
   - Mixer Grinder / Electric Kettle
   - Air Cooler
   - Water Pump
   - Computer / Laptop

Return ONLY valid JSON matching this schema:
{
  "detected_appliances": [
    {
      "appliance_type": "string (e.g. Refrigerator, Ceiling Fan, Air Conditioner, Television)",
      "appliance_name": "string (human readable name with model/type details if visible)",
      "quantity": 1,
      "power_rating_watts": null,
      "confidence": 0.95,
      "evidence": "string (concise description of visible location, model clues, and appearance)"
    }
  ]
}

If no electrical appliances are visible, return: {"detected_appliances": []}
Do NOT return conversational filler or explanations outside of the JSON."""


class ApplianceService:
    @staticmethod
    def create_appliance(db: Session, user_id: uuid.UUID, appliance_in: ApplianceCreate) -> Appliance:
        db_appliance = Appliance(
            user_id=user_id,
            **appliance_in.model_dump()
        )
        db.add(db_appliance)
        db.commit()
        db.refresh(db_appliance)
        return db_appliance

    @staticmethod
    def get_appliances(db: Session, user_id: uuid.UUID) -> List[Appliance]:
        result = db.execute(select(Appliance).where(Appliance.user_id == user_id))
        return result.scalars().all()

    @staticmethod
    def get_appliance(db: Session, user_id: uuid.UUID, appliance_id: uuid.UUID) -> Appliance:
        result = db.execute(select(Appliance).where(Appliance.id == appliance_id, Appliance.user_id == user_id))
        db_appliance = result.scalar_one_or_none()
        if not db_appliance:
            raise HTTPException(status_code=404, detail="Appliance not found")
        return db_appliance

    @staticmethod
    def update_appliance(db: Session, user_id: uuid.UUID, appliance_id: uuid.UUID, appliance_in: ApplianceUpdate) -> Appliance:
        db_appliance = ApplianceService.get_appliance(db, user_id, appliance_id)
        update_data = appliance_in.model_dump(exclude_unset=True)
        for field, value in update_data.items():
            setattr(db_appliance, field, value)
        db.commit()
        db.refresh(db_appliance)
        return db_appliance

    @staticmethod
    def delete_appliance(db: Session, user_id: uuid.UUID, appliance_id: uuid.UUID) -> None:
        db_appliance = ApplianceService.get_appliance(db, user_id, appliance_id)
        db.delete(db_appliance)
        db.commit()


def _get_gemini_client() -> genai.Client:
    """Initialize Google GenAI client targeting Vertex AI with fallback to API key."""
    try:
        return genai.Client(vertexai=True, project="solarease-bf39c", location="global")
    except Exception as e:
        logger.warning(f"[LENS_UPLOAD] Vertex AI client initialization exception: {e}. Checking GEMINI_API_KEY fallback...")
        api_key = settings.GEMINI_API_KEY
        if not api_key:
            raise HTTPException(
                status_code=500,
                detail="Gemini vision service is not configured on the backend."
            )
        return genai.Client(api_key=api_key)


class ApplianceAnalysisService:
    @staticmethod
    async def analyze_media(media_bytes: bytes, mime_type: str, source: str = "UPLOAD") -> DetectionResult:
        """Analyze uploaded image or video using Gemini 3.8 Flash."""
        input_type = "video" if mime_type.startswith("video/") else "image"
        logger.info(
            f"[LENS_UPLOAD] provider=gemini model={GEMINI_MODEL} input_type={input_type} "
            f"size_bytes={len(media_bytes)} mime_type={mime_type}"
        )

        client = _get_gemini_client()

        try:
            part = types.Part.from_bytes(data=media_bytes, mime_type=mime_type)

            response = client.models.generate_content(
                model=GEMINI_MODEL,
                contents=[part, LENS_UPLOAD_PROMPT],
                config=types.GenerateContentConfig(
                    response_mime_type="application/json",
                    temperature=0.1,
                ),
            )
        except Exception as e:
            logger.exception(f"[LENS_UPLOAD] Gemini generation failed: {e}")
            raise HTTPException(
                status_code=502,
                detail=f"Gemini vision analysis failed: {str(e)}"
            )

        raw_text = response.text or ""
        logger.info(f"[LENS_UPLOAD] Gemini 3.8 Flash response received: {len(raw_text)} chars")

        return ApplianceAnalysisService._parse_gemini_response(raw_text, source)

    # Alias for backward compatibility
    analyze_image = analyze_media

    @staticmethod
    def _parse_gemini_response(raw_text: str, source: str) -> DetectionResult:
        text = raw_text.strip()
        try:
            data = json.loads(text)
        except json.JSONDecodeError:
            import re
            match = re.search(r"\{[\s\S]*\}", text)
            if match:
                try:
                    data = json.loads(match.group(0))
                except Exception:
                    data = {"detected_appliances": []}
            else:
                logger.error("[LENS_UPLOAD] Could not extract valid JSON from Gemini response")
                return DetectionResult(detections=[], detected_appliances=[])

        raw_appliances = data.get("detected_appliances")
        if not isinstance(raw_appliances, list):
            raw_appliances = data.get("appliances")
        if not isinstance(raw_appliances, list):
            raw_appliances = data.get("detections")
        if not isinstance(raw_appliances, list):
            raw_appliances = []

        detected_appliances = []
        parsed_detections = []

        for item in raw_appliances:
            if not isinstance(item, dict):
                continue
            app_type = item.get("appliance_type") or item.get("label") or "Appliance"
            app_name = item.get("appliance_name") or app_type
            qty = max(1, int(item.get("quantity") or 1))

            power_watts = None
            raw_power = item.get("power_rating_watts")
            if raw_power is not None:
                try:
                    power_watts = float(raw_power)
                except (ValueError, TypeError):
                    power_watts = None

            conf = 0.9
            raw_conf = item.get("confidence")
            if raw_conf is not None:
                try:
                    conf = float(raw_conf)
                except (ValueError, TypeError):
                    conf = 0.9

            evidence = item.get("evidence")

            detected_appliances.append(
                DetectedAppliance(
                    appliance_type=app_type,
                    appliance_name=app_name,
                    quantity=qty,
                    power_rating_watts=power_watts,
                    confidence=conf,
                    evidence=evidence,
                )
            )

            # Map to Detection objects for backward compatibility with Android UI
            for _ in range(qty):
                parsed_detections.append(
                    Detection(
                        id=f"det_{uuid.uuid4().hex[:8]}",
                        label=app_name,
                        confidence=conf,
                        source=source,
                        bounding_box=BoundingBox(left=0.0, top=0.0, right=1.0, bottom=1.0),
                        power_rating_watts=power_watts,
                        quantity=1,
                        evidence=evidence,
                        appliance_type=app_type,
                        appliance_name=app_name,
                    )
                )

        logger.info(
            f"[LENS_UPLOAD] Parsed {len(detected_appliances)} unique appliance categories, "
            f"{len(parsed_detections)} total items."
        )

        return DetectionResult(
            detections=parsed_detections,
            detected_appliances=detected_appliances,
        )
