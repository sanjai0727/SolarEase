from sqlalchemy.orm import Session
from sqlalchemy.future import select
from fastapi import HTTPException
import asyncio
import uuid
import base64
import re
import time
from typing import List
import json
import logging
import httpx
from src.core.config import settings
from .models import Appliance
from .schemas import ApplianceCreate, ApplianceUpdate, DetectionResult, Detection, BoundingBox

logger = logging.getLogger(__name__)# OpenRouter configuration
OPENROUTER_MODEL = "nvidia/nemotron-3-nano-omni-30b-a3b-reasoning:free"
OPENROUTER_API_URL = "https://openrouter.ai/api/v1/chat/completions"

# Shared Solar Lens analysis prompt
LENS_PROMPT = """You are the visual detection engine for SolarEase, a household energy analysis application.

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
10. Only detect an appliance when there is sufficient visual evidence that the object belongs to one of the five supported categories. Do not classify based solely on context or assumptions.
11. Ignore all unsupported objects.
12. If no supported appliance is visible, return an empty detections array.

Bounding boxes must tightly surround the visible appliance.
For partially visible appliances, the bounding box must cover only the visible portion of the appliance.
Do not extend the bounding box into areas where the appliance is not visible.

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

Return ONLY valid JSON (no markdown, no explanation) in exactly this format:
{"detections": [{"label": "<category>", "confidence": <0.0-1.0>, "box_2d": [<ymin>, <xmin>, <ymax>, <xmax>]}]}

box_2d values must be integers from 0 to 1000 representing normalized coordinates.
If no supported appliance is visible, return: {"detections": []}"""


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


def _parse_detections_json(raw_text: str, source: str) -> DetectionResult:
    """Parse detection JSON from model response text.

    Handles:
    - Clean JSON
    - Markdown code fences (```json ... ```)
    - Extraneous whitespace / newlines
    - Malformed JSON (returns empty detections)
    """
    text = raw_text.strip()

    # Strip markdown code fences if present
    fence_match = re.search(r"```(?:json)?\s*([\s\S]*?)```", text)
    if fence_match:
        text = fence_match.group(1).strip()

    try:
        data = json.loads(text)
    except json.JSONDecodeError:
        # Try to extract the first JSON object from the text
        obj_match = re.search(r"\{[\s\S]*\}", text)
        if obj_match:
            try:
                data = json.loads(obj_match.group(0))
            except json.JSONDecodeError:
                logger.warning("[LENS] Failed to parse model response as JSON")
                return DetectionResult(detections=[])
        else:
            logger.warning("[LENS] No JSON object found in model response")
            return DetectionResult(detections=[])

    parsed_detections = []
    for det in data.get("detections", []):
        label = det.get("label")
        if not label:
            continue

        confidence = 0.0
        try:
            confidence = float(det.get("confidence", 0.0))
        except (ValueError, TypeError):
            pass

        # box_2d is [ymin, xmin, ymax, xmax] normalized from 0-1000
        box = det.get("box_2d", [0, 0, 0, 0])
        if not isinstance(box, list) or len(box) != 4:
            box = [0, 0, 0, 0]

        try:
            ymin, xmin, ymax, xmax = [int(v) for v in box]
        except (ValueError, TypeError):
            ymin, xmin, ymax, xmax = 0, 0, 0, 0

        b_box = BoundingBox(
            left=xmin / 1000.0,
            top=ymin / 1000.0,
            right=xmax / 1000.0,
            bottom=ymax / 1000.0
        )
        parsed_detections.append(
            Detection(
                id=f"det_{uuid.uuid4().hex[:8]}",
                label=label,
                confidence=confidence,
                source=source,
                bounding_box=b_box
            )
        )

    logger.info(f"[LENS DIAG] 7. Detections returned: {len(parsed_detections)}")
    return DetectionResult(detections=parsed_detections)


