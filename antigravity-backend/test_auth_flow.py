import unittest
from unittest.mock import patch, MagicMock
from fastapi.testclient import TestClient
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from sqlalchemy.pool import StaticPool

from src.main import app
from src.core.database import Base, get_db
from src.modules.auth.models import User
from src.modules.auth.repository import AuthRepository
from src.modules.auth.service import AuthService

# Use an in-memory SQLite database for testing
SQLALCHEMY_DATABASE_URL = "sqlite:///:memory:"
engine = create_engine(
    SQLALCHEMY_DATABASE_URL,
    connect_args={"check_same_thread": False},
    poolclass=StaticPool,
)
TestingSessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)

def override_get_db():
    db = TestingSessionLocal()
    try:
        yield db
    finally:
        db.close()

class AuthFlowBackendTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        Base.metadata.create_all(bind=engine)
        app.dependency_overrides[get_db] = override_get_db

    @classmethod
    def tearDownClass(cls):
        Base.metadata.drop_all(bind=engine)
        app.dependency_overrides.clear()

    def setUp(self):
        self.client_context = TestClient(app)
        self.client = self.client_context.__enter__()
        # Clear tables
        db = TestingSessionLocal()
        db.query(User).delete()
        db.commit()
        db.close()

    def tearDown(self):
        self.client_context.__exit__(None, None, None)

    def test_check_username_availability(self):
        # 1. Invalid username (too short)
        res = self.client.get("/auth/check-username?username=ab")
        self.assertEqual(res.status_code, 200)
        self.assertFalse(res.json()["available"])

        # 2. Invalid username (no letters, only digits)
        res = self.client.get("/auth/check-username?username=12345")
        self.assertEqual(res.status_code, 200)
        self.assertFalse(res.json()["available"])

        # 3. Valid available username
        res = self.client.get("/auth/check-username?username=sanjai123")
        self.assertEqual(res.status_code, 200)
        self.assertTrue(res.json()["available"])

        # 4. Insert existing user
        db = TestingSessionLocal()
        repo = AuthRepository(db)
        repo.create_user(email="sanjai@example.com", username="sanjai123", is_verified=True, firebase_uid="uid123")
        db.close()

        # 5. Check duplicate username (case-insensitive)
        res = self.client.get("/auth/check-username?username=Sanjai123")
        self.assertEqual(res.status_code, 200)
        self.assertFalse(res.json()["available"])
        self.assertIn("already taken", res.json()["message"])

    def test_resolve_identifier_email(self):
        # When identifier has @, it should simply return the normalized email
        res = self.client.post("/auth/resolve-identifier", json={"identifier": "  User@Example.COM  "})
        self.assertEqual(res.status_code, 200)
        self.assertEqual(res.json()["email"], "user@example.com")

    def test_resolve_identifier_username(self):
        db = TestingSessionLocal()
        repo = AuthRepository(db)
        repo.create_user(email="sanjai@example.com", username="sanjai123", is_verified=True, firebase_uid="uid123")
        db.close()

        # Found username
        res = self.client.post("/auth/resolve-identifier", json={"identifier": "  SANJAI123  "})
        self.assertEqual(res.status_code, 200)
        self.assertEqual(res.json()["email"], "sanjai@example.com")

        # Non-existent username
        res = self.client.post("/auth/resolve-identifier", json={"identifier": "unknown_user"})
        self.assertEqual(res.status_code, 404)
        self.assertEqual(res.json()["detail"], "Invalid username or password.")

    def test_sync_social_user_uniqueness_and_linking(self):
        db = TestingSessionLocal()
        repo = AuthRepository(db)
        service = AuthService(repo)

        # 1. Sign up new email user with username
        user1 = service.sync_social_user(
            firebase_uid="uid_email_1",
            email="solar_user@example.com",
            username="solar_dev",
            name="Solar Dev",
            is_signup=True
        )
        self.assertEqual(user1.email, "solar_user@example.com")
        self.assertEqual(user1.username, "solar_dev")
        self.assertEqual(user1.firebase_uid, "uid_email_1")

        # 2. Duplicate username signup should raise 409
        from fastapi import HTTPException
        with self.assertRaises(HTTPException) as ctx:
            service.sync_social_user(
                firebase_uid="uid_diff",
                email="other@example.com",
                username="SOLAR_DEV",
                name="Other User",
                is_signup=True
            )
        self.assertEqual(ctx.exception.status_code, 409)

        # 3. Duplicate email signup should raise 409
        with self.assertRaises(HTTPException) as ctx:
            service.sync_social_user(
                firebase_uid="uid_diff2",
                email="solar_user@example.com",
                username="another_user",
                name="Another",
                is_signup=True
            )
        self.assertEqual(ctx.exception.status_code, 409)

        # 4. Sign in with same UID should return existing user
        user_sign_in = service.sync_social_user(
            firebase_uid="uid_email_1",
            email="solar_user@example.com",
            is_signup=False
        )
        self.assertEqual(user_sign_in.id, user1.id)

        # 5. Account linking: In Firebase, linking Google via linkWithCredential preserves the existing UID
        linked_user = service.sync_social_user(
            firebase_uid="uid_email_1",
            email="solar_user@example.com",
            is_signup=False,
            provider="google.com"
        )
        self.assertEqual(linked_user.id, user1.id)
        self.assertEqual(linked_user.firebase_uid, "uid_email_1")

        # 6. Legacy account linking: DB user has no firebase_uid yet, first social login links it
        user_legacy = repo.create_user(email="legacy@example.com", username="legacy_user", is_verified=True, firebase_uid=None)
        linked_legacy = service.sync_social_user(
            firebase_uid="uid_legacy_google",
            email="legacy@example.com",
            is_signup=False,
            provider="google.com"
        )
        self.assertEqual(linked_legacy.id, user_legacy.id)
        self.assertEqual(linked_legacy.firebase_uid, "uid_legacy_google")

        db.close()


if __name__ == "__main__":
    unittest.main()
