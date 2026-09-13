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


import random

_PERMANENT_ERROR_TYPES = {
    "authentication",         # invalid/revoked API key
    "permission_denied",      # key lacks permission
    "payment_required",       # account out of credits
    "invalid_request",        # malformed request / unsupported param
    "not_found",              # model does not exist
    "invalid_prompt",         # prompt rejected
    "precondition_failed",    # request precondition not met
    "unprocessable",          # request cannot be processed
}
_PERMANENT_HTTP_CODES = {401, 402, 403, 404, 422}


def _parse_detections_json(raw_text: str, source: str) -> DetectionResult:
    """Parse detection JSON from model response text.

    Handles:
    - Clean JSON
    - Markdown code fences (```json ... ```)
    - Extraneous whitespace / newlines
    - Malformed JSON (raises ValueError to prevent masquerading as empty detections)
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
                raise ValueError("AI provider returned invalid JSON in model response")
        else:
            logger.warning("[LENS] No JSON object found in model response")
            raise ValueError("No JSON object found in AI provider model response")

    if not isinstance(data, dict):
        logger.warning("[LENS] Model response JSON root is not an object")
        raise ValueError("AI provider response root is not a JSON object")

    if "detections" not in data or not isinstance(data["detections"], list):
        logger.warning("[LENS] Model response JSON missing 'detections' list")
        raise ValueError("AI provider response missing 'detections' list")

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

        max_retries = 3
        base_backoff = 1.5
        max_backoff = 8.0

        for attempt in range(1, max_retries + 1):
            logger.info(f"[LENS DIAG] 4. OpenRouter request started (Model: {OPENROUTER_MODEL}), Attempt {attempt}/{max_retries}")
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

                # Permanent errors
                if response.status_code == 401:
                    logger.error("[LENS] OpenRouter authentication failed (401)")
                    raise HTTPException(status_code=500, detail="AI provider authentication failed. Please check server configuration.")

                if response.status_code == 400:
                    logger.error("[LENS] OpenRouter bad request (400)")
                    try:
                        err_data = response.json()
                        logger.error(f"[LENS] OpenRouter error details: {err_data.get('error', {})}")
                    except Exception:
                        pass
                    raise HTTPException(status_code=400, detail="AI provider rejected the request.")

                # Retryable HTTP errors (429, 502, 503, 504, 5xx)
                if response.status_code in (429, 502, 503, 504) or response.status_code >= 500:
                    err_code = None
                    err_msg = ""
                    err_type = ""
                    try:
                        err_data = response.json()
                        if isinstance(err_data, dict) and "error" in err_data:
                            err_obj = err_data["error"]
                            if isinstance(err_obj, dict):
                                err_code = err_obj.get("code")
                                err_msg = err_obj.get("message", "")
                                err_meta = err_obj.get("metadata", {})
                                if isinstance(err_meta, dict):
                                    err_type = err_meta.get("error_type", "")
                            else:
                                err_msg = str(err_obj)
                    except Exception:
                        err_msg = response.text[:200]

                    logger.warning(
                        f"[LENS] OpenRouter retryable error on attempt {attempt}/{max_retries}. "
                        f"Status: {response.status_code}, error.code: {err_code}, "
                        f"error_type: {err_type}, error.message: {err_msg}, Latency: {elapsed_ms}ms"
                    )

                    # Do not retry permanent error types
                    if err_type in _PERMANENT_ERROR_TYPES or (isinstance(err_code, int) and err_code in _PERMANENT_HTTP_CODES):
                        logger.error(
                            f"[LENS] Permanent OpenRouter error encountered with status {response.status_code} — not retrying. "
                            f"error_type: {err_type}, error.code: {err_code}"
                        )
                        raise HTTPException(status_code=500, detail=f"AI provider error: {err_msg}")

                    if attempt == max_retries:
                        raise HTTPException(
                            status_code=503,
                            detail="Solar Lens AI provider is temporarily unavailable. Please try again."
                        )

                    backoff = min(base_backoff * (2 ** (attempt - 1)), max_backoff)
                    sleep_time = random.uniform(0.5, 1.0) * backoff
                    logger.info(f"[LENS] Backoff sleep {sleep_time:.2f}s before attempt {attempt + 1}")
                    await asyncio.sleep(sleep_time)
                    continue

                if response.status_code != 200:
                    logger.error(f"[LENS] OpenRouter unexpected status: {response.status_code}")
                    raise HTTPException(status_code=500, detail=f"AI provider returned unexpected status: {response.status_code}")

                # Parse OpenRouter response
                try:
                    resp_data = response.json()
                except json.JSONDecodeError:
                    logger.error("[LENS] OpenRouter response is not valid JSON")
                    if attempt == max_retries:
                        raise HTTPException(status_code=500, detail="AI provider returned an invalid response.")
                    backoff = min(base_backoff * (2 ** (attempt - 1)), max_backoff)
                    sleep_time = random.uniform(0.5, 1.0) * backoff
                    await asyncio.sleep(sleep_time)
                    continue

                # Check for error field in response body (OpenRouter sometimes returns 200 with error object)
                if "error" in resp_data:
                    err_obj = resp_data["error"]
                    err_code = err_obj.get("code") if isinstance(err_obj, dict) else None
                    err_msg = err_obj.get("message", "unknown") if isinstance(err_obj, dict) else str(err_obj)
                    err_meta = err_obj.get("metadata", {}) if isinstance(err_obj, dict) else {}
                    err_type = err_meta.get("error_type", "") if isinstance(err_meta, dict) else ""

                    logger.warning(
                        f"[LENS] OpenRouter body-level error on attempt {attempt}/{max_retries}. "
                        f"Model: {OPENROUTER_MODEL}, error.code: {err_code}, "
                        f"error.message: {err_msg}, error_type: {err_type}, Latency: {elapsed_ms}ms"
                    )

                    is_permanent = (
                        err_type in _PERMANENT_ERROR_TYPES
                        or (isinstance(err_code, int) and err_code in _PERMANENT_HTTP_CODES)
                    )

                    if is_permanent:
                        logger.error(
                            f"[LENS] Permanent OpenRouter error — not retrying. "
                            f"error_type: {err_type}, error.code: {err_code}"
                        )
                        raise HTTPException(
                            status_code=500,
                            detail=f"AI provider error: {err_msg}"
                        )

                    # Retryable errors — provider_unavailable, rate_limit, server, timeout, etc.
                    if attempt == max_retries:
                        raise HTTPException(
                            status_code=503,
                            detail="Solar Lens AI provider is temporarily unavailable. Please try again."
                        )
                    backoff = min(base_backoff * (2 ** (attempt - 1)), max_backoff)
                    sleep_time = random.uniform(0.5, 1.0) * backoff
                    await asyncio.sleep(sleep_time)
                    continue

                # Extract the model's text content from the Chat Completions response
                choices = resp_data.get("choices", [])
                if not choices:
                    err_info = resp_data.get("error", "No choices returned")
                    logger.warning(f"[LENS] OpenRouter response contains no choices. Error/Info: {err_info}")
                    if attempt == max_retries:
                        raise HTTPException(status_code=503, detail="Solar Lens AI provider is temporarily unavailable. Please try again.")
                    backoff = min(base_backoff * (2 ** (attempt - 1)), max_backoff)
                    sleep_time = random.uniform(0.5, 1.0) * backoff
                    await asyncio.sleep(sleep_time)
                    continue

                message = choices[0].get("message", {})
                content_text = message.get("content", "")

                if not content_text:
                    logger.warning(f"[LENS] OpenRouter response content is empty on attempt {attempt}")
                    if attempt == max_retries:
                        raise HTTPException(status_code=503, detail="Solar Lens AI provider is temporarily unavailable. Please try again.")
                    backoff = min(base_backoff * (2 ** (attempt - 1)), max_backoff)
                    sleep_time = random.uniform(0.5, 1.0) * backoff
                    await asyncio.sleep(sleep_time)
                    continue

                response_length = len(content_text)
                logger.info(f"[LENS DIAG] 5. OpenRouter content length: {response_length}")
                logger.info(f"[LENS DIAG] 6. Response parsing started")

                try:
                    return _parse_detections_json(content_text, source)
                except ValueError as ve:
                    logger.warning(f"[LENS] Model response could not be parsed on attempt {attempt}/{max_retries}: {ve}")
                    if attempt == max_retries:
                        raise HTTPException(
                            status_code=502,
                            detail="AI provider returned an unparseable detection format. Please try again."
                        )
                    backoff = min(base_backoff * (2 ** (attempt - 1)), max_backoff)
                    sleep_time = random.uniform(0.5, 1.0) * backoff
                    await asyncio.sleep(sleep_time)
                    continue

            except HTTPException:
                raise
            except httpx.TimeoutException:
                elapsed_ms = int((time.monotonic() - start_time) * 1000)
                logger.error(f"[LENS] OpenRouter request timed out after {elapsed_ms}ms on attempt {attempt}/{max_retries}")
                if attempt == max_retries:
                    raise HTTPException(status_code=504, detail="Solar Lens analysis timed out. Please try again.")
                backoff = min(base_backoff * (2 ** (attempt - 1)), max_backoff)
                sleep_time = random.uniform(0.5, 1.0) * backoff
                await asyncio.sleep(sleep_time)
            except httpx.RequestError as e:
                logger.error(f"[LENS] OpenRouter network error on attempt {attempt}/{max_retries}: {e.__class__.__name__}")
                if attempt == max_retries:
                    raise HTTPException(status_code=502, detail="Solar Lens could not reach the AI provider. Please check your connection.")
                backoff = min(base_backoff * (2 ** (attempt - 1)), max_backoff)
                sleep_time = random.uniform(0.5, 1.0) * backoff
                await asyncio.sleep(sleep_time)
            except Exception as e:
                logger.exception(f"[LENS DIAG] 8. OpenRouter analysis failed with exception on attempt {attempt}/{max_retries}. Type: {e.__class__.__name__}")
                if attempt == max_retries:
                    raise HTTPException(status_code=500, detail=f"Analysis failed: {str(e)}")
                backoff = min(base_backoff * (2 ** (attempt - 1)), max_backoff)
                sleep_time = random.uniform(0.5, 1.0) * backoff
                await asyncio.sleep(sleep_time)

        raise HTTPException(status_code=503, detail="Solar Lens AI provider is temporarily unavailable after multiple attempts.")