class ApplianceAnalysisService:
    @staticmethod
    async def analyze_image(image_bytes: bytes, mime_type: str, source: str = "PHOTO") -> DetectionResult:
        if not settings.OPENROUTER_API_KEY:
            raise HTTPException(
                status_code=500,
                detail="OPENROUTER_API_KEY is missing. Solar Lens requires an OpenRouter API key."
            )

        logger.info(f"[LENS] Lens service started. Model: {OPENROUTER_MODEL}")

        # Encode image as base64 data URL for the OpenRouter multimodal message
        image_b64 = base64.b64encode(image_bytes).decode("utf-8")
        image_data_url = f"data:{mime_type};base64,{image_b64}"

        logger.info(f"[LENS DIAG] 4. OpenRouter request started (Model: {OPENROUTER_MODEL})")

        headers = {
            "Authorization": f"Bearer {settings.OPENROUTER_API_KEY}",
            "Content-Type": "application/json",
            "HTTP-Referer": "https://solarease.app",
            "X-Title": "SolarEase Solar Lens",
        }

        payload = {
            "model": OPENROUTER_MODEL,
            "messages": [
                {
                    "role": "user",
                    "content": [
                        {
                            "type": "text",
                            "text": LENS_PROMPT,
                        },
                        {
                            "type": "image_url",
                            "image_url": {
                                "url": image_data_url,
                            },
                        },
                    ],
                }
            ],
            "temperature": 0.2,
            "max_tokens": 1000,
        }

        start_time = time.monotonic()

        try:
            async with httpx.AsyncClient(timeout=60.0) as client:
                response = await client.post(
                    OPENROUTER_API_URL,
                    headers=headers,
                    json=payload,
                )

            elapsed_ms = int((time.monotonic() - start_time) * 1000)
            logger.info(f"[LENS DIAG] 5. OpenRouter response received. Status: {response.status_code}, Latency: {elapsed_ms}ms")

            # Handle OpenRouter HTTP errors
            if response.status_code == 401:
                logger.error("[LENS] OpenRouter authentication failed (401)")
                raise HTTPException(status_code=500, detail="AI provider authentication failed. Please check server configuration.")

            if response.status_code == 429:
                logger.warning("[LENS] OpenRouter rate limit exceeded (429)")
                raise HTTPException(status_code=503, detail="Solar Lens analysis is temporarily unavailable due to rate limits. Please try again.")

            if response.status_code >= 500:
                logger.error(f"[LENS] OpenRouter server error ({response.status_code})")
                raise HTTPException(status_code=503, detail="Solar Lens AI provider is temporarily unavailable. Please try again.")

            if response.status_code != 200:
                logger.error(f"[LENS] OpenRouter unexpected status: {response.status_code}")
                raise HTTPException(status_code=500, detail=f"AI provider returned unexpected status: {response.status_code}")

            # Parse OpenRouter response
            try:
                resp_data = response.json()
            except json.JSONDecodeError:
                logger.error("[LENS] OpenRouter response is not valid JSON")
                raise HTTPException(status_code=500, detail="AI provider returned an invalid response.")

            # Extract the model's text content from the Chat Completions response
            choices = resp_data.get("choices", [])
            if not choices:
                logger.warning("[LENS] OpenRouter response contains no choices")
                return DetectionResult(detections=[])

            message = choices[0].get("message", {})
            content_text = message.get("content", "")

            if not content_text:
                logger.warning("[LENS] OpenRouter response content is empty")
                return DetectionResult(detections=[])

            response_length = len(content_text)
            logger.info(f"[LENS DIAG] 5. OpenRouter content length: {response_length}")
            logger.info(f"[LENS DIAG] 6. Response parsing started")

            return _parse_detections_json(content_text, source)

        except HTTPException:
            raise
        except httpx.TimeoutException:
            elapsed_ms = int((time.monotonic() - start_time) * 1000)
            logger.error(f"[LENS] OpenRouter request timed out after {elapsed_ms}ms")
            raise HTTPException(status_code=504, detail="Solar Lens analysis timed out. Please try again.")
        except httpx.RequestError as e:
            logger.error(f"[LENS] OpenRouter network error: {e.__class__.__name__}")
            raise HTTPException(status_code=502, detail="Solar Lens could not reach the AI provider. Please check your connection.")
        except Exception as e:
            logger.exception(f"[LENS DIAG] 8. OpenRouter analysis failed with exception. Type: {e.__class__.__name__}")
            raise HTTPException(status_code=500, detail=f"Analysis failed: {str(e)}")
