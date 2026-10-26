import os
import unittest
from unittest.mock import patch

# Set a valid runtime override before importing Settings
os.environ["DEBUG"] = "false"

from fastapi.testclient import TestClient

from src.main import app
from src.modules.auth import dependencies
from src.modules.lens.schemas import DetectionResult, DetectedAppliance
from src.modules.lens.service import ApplianceAnalysisService


class LensAuthenticationTests(unittest.TestCase):
    def setUp(self):
        self.client_context = TestClient(app)
        self.client = self.client_context.__enter__()

    def tearDown(self):
        self.client_context.__exit__(None, None, None)

    @staticmethod
    def request(client, authorization=None, filename="scan.jpg", content_type="image/jpeg", data_bytes=b"test-image"):
        headers = {"Authorization": authorization} if authorization else {}
        return client.post(
            "/lens/analyze",
            headers=headers,
            data={"source": "PHOTO"},
            files={"image": (filename, data_bytes, content_type)},
        )

    def test_missing_authorization_is_rejected(self):
        response = self.request(self.client)

        self.assertEqual(response.status_code, 401)
        self.assertEqual(response.json()["detail"], "Firebase authentication token missing.")

    def test_invalid_firebase_token_is_rejected(self):
        with patch.object(
            dependencies.firebase_auth,
            "verify_id_token",
            side_effect=dependencies.firebase_auth.InvalidIdTokenError("invalid"),
        ):
            response = self.request(self.client, "Bearer invalid-token")

        self.assertEqual(response.status_code, 401)
        self.assertEqual(response.json()["detail"], "Invalid or expired Firebase authentication token.")

    def test_verified_firebase_token_reaches_lens_service_for_image(self):
        async def analyze_media(*_args, **_kwargs):
            return DetectionResult(
                detections=[],
                detected_appliances=[
                    DetectedAppliance(
                        appliance_type="refrigerator",
                        appliance_name="Refrigerator",
                        quantity=1,
                        power_rating_watts=None,
                        confidence=0.95,
                        evidence="Single refrigerator visible."
                    )
                ]
            )

        with (
            patch.object(
                dependencies.firebase_auth,
                "verify_id_token",
                return_value={"uid": "verified-test-user"},
            ) as verify_id_token,
            patch.object(ApplianceAnalysisService, "analyze_media", side_effect=analyze_media) as service,
        ):
            response = self.request(self.client, "Bearer verified-test-token")

        self.assertEqual(response.status_code, 200)
        json_resp = response.json()
        self.assertIn("detected_appliances", json_resp)
        self.assertEqual(len(json_resp["detected_appliances"]), 1)
        self.assertIsNone(json_resp["detected_appliances"][0]["power_rating_watts"])
        verify_id_token.assert_called_once_with("verified-test-token", check_revoked=True)
        service.assert_called_once()

    def test_verified_firebase_token_reaches_lens_service_for_video(self):
        async def analyze_media(*_args, **_kwargs):
            return DetectionResult(
                detections=[],
                detected_appliances=[
                    DetectedAppliance(
                        appliance_type="ceiling fan",
                        appliance_name="Ceiling Fan",
                        quantity=2,
                        power_rating_watts=None,
                        confidence=0.92,
                        evidence="Two ceiling fans detected."
                    )
                ]
            )

        with (
            patch.object(
                dependencies.firebase_auth,
                "verify_id_token",
                return_value={"uid": "verified-test-user"},
            ) as verify_id_token,
            patch.object(ApplianceAnalysisService, "analyze_media", side_effect=analyze_media) as service,
        ):
            response = self.request(
                self.client,
                authorization="Bearer verified-test-token",
                filename="room_scan.mp4",
                content_type="video/mp4",
                data_bytes=b"dummy-video-data"
            )

        self.assertEqual(response.status_code, 200)
        json_resp = response.json()
        self.assertIn("detected_appliances", json_resp)
        self.assertEqual(json_resp["detected_appliances"][0]["quantity"], 2)
        verify_id_token.assert_called_once_with("verified-test-token", check_revoked=True)
        service.assert_called_once()

    def test_unsupported_file_type_is_rejected(self):
        with patch.object(
            dependencies.firebase_auth,
            "verify_id_token",
            return_value={"uid": "verified-test-user"},
        ):
            response = self.request(
                self.client,
                authorization="Bearer verified-test-token",
                filename="notes.txt",
                content_type="text/plain",
                data_bytes=b"hello world"
            )

        self.assertEqual(response.status_code, 400)
        self.assertIn("Unsupported file type", response.json()["detail"])


if __name__ == "__main__":
    unittest.main()
