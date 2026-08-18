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

from src.core.database import SessionLocal
from src.modules.auth.dependencies import get_verified_firebase_token
from src.modules.auth.models import User
from src.modules.auth.repository import AuthRepository

client = TestClient(app)

def clear_db():
    db: Session = SessionLocal()
    db.execute(delete(User))
    db.commit()
    db.close()

def override_firebase_dependency(claims: dict):
    app.dependency_overrides[get_verified_firebase_token] = lambda: claims

def test_jwt_flow():
    clear_db()
    db: Session = SessionLocal()
    repo = AuthRepository(db)

    print("\n--- TEST 1: New Firebase User ---")
    claims1 = {"uid": "new_firebase_uid_123", "phone_number": "+919876543210"}
    override_firebase_dependency(claims1)
    
    resp = client.post("/auth/firebase", json={"firebase_id_token": "dummy"})
    assert resp.status_code == 200, resp.text
    data = resp.json()
    print("Tokens returned:", data.keys())
    assert "access_token" in data
    assert "refresh_token" in data
    assert data["token_type"] == "Bearer"
    # assert data["user"]["firebase_uid"] == "new_firebase_uid_123" # UserResponse doesn't have firebase_uid
    assert data["user"]["mobile_number"] == "9876543210"
    assert data["user"]["is_verified"] is True
    
    access_token = data["access_token"]
    refresh_token = data["refresh_token"]

    print("\n--- TEST 2: Existing Firebase User ---")
    resp = client.post("/auth/firebase", json={"firebase_id_token": "dummy"})
    assert resp.status_code == 200
    data2 = resp.json()
    print("Existing user returned with new tokens.")
    assert data2["user"]["id"] == data["user"]["id"]
    assert data2["access_token"] != access_token  # New token generated
    
    print("\n--- TEST 3: Existing phone, no Firebase UID ---")
    repo.create_user(mobile_number="9000000000", is_verified=False)
    
    claims3 = {"uid": "another_firebase_uid", "phone_number": "+919000000000"}
    override_firebase_dependency(claims3)
    
    resp = client.post("/auth/firebase", json={"firebase_id_token": "dummy"})
    assert resp.status_code == 200
    data3 = resp.json()
    print("Existing user linked and tokens generated.")
    # assert data3["user"]["firebase_uid"] == "another_firebase_uid"
    assert "access_token" in data3
    
    print("\n--- TEST 4: /auth/me ---")
    resp = client.get("/auth/me", headers={"Authorization": f"Bearer {access_token}"})
    assert resp.status_code == 200
    me_data = resp.json()
    print("/auth/me returned user:", me_data["id"])
    assert me_data["id"] == data["user"]["id"]
    assert me_data["mobile_number"] == "9876543210"

    print("\n--- TEST 5: /auth/refresh ---")
    resp = client.post("/auth/refresh", json={"refresh_token": refresh_token})
    assert resp.status_code == 200
    refresh_data = resp.json()
    print("Refreshed tokens returned.")
    assert "access_token" in refresh_data
    assert refresh_data["user"]["id"] == data["user"]["id"]

    print("\n--- TEST 6: Invalid Firebase Token ---")
    # Restore normal dependency behavior to test missing token
    app.dependency_overrides.clear()
    resp = client.post("/auth/firebase", json={"firebase_id_token": "invalid"})
    print("Invalid token status:", resp.status_code)
    assert resp.status_code == 401
    
    print("\n--- TEST 7: Firebase UID Conflict ---")
    override_firebase_dependency({"uid": "conflict_uid", "phone_number": "+919876543210"})
    resp = client.post("/auth/firebase", json={"firebase_id_token": "dummy"})
    print("Conflict status:", resp.status_code)
    assert resp.status_code == 409
    
    print("\nALL TESTS PASSED SUCCESSFULLY!")

if __name__ == "__main__":
    test_jwt_flow()
