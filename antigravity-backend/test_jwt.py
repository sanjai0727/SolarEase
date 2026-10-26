import sys
from fastapi.testclient import TestClient
from sqlalchemy.orm import Session
from sqlalchemy import delete

try:
    from src.api.server import app
except ImportError:
    try:
        from src.main import app
    except ImportError:
        print("Could not find the FastAPI app.")
        sys.exit(1)

from sqlalchemy import create_engine
from sqlalchemy.pool import StaticPool
from sqlalchemy.orm import sessionmaker
from src.core.database import Base, get_db
from src.modules.auth.dependencies import get_verified_firebase_token
from src.modules.auth.models import User
from src.modules.auth.repository import AuthRepository

test_engine = create_engine(
    "sqlite:///:memory:",
    connect_args={"check_same_thread": False},
    poolclass=StaticPool
)
TestingSessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=test_engine)
Base.metadata.create_all(bind=test_engine)

def override_get_db():
    db = TestingSessionLocal()
    try:
        yield db
    finally:
        db.close()

app.dependency_overrides[get_db] = override_get_db
client = TestClient(app)

def clear_db():
    db: Session = TestingSessionLocal()
    db.execute(delete(User))
    db.commit()
    db.close()

def override_firebase_dependency(claims: dict):
    app.dependency_overrides[get_verified_firebase_token] = lambda: claims

def test_jwt_flow():
    clear_db()
    db: Session = TestingSessionLocal()
    repo = AuthRepository(db)

    print("\n--- TEST 1: New Firebase User ---")
    claims1 = {"uid": "new_firebase_uid_123", "phone_number": "+919876543210"}
    override_firebase_dependency(claims1)
    
    resp = client.post("/auth/firebase", json={"firebase_id_token": "dummy", "is_signup": True})
    assert resp.status_code == 200, resp.text
    data = resp.json()
    print("Tokens returned:", data.keys())
    assert "access_token" in data
    assert "refresh_token" in data
    assert data["token_type"] == "Bearer"
    assert data["user"]["mobile_number"] == "9876543210"
    assert data["user"]["is_verified"] is True
    
    access_token = data["access_token"]
    refresh_token = data["refresh_token"]

    print("\n--- TEST 2: Existing Firebase User ---")
    resp = client.post("/auth/firebase", json={"firebase_id_token": "dummy", "is_signup": False})
    assert resp.status_code == 200
    data2 = resp.json()
    print("Existing user returned with new tokens.")
    assert data2["user"]["id"] == data["user"]["id"]
    assert data2["access_token"] != access_token  # New token generated
    
    print("\n--- TEST 3: Existing phone, no Firebase UID ---")
    repo.create_user(mobile_number="9000000000", is_verified=False)
    
    claims3 = {"uid": "another_firebase_uid", "phone_number": "+919000000000"}
    override_firebase_dependency(claims3)
    
    resp = client.post("/auth/firebase", json={"firebase_id_token": "dummy", "is_signup": False})
    assert resp.status_code == 200
    data3 = resp.json()
    print("Existing user linked and tokens generated.")
    assert "access_token" in data3
    
    print("\n--- TEST 4: /auth/me ---")
    resp = client.get("/auth/me", headers={"Authorization": f"Bearer {access_token}"})
    assert resp.status_code == 200
    me_data = resp.json()
    print("/auth/me returned user:", me_data["id"])
    assert me_data["id"] == data["user"]["id"]
    assert me_data["mobile_number"] == "9876543210"

    print("\n--- TEST 5: /auth/refresh & Token Rotation ---")
    resp = client.post("/auth/refresh", json={"refresh_token": refresh_token})
    assert resp.status_code == 200
    refresh_data = resp.json()
    print("Refreshed tokens returned.")
    assert "access_token" in refresh_data
    assert "refresh_token" in refresh_data
    assert refresh_data["refresh_token"] != refresh_token  # Refresh token rotated!
    assert refresh_data["user"]["id"] == data["user"]["id"]

    # Re-using the old refresh token must be rejected!
    resp_reuse = client.post("/auth/refresh", json={"refresh_token": refresh_token})
    assert resp_reuse.status_code == 401

    print("\n--- TEST 6: /auth/logout and Revocation ---")
    new_access_token = refresh_data["access_token"]
    resp_logout = client.post("/auth/logout", headers={"Authorization": f"Bearer {new_access_token}"})
    assert resp_logout.status_code == 200

    # Using the logged-out access token must fail with 401
    resp_after_logout = client.get("/auth/me", headers={"Authorization": f"Bearer {new_access_token}"})
    assert resp_after_logout.status_code == 401

    print("\n--- TEST 7: Invalid Firebase Token ---")
    # Restore normal dependency behavior to test missing token
    app.dependency_overrides.pop(get_verified_firebase_token, None)
    resp = client.post("/auth/firebase", json={"firebase_id_token": "invalid"})
    print("Invalid token status:", resp.status_code)
    assert resp.status_code == 401
    
    print("\n--- TEST 8: Firebase UID Conflict ---")
    override_firebase_dependency({"uid": "conflict_uid", "phone_number": "+919876543210"})
    resp = client.post("/auth/firebase", json={"firebase_id_token": "dummy", "is_signup": True})
    print("Conflict status:", resp.status_code)
    assert resp.status_code == 409

    # Clean up overrides to prevent polluting other test suites
    app.dependency_overrides.clear()
    
    print("\nALL AUTH TESTS PASSED SUCCESSFULLY!")

if __name__ == "__main__":
    test_jwt_flow()
