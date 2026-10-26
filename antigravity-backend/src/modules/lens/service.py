from sqlalchemy.orm import Session
from sqlalchemy.future import select
from fastapi import HTTPException
import asyncio
import json
import logging
import math
import re
import uuid
from typing import List, Optional, Any
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
PROVIDER_TIMEOUT_SECONDS = float(getattr(settings, "LENS_PROVIDER_TIMEOUT_SECONDS", 30.0))

LENS_UPLOAD_PROMPT = """You are the expert household energy-audit vision analyzer for SolarEase.
Analyze the supplied image or video sweep and identify ONLY household electrical appliances that are directly and clearly visible.

CRITICAL INSTRUCTIONS:
1. STRICT VISUAL EVIDENCE: Detect only real, visible household electrical appliances with unambiguous visual proof in the image or video.
2. STRICT PROHIBITION AGAINST GUESSING / INFERENCE: Strictly DO NOT guess, extrapolate, infer, or hallucinate appliances based on room context, typical household contents, unseen areas, or expectations.
   - For example: If only a refrigerator is visible in a kitchen, return ONLY the refrigerator. NEVER invent, assume, or add an air conditioner, laptop, ceiling fan, or tube light.
   - Do NOT assume appliances that are outside the camera frame, occluded, or merely plausible for this room type.
3. DISTINGUISH FIXTURES & FURNITURE: Distinguish electrical appliances from non-electrical furniture, architectural fixtures, and general decor.
4. CATEGORY NORMALIZATION ONLY: Standard categories are provided ONLY to normalize naming for appliances that have ALREADY been visually confirmed in the image. They are NOT a checklist or suggestion list of appliances to find. Normalize names to standard categories where applicable:
   - Refrigerator
   - Ceiling Fan / Table Fan
   - Television
   - Air Conditioner
   - Washing Machine
   - LED Tube Light / Bulb
   - Water Heater / Geyser
   - Microwave / Oven
   - Induction Cooktop / Stove
   - Mixer Grinder / Electric Kettle
   - Air Cooler
   - Water Pump
   - Computer / Laptop
5. CONSOLIDATE DUPLICATE APPLIANCES: If an appliance appears across multiple video frames or perspectives, report it only once with the correct consolidated quantity.
6. MANDATORY DETAILED VISUAL EVIDENCE: For each detection, you MUST provide a concrete visual evidence description noting its specific location, physical features, and visible appearance clues in the image. Detections lacking concrete visual evidence must NOT be returned.
7. NAMEPLATE SPECIFICATIONS & NULL WATTAGE: Extract visible manufacturer labels, model numbers, and nameplate power ratings (in Watts) ONLY when visually confirmed and readable on the physical appliance or rating label. If the power rating is not visible or reliably inferable, set power_rating_watts to null. NEVER invent, assume, or hallucinate wattage specifications.
8. UNCERTAIN OR EMPTY INVENTORY: If no electrical appliances are clearly visible, or if the image is too blurry/unclear to identify any electrical appliance with direct visual evidence, return: {"detected_appliances": []}. Never guess or invent an inventory for unclear images.

Return ONLY valid JSON matching this schema:
{
  "detected_appliances": [
    {
      "appliance_type": "string (e.g. Refrigerator, Ceiling Fan, Air Conditioner, Television)",
      "appliance_name": "string (human readable name with model/type details if visible)",
      "quantity": 1,
      "power_rating_watts": null,
      "confidence": 0.95,
      "evidence": "string (mandatory description of specific visible location and physical appearance in image)"
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
    WORD_TO_NUM = {
        "zero": 0, "one": 1, "two": 2, "three": 3, "four": 4,
        "five": 5, "six": 6, "seven": 7, "eight": 8, "nine": 9,
        "ten": 10, "eleven": 11, "twelve": 12, "single": 1,
        "pair": 2, "couple": 2, "double": 2, "triple": 3,
    }

    @staticmethod
    def _parse_quantity(raw_val: Any, min_qty: int = 1, max_qty: int = 50) -> int:
        """
        Safely validates and parses AI-generated quantity values into bounded integers.
        Handles words ('one', 'two', etc.), string integers/floats, fractional values,
        null/missing values, and clamps excessive or invalid quantities.
        Bounds: min_qty=1, max_qty=50.
        """
        if raw_val is None:
            return min_qty

        parsed: Optional[float] = None
        if isinstance(raw_val, (int, float)):
            if isinstance(raw_val, float) and (math.isnan(raw_val) or math.isinf(raw_val)):
                return min_qty
            parsed = float(raw_val)
        elif isinstance(raw_val, str):
            clean = raw_val.strip().lower()
            if not clean:
                return min_qty
            if clean in ApplianceAnalysisService.WORD_TO_NUM:
                parsed = float(ApplianceAnalysisService.WORD_TO_NUM[clean])
            else:
                match = re.search(r"[-+]?\d*\.?\d+", clean)
                if match:
                    try:
                        v = float(match.group(0))
                        if not (math.isnan(v) or math.isinf(v)):
                            parsed = v
                    except (ValueError, TypeError):
                        pass

        if parsed is None:
            return min_qty

        int_qty = round(parsed)
        return max(min_qty, min(int_qty, max_qty))

    @staticmethod
    def _parse_power_watts(raw_val: Any) -> Optional[float]:
        """
        Safely parses power rating in Watts. Must be a plausible household value
        in [1.0, 50000.0] W, otherwise None.
        """
        if raw_val is None:
            return None
        try:
            val = float(raw_val)
            if math.isnan(val) or math.isinf(val):
                return None
            if val <= 0.0 or val > 50000.0:
                return None
            return round(val, 1)
        except (ValueError, TypeError):
            return None

    @staticmethod
    def _parse_confidence(raw_val: Any, default: float = 0.9) -> float:
        """
        Safely parses confidence score in [0.0, 1.0].
        """
        if raw_val is None:
            return default
        try:
            val = float(raw_val)
            if math.isnan(val) or math.isinf(val):
                return default
            return max(0.0, min(1.0, val))
        except (ValueError, TypeError):
            return default

    @staticmethod
    def _is_valid_evidence(evidence: Optional[str]) -> bool:
        """
        Validates that a detection includes substantive visual evidence.
        Rejects missing, whitespace-only, too short (< 6 chars), or vacuous placeholder evidence.
        """
        if not evidence:
            return False
        clean = evidence.strip().lower()
        if len(clean) < 6:
            return False
        generic_placeholders = {
            "none", "n/a", "na", "null", "unknown", "not visible", "unseen",
            "assumed", "guessed", "typical", "inferred", "expected", "standard",
            "present", "visible", "appliance", "room"
        }
        if clean in generic_placeholders:
            return False
        return True

    @staticmethod
    async def analyze_media(media_bytes: bytes, mime_type: str, source: str = "UPLOAD") -> DetectionResult:
        """Analyze uploaded image or video using Gemini 3.8 Flash asynchronously without blocking event loop."""
        input_type = "video" if mime_type.startswith("video/") else "image"
        logger.info(
            f"[LENS_UPLOAD] provider=gemini model={GEMINI_MODEL} input_type={input_type} "
            f"size_bytes={len(media_bytes)} mime_type={mime_type} timeout={PROVIDER_TIMEOUT_SECONDS}s"
        )

        client = _get_gemini_client()

        def _sync_generate():
            part = types.Part.from_bytes(data=media_bytes, mime_type=mime_type)
            return client.models.generate_content(
                model=GEMINI_MODEL,
                contents=[part, LENS_UPLOAD_PROMPT],
                config=types.GenerateContentConfig(
                    response_mime_type="application/json",
                    temperature=0.1,
                ),
            )

        try:
            # Offload synchronous Gemini call from async event loop and apply bounded timeout
            response = await asyncio.wait_for(
                asyncio.to_thread(_sync_generate),
                timeout=PROVIDER_TIMEOUT_SECONDS,
            )
        except asyncio.TimeoutError:
            logger.error(f"[LENS_UPLOAD] Gemini vision analysis timed out after {PROVIDER_TIMEOUT_SECONDS}s")
            raise HTTPException(
                status_code=504,
                detail=f"AI vision analysis timed out after {PROVIDER_TIMEOUT_SECONDS} seconds. Please retry."
            )
        except HTTPException:
            raise
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
        if not text:
            logger.error("[LENS_UPLOAD] Empty response text received from Gemini")
            raise HTTPException(
                status_code=502,
                detail="Empty response received from AI vision provider. Please retry."
            )

        data = None
        try:
            data = json.loads(text)
        except json.JSONDecodeError:
            match = re.search(r"\{[\s\S]*\}", text)
            if match:
                try:
                    data = json.loads(match.group(0))
                except Exception:
                    data = None

        if not isinstance(data, dict):
            logger.error(f"[LENS_UPLOAD] Could not extract valid JSON dictionary from Gemini response: {text[:200]}")
            raise HTTPException(
                status_code=502,
                detail="Malformed response received from AI vision provider. Please retry."
            )

        raw_appliances = data.get("detected_appliances")
        if raw_appliances is None:
            raw_appliances = data.get("appliances")
        if raw_appliances is None:
            raw_appliances = data.get("detections")

        if raw_appliances is None or not isinstance(raw_appliances, list):
            logger.error(f"[LENS_UPLOAD] Invalid response schema: expected list of appliances, got: {type(raw_appliances).__name__}")
            raise HTTPException(
                status_code=502,
                detail="Invalid response schema: missing detected_appliances list from AI vision provider."
            )

        detected_appliances = []
        parsed_detections = []

        for item in raw_appliances:
            if not isinstance(item, dict):
                continue
            app_type = str(item.get("appliance_type") or item.get("label") or "Appliance").strip()[:100] or "Appliance"
            app_name = str(item.get("appliance_name") or app_type).strip()[:100] or app_type
            qty = ApplianceAnalysisService._parse_quantity(item.get("quantity"))
            power_watts = ApplianceAnalysisService._parse_power_watts(item.get("power_rating_watts"))
            conf = ApplianceAnalysisService._parse_confidence(item.get("confidence"), default=0.9)
            evidence = item.get("evidence")
            if evidence is not None:
                evidence = str(evidence).strip()[:500]

            # Evidence Validation Policy:
            # Model-reported confidence in LLM JSON outputs is heuristic and uncalibrated.
            # Concrete visual evidence is our primary correctness gate.
            # Detections without meaningful visual evidence (or marked with speculative confidence < 0.3)
            # are rejected to prevent hallucinated items from contaminating inventory.
            if not ApplianceAnalysisService._is_valid_evidence(evidence):
                logger.warning(
                    f"[LENS_UPLOAD] Omitting detection '{app_name}' due to missing or insubstantial visual evidence: '{evidence}'"
                )
                continue

            if conf < 0.3:
                logger.warning(
                    f"[LENS_UPLOAD] Omitting detection '{app_name}' due to low confidence ({conf:.2f}): '{evidence}'"
                )
                continue

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
