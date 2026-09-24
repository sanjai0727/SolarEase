import os
import unittest
from unittest.mock import patch

# The developer .env currently has a non-boolean DEBUG value.  Tests must set
# a valid runtime override before importing Settings.
os.environ["DEBUG"] = "false"

from fastapi.testclient import TestClient

from src.main import app
from src.modules.auth import dependencies
from src.modules.lens.schemas import DetectionResult
from src.modules.lens.service import ApplianceAnalysisService


class LensAuthenticationTests(unittest.TestCase):
    def setUp(self):
        self.client_context = TestClient(app)
        self.client = self.client_context.__enter__()

    def tearDown(self):
        self.client_context.__exit__(None, None, None)

    @staticmethod
    def request(client, authorization=None):
        headers = {"Authorization": authorization} if authorization else {}
        return client.post(
            "/lens/analyze",
            headers=headers,
            data={"source": "PHOTO"},
            files={"image": ("scan.jpg", b"test-image", "image/jpeg")},
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

    def test_verified_firebase_token_reaches_lens_service(self):
        async def analyze_image(*_args, **_kwargs):
            return DetectionResult(detections=[])

        with (
            patch.object(
                dependencies.firebase_auth,
                "verify_id_token",
                return_value={"uid": "verified-test-user"},
            ) as verify_id_token,
            patch.object(ApplianceAnalysisService, "analyze_image", side_effect=analyze_image) as service,
        ):
            response = self.request(self.client, "Bearer verified-test-token")

        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json(), {"detections": []})
        verify_id_token.assert_called_once_with("verified-test-token", check_revoked=True)
        service.assert_called_once()


if __name__ == "__main__":
    unittest.main()
