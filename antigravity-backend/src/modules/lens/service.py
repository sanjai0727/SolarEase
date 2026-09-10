from sqlalchemy.orm import Session
from sqlalchemy.future import select
from fastapi import HTTPException
import uuid
from typing import List
import json
import logging
from google import genai
from google.genai import types
from src.core.config import settings
from .models import Appliance
from .schemas import ApplianceCreate, ApplianceUpdate, DetectionResult, Detection, BoundingBox

logger = logging.getLogger(__name__)

LENS_MODEL = "gemini-3.8-flash"

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

class ApplianceAnalysisService:
    @staticmethod
    async def analyze_image(image_bytes: bytes, mime_type: str, source: str = "PHOTO") -> DetectionResult:
        if not settings.GEMINI_API_KEY:
            raise HTTPException(
                status_code=500,
                detail="GEMINI_API_KEY is missing. Solar Lens requires a Gemini API key."
            )
        
        logger.info(f"[LENS] Lens service started")
        logger.info(f"[LENS] Gemini model: {LENS_MODEL}")
        
        client = genai.Client(api_key=settings.GEMINI_API_KEY)
        
        prompt = """
        You are the visual detection engine for SolarEase, a household energy analysis application.

        Analyze the supplied image and identify visible instances of ONLY these supported appliance categories:

        - LED Tube Light
        - Ceiling Fan
        - Air Conditioner
        - Refrigerator
        - Television

        For every clearly visible instance:

        1. Identify the appliance category.
        2. Estimate a confidence score from 0.0 to 1.0.
        3. Return a tight 2D bounding box around the visible appliance.
        4. Multiple instances of the same appliance are allowed.
        5. Each physically separate appliance must be a separate detection.
        6. Do not merge separate appliances.
        7. Do not hallucinate appliances.
        8. Do not infer that an appliance exists outside the visible image.
        9. If an appliance is heavily occluded and there is insufficient visual evidence, do not confidently detect it.
        10. Ignore all unsupported objects.
        11. If no supported appliance is visible, return an empty detections array.

        Bounding boxes must tightly surround the visible appliance.

        Return only the structured detection response.

        Do not estimate:
        - wattage
        - usage hours
        - days per month
        - electricity tariff
        - monthly units
        - monthly cost
        - solar panel size
        - solar savings

        Those values belong to SolarEase's existing appliance/energy system and user review process.
        """
        
        try:
            logger.info(f"[LENS DIAG] 4. Gemini request started (Model: {LENS_MODEL})")
            response = client.models.generate_content(
                model=LENS_MODEL,
                contents=[
                    prompt,
                    types.Part.from_bytes(data=image_bytes, mime_type=mime_type)
                ],
                config=types.GenerateContentConfig(
                    response_mime_type="application/json",
                    response_schema={
                        "type": "OBJECT",
                        "properties": {
                            "detections": {
                                "type": "ARRAY",
                                "items": {
                                    "type": "OBJECT",
                                    "properties": {
                                        "label": {
                                            "type": "STRING",
                                            "enum": ["LED Tube Light", "Ceiling Fan", "Air Conditioner", "Refrigerator", "Television"]
                                        },
                                        "confidence": {"type": "NUMBER"},
                                        "box_2d": {
                                            "type": "ARRAY",
                                            "items": {"type": "INTEGER"}
                                        }
                                    },
                                    "required": ["label", "confidence", "box_2d"]
                                }
                            }
                        },
                        "required": ["detections"]
                    },
                    temperature=0.2
                )
            )
            logger.info(f"[LENS DIAG] 5. Gemini request completed. Response text length: {len(response.text) if hasattr(response, 'text') and response.text else 0}")
            
            logger.info(f"[LENS DIAG] 6. Response parsing started")
            result_text = response.text
            data = json.loads(result_text)
            
            parsed_detections = []
            for det in data.get("detections", []):
                # box_2d is [ymin, xmin, ymax, xmax] normalized from 0-1000
                box = det.get("box_2d", [0, 0, 0, 0])
                if len(box) == 4:
                    ymin, xmin, ymax, xmax = box
                    b_box = BoundingBox(
                        left=xmin / 1000.0,
                        top=ymin / 1000.0,
                        right=xmax / 1000.0,
                        bottom=ymax / 1000.0
                    )
                    parsed_detections.append(
                        Detection(
                            id=f"det_{uuid.uuid4().hex[:8]}",
                            label=det.get("label"),
                            confidence=float(det.get("confidence", 0.0)),
                            source=source,
                            bounding_box=b_box
                        )
                    )

            logger.info(f"[LENS DIAG] 7. Detections returned: {len(parsed_detections)}")
            return DetectionResult(detections=parsed_detections)
            
        except Exception as e:
            logger.exception(f"[LENS DIAG] 8. Lens analysis failed with exception. Type: {e.__class__.__name__}")
            raise HTTPException(status_code=500, detail=f"Analysis failed: {str(e)}")
