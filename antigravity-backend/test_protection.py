import unittest
from unittest.mock import patch
from fastapi.testclient import TestClient
from src.main import app
from src.modules.auth import dependencies

class ProtectionAndSecurityTests(unittest.TestCase):
    def setUp(self):
        self.client_context = TestClient(app)
        self.client = self.client_context.__enter__()

    def tearDown(self):
        self.client_context.__exit__(None, None, None)
        app.dependency_overrides.clear()

    def test_health_and_ready_endpoints(self):
        resp_health = self.client.get("/health")
        self.assertEqual(resp_health.status_code, 200)
        self.assertEqual(resp_health.json()["status"], "healthy")

        resp_ready = self.client.get("/ready")
        self.assertEqual(resp_ready.status_code, 200)

    def test_chatbot_requires_authentication(self):
        resp = self.client.post("/chatbot/query", json={
            "query": "How do solar panels work?",
            "mode": "citizen",
            "language": "en"
        })
        self.assertEqual(resp.status_code, 401)

    def test_chatbot_rejects_oversized_query(self):
        from unittest.mock import MagicMock
        long_query = "a" * 1001
        with patch.object(dependencies, "decode_token", return_value={"type": "access", "sub": "00000000-0000-0000-0000-000000000000"}):
            with patch.object(dependencies.AuthRepository, "get_user_by_id", return_value=MagicMock()):
                with patch.object(dependencies.AuthRepository, "is_token_revoked", return_value=False):
                    resp = self.client.post(
                        "/chatbot/query",
                        headers={"Authorization": "Bearer mock-access-token"},
                        json={"query": long_query, "mode": "citizen", "language": "en"}
                    )
                    self.assertEqual(resp.status_code, 422)

    def test_chatbot_rejects_invalid_mode(self):
        from unittest.mock import MagicMock
        with patch.object(dependencies, "decode_token", return_value={"type": "access", "sub": "00000000-0000-0000-0000-000000000000"}):
            with patch.object(dependencies.AuthRepository, "get_user_by_id", return_value=MagicMock()):
                with patch.object(dependencies.AuthRepository, "is_token_revoked", return_value=False):
                    resp = self.client.post(
                        "/chatbot/query",
                        headers={"Authorization": "Bearer mock-access-token"},
                        json={"query": "test query", "mode": "hacker_mode", "language": "en"}
                    )
                    self.assertEqual(resp.status_code, 422)

    def test_lens_rejects_oversized_file(self):
        with patch.object(
            dependencies.firebase_auth,
            "verify_id_token",
            return_value={"uid": "verified-test-user"},
        ):
            # Send file exceeding 10MB limit (10MB + 1KB)
            big_data = b"0" * (10 * 1024 * 1024 + 1024)
            resp = self.client.post(
                "/lens/analyze",
                headers={"Authorization": "Bearer valid-firebase-token"},
                data={"source": "PHOTO"},
                files={"image": ("big_photo.jpg", big_data, "image/jpeg")}
            )
            self.assertEqual(resp.status_code, 413)
            self.assertIn("exceeds maximum permitted size", resp.json()["detail"])
