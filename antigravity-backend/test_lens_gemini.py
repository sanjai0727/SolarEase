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


if __name__ == "__main__":
    unittest.main()
