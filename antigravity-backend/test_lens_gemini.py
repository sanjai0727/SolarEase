import os
import unittest
from unittest.mock import patch, MagicMock
import json

os.environ["DEBUG"] = "false"

from src.modules.lens.service import ApplianceAnalysisService, LENS_UPLOAD_PROMPT
from src.modules.lens.schemas import DetectionResult, DetectedAppliance


class GeminiLensUploadUnitTests(unittest.TestCase):

    def test_system_prompt_enforces_rules(self):
        """Verify prompt explicitly enforces null wattage, consolidation, and no hallucinations."""
        self.assertIn("null", LENS_UPLOAD_PROMPT.lower())
        self.assertIn("consolidate", LENS_UPLOAD_PROMPT.lower())
        self.assertIn("never invent", LENS_UPLOAD_PROMPT.lower())
        self.assertIn("electrical appliances", LENS_UPLOAD_PROMPT.lower())

    def test_gemini_response_parsing_with_null_wattage(self):
        """Verify that Gemini JSON response with null power_rating_watts is preserved as None."""
        mock_gemini_json = {
            "appliances": [
                {
                    "appliance_type": "refrigerator",
                    "appliance_name": "Double Door Refrigerator",
                    "quantity": 1,
                    "power_rating_watts": None,
                    "confidence": 0.94,
                    "evidence": "Visible in kitchen corner with metallic finish."
                },
                {
                    "appliance_type": "ceiling fan",
                    "appliance_name": "Ceiling Fan",
                    "quantity": 2,
                    "power_rating_watts": None,
                    "confidence": 0.96,
                    "evidence": "Two fans mounted on ceiling."
                }
            ]
        }

        mock_response = MagicMock()
        mock_response.text = f"```json\n{json.dumps(mock_gemini_json)}\n```"

        mock_client = MagicMock()
        mock_client.models.generate_content.return_value = mock_response

        with patch("src.modules.lens.service._get_gemini_client", return_value=mock_client):
            import asyncio
            result: DetectionResult = asyncio.run(
                ApplianceAnalysisService.analyze_media(b"fake-image-bytes", "image/jpeg", "UPLOAD_IMAGE")
            )

        self.assertEqual(len(result.detections), 3)
        self.assertIsNotNone(result.detected_appliances)
        self.assertEqual(len(result.detected_appliances), 2)

        # Check first appliance
        ref = result.detected_appliances[0]
        self.assertEqual(ref.appliance_type, "refrigerator")
        self.assertEqual(ref.quantity, 1)
        self.assertIsNone(ref.power_rating_watts)
        self.assertEqual(ref.confidence, 0.94)

        # Check second appliance with quantity=2
        fan = result.detected_appliances[1]
        self.assertEqual(fan.appliance_type, "ceiling fan")
        self.assertEqual(fan.quantity, 2)
        self.assertIsNone(fan.power_rating_watts)

        # Check detections list compatibility
        det_ref = result.detections[0]
        self.assertEqual(det_ref.label, "Double Door Refrigerator")
        self.assertIsNone(det_ref.power_rating_watts)
        self.assertEqual(det_ref.quantity, 1)

    def test_gemini_video_upload_call_structure(self):
        """Verify video upload invokes gemini-3.8-flash with video mime type."""
        mock_gemini_json = {
            "appliances": [
                {
                    "appliance_type": "washing machine",
                    "appliance_name": "Front Load Washing Machine",
                    "quantity": 1,
                    "power_rating_watts": None,
                    "confidence": 0.91,
                    "evidence": "Spotted in utility area."
                }
            ]
        }

        mock_response = MagicMock()
        mock_response.text = json.dumps(mock_gemini_json)

        mock_client = MagicMock()
        mock_client.models.generate_content.return_value = mock_response

        with patch("src.modules.lens.service._get_gemini_client", return_value=mock_client):
            import asyncio
            result = asyncio.run(
                ApplianceAnalysisService.analyze_media(b"fake-video-bytes", "video/mp4", "UPLOAD_VIDEO")
            )

        mock_client.models.generate_content.assert_called_once()
        call_kwargs = mock_client.models.generate_content.call_args[1]
        self.assertEqual(call_kwargs["model"], "gemini-3.8-flash")
        contents = call_kwargs["contents"]
        self.assertEqual(len(contents), 2)  # Part and LENS_UPLOAD_PROMPT

        self.assertEqual(len(result.detections), 1)
        self.assertEqual(result.detections[0].label, "Front Load Washing Machine")

    def test_no_openrouter_or_nvidia_reference(self):
        """Strictly verify service does NOT reference OpenRouter or NVIDIA models."""
        import inspect
        service_source = inspect.getsource(ApplianceAnalysisService)
        self.assertNotIn("openrouter", service_source.lower())
        self.assertNotIn("nemotron", service_source.lower())
        self.assertNotIn("nvidia", service_source.lower())

    def test_lens_bug_006_timeout_raises_504(self):
        """LENS-BUG-006: Gemini provider timeout raises HTTP 504 and does not block."""
        import time
        from fastapi import HTTPException

        def delayed_call(*args, **kwargs):
            time.sleep(0.3)
            return MagicMock()

        mock_client = MagicMock()
        mock_client.models.generate_content.side_effect = delayed_call

        with patch("src.modules.lens.service._get_gemini_client", return_value=mock_client), \
             patch("src.modules.lens.service.PROVIDER_TIMEOUT_SECONDS", 0.05):
            import asyncio
            with self.assertRaises(HTTPException) as ctx:
                asyncio.run(
                    ApplianceAnalysisService.analyze_media(b"fake-bytes", "image/jpeg", "UPLOAD_IMAGE")
                )
            self.assertEqual(ctx.exception.status_code, 504)
            self.assertIn("timed out", ctx.exception.detail.lower())

    def test_lens_bug_006_provider_failure_raises_502(self):
        """LENS-BUG-006: Gemini provider failures map cleanly to HTTP 502."""
        from fastapi import HTTPException

        mock_client = MagicMock()
        mock_client.models.generate_content.side_effect = RuntimeError("API connection reset by peer")

        with patch("src.modules.lens.service._get_gemini_client", return_value=mock_client):
            import asyncio
            with self.assertRaises(HTTPException) as ctx:
                asyncio.run(
                    ApplianceAnalysisService.analyze_media(b"fake-bytes", "image/jpeg", "UPLOAD_IMAGE")
                )
            self.assertEqual(ctx.exception.status_code, 502)
            self.assertIn("failed", ctx.exception.detail.lower())

    def test_lens_bug_007_malformed_json_raises_502(self):
        """LENS-BUG-007: Unparseable AI response raises HTTP 502 rather than returning empty detections."""
        from fastapi import HTTPException

        bad_payloads = [
            "",
            "   ",
            "This is plain text with no JSON at all.",
            "```json\n{ \"detected_appliances\": [ broken unclosed json",
            "<div>Internal Server Error 500</div>",
        ]

        for payload in bad_payloads:
            with self.subTest(payload=payload):
                with self.assertRaises(HTTPException) as ctx:
                    ApplianceAnalysisService._parse_gemini_response(payload, "UPLOAD")
                self.assertEqual(ctx.exception.status_code, 502)

    def test_lens_bug_007_invalid_schema_raises_502(self):
        """LENS-BUG-007: JSON missing expected appliance list raises HTTP 502."""
        from fastapi import HTTPException

        invalid_schemas = [
            json.dumps(["not", "a", "dict"]),
            json.dumps({"status": "success", "items_count": 0}),
            json.dumps({"detected_appliances": "not-a-list"}),
        ]

        for payload in invalid_schemas:
            with self.subTest(payload=payload):
                with self.assertRaises(HTTPException) as ctx:
                    ApplianceAnalysisService._parse_gemini_response(payload, "UPLOAD")
                self.assertEqual(ctx.exception.status_code, 502)

    def test_lens_bug_007_valid_empty_appliances_preserved(self):
        """LENS-BUG-007: Valid scan with 0 appliances returns empty detections without error."""
        valid_empty = json.dumps({"detected_appliances": []})
        result = ApplianceAnalysisService._parse_gemini_response(valid_empty, "UPLOAD")
        self.assertEqual(len(result.detections), 0)
        self.assertEqual(len(result.detected_appliances), 0)

    def test_lens_bug_008_quantity_parsing_bounds_and_safety(self):
        """LENS-BUG-008: Validate AI-generated quantities safely across types, strings, and bounds."""
        parse_qty = ApplianceAnalysisService._parse_quantity

        # Standard integers
        self.assertEqual(parse_qty(1), 1)
        self.assertEqual(parse_qty(5), 5)

        # Word numbers
        self.assertEqual(parse_qty("two"), 2)
        self.assertEqual(parse_qty("three"), 3)
        self.assertEqual(parse_qty("pair"), 2)
        self.assertEqual(parse_qty("single"), 1)
        self.assertEqual(parse_qty("five"), 5)

        # Numeric strings & floats
        self.assertEqual(parse_qty("4"), 4)
        self.assertEqual(parse_qty("2.0"), 2)
        self.assertEqual(parse_qty(3.7), 4)
        self.assertEqual(parse_qty(1.2), 1)

        # Missing / None
        self.assertEqual(parse_qty(None), 1)

        # Negative / Zero clamp to min 1
        self.assertEqual(parse_qty(0), 1)
        self.assertEqual(parse_qty(-10), 1)
        self.assertEqual(parse_qty("0"), 1)
        self.assertEqual(parse_qty("-5"), 1)

        # Excessive values clamp to max 50
        self.assertEqual(parse_qty(100), 50)
        self.assertEqual(parse_qty(999999), 50)
        self.assertEqual(parse_qty("500"), 50)

        # Unparseable strings fallback safely to 1
        self.assertEqual(parse_qty("several"), 1)
        self.assertEqual(parse_qty("many"), 1)
        self.assertEqual(parse_qty("N/A"), 1)

        # Special float values
        self.assertEqual(parse_qty(float("nan")), 1)
        self.assertEqual(parse_qty(float("inf")), 1)
        self.assertEqual(parse_qty(float("-inf")), 1)

    def test_lens_bug_008_power_watts_parsing_bounds(self):
        """LENS-BUG-008: Validate power rating parsing and household range bounds."""
        parse_watts = ApplianceAnalysisService._parse_power_watts

        # Plausible values
        self.assertEqual(parse_watts(150), 150.0)
        self.assertEqual(parse_watts(1500.5), 1500.5)
        self.assertEqual(parse_watts("2000"), 2000.0)

        # None / missing
        self.assertIsNone(parse_watts(None))

        # Invalid / Out of bounds
        self.assertIsNone(parse_watts(0))
        self.assertIsNone(parse_watts(-50))
        self.assertIsNone(parse_watts(100000))
        self.assertIsNone(parse_watts("invalid"))
        self.assertIsNone(parse_watts(float("nan")))
        self.assertIsNone(parse_watts(float("inf")))

    def test_strict_visual_evidence_prompt_contains_negative_constraints(self):
        """Regression: Prompt must explicitly prohibit guessing, context inference, and checklist matching."""
        prompt_lower = LENS_UPLOAD_PROMPT.lower()
        self.assertIn("do not guess", prompt_lower)
        self.assertIn("visual proof", prompt_lower)
        self.assertIn("refrigerator is visible", prompt_lower)
        self.assertIn("normalization", prompt_lower)

    def test_refrigerator_only_payload_does_not_gain_unrelated_appliances(self):
        """Regression: A refrigerator detection with evidence must not append laptop, AC, or tube light."""
        gemini_json = {
            "detected_appliances": [
                {
                    "appliance_type": "Refrigerator",
                    "appliance_name": "Single Door Refrigerator",
                    "quantity": 1,
                    "power_rating_watts": None,
                    "confidence": 0.95,
                    "evidence": "Visible tall silver refrigerator unit in kitchen corner."
                }
            ]
        }
        result = ApplianceAnalysisService._parse_gemini_response(json.dumps(gemini_json), "UPLOAD")
        labels = [d.appliance_type for d in result.detected_appliances]
        self.assertEqual(labels, ["Refrigerator"])
        self.assertNotIn("Air Conditioner", labels)
        self.assertNotIn("Laptop", labels)
        self.assertNotIn("LED Tube Light", labels)
        self.assertIsNone(result.detected_appliances[0].power_rating_watts)

    def test_laptop_only_payload_does_not_gain_refrigerator(self):
        """Regression: A laptop detection must not cause a refrigerator to be added."""
        gemini_json = {
            "detected_appliances": [
                {
                    "appliance_type": "Computer / Laptop",
                    "appliance_name": "Silver Laptop",
                    "quantity": 1,
                    "power_rating_watts": 65.0,
                    "confidence": 0.92,
                    "evidence": "Open laptop screen and keyboard visible on wooden desk."
                }
            ]
        }
        result = ApplianceAnalysisService._parse_gemini_response(json.dumps(gemini_json), "UPLOAD")
        labels = [d.appliance_type for d in result.detected_appliances]
        self.assertEqual(labels, ["Computer / Laptop"])
        self.assertNotIn("Refrigerator", labels)

    def test_detections_lacking_evidence_are_omitted(self):
        """Regression: Detections with missing, empty, or generic placeholder evidence are rejected."""
        gemini_json = {
            "detected_appliances": [
                {
                    "appliance_type": "Refrigerator",
                    "appliance_name": "Refrigerator",
                    "quantity": 1,
                    "confidence": 0.9,
                    "evidence": "Clearly visible in center frame."
                },
                {
                    "appliance_type": "Air Conditioner",
                    "appliance_name": "Air Conditioner",
                    "quantity": 1,
                    "confidence": 0.85,
                    "evidence": "none"  # Generic placeholder -> reject
                },
                {
                    "appliance_type": "Laptop",
                    "appliance_name": "Laptop",
                    "quantity": 1,
                    "confidence": 0.85,
                    "evidence": None  # Missing evidence -> reject
                },
                {
                    "appliance_type": "LED Tube Light",
                    "appliance_name": "LED Tube Light",
                    "quantity": 1,
                    "confidence": 0.85,
                    "evidence": "seen"  # Too short (< 6 chars) -> reject
                }
            ]
        }
        result = ApplianceAnalysisService._parse_gemini_response(json.dumps(gemini_json), "UPLOAD")
        self.assertEqual(len(result.detected_appliances), 1)
        self.assertEqual(result.detected_appliances[0].appliance_type, "Refrigerator")

    def test_low_confidence_speculative_detection_omitted(self):
        """Regression: Speculative detections with confidence < 0.3 are omitted."""
        gemini_json = {
            "detected_appliances": [
                {
                    "appliance_type": "Television",
                    "appliance_name": "Possible TV",
                    "quantity": 1,
                    "confidence": 0.15,
                    "evidence": "Very dark shadow on the wall that might be a screen."
                }
            ]
        }
        result = ApplianceAnalysisService._parse_gemini_response(json.dumps(gemini_json), "UPLOAD")
        self.assertEqual(len(result.detected_appliances), 0)


if __name__ == "__main__":
    unittest.main()

