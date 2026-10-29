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

def test_sync():
    clear_db()
    db: Session = SessionLocal()
    repo = AuthRepository(db)

    print("\n--- TEST 1: Completely new phone ---")
    claims1 = {"uid": "new_firebase_uid_123", "phone_number": "+919876543210"}
    override_firebase_dependency(claims1)
    
    resp = client.post("/auth/firebase", json={"firebase_id_token": "dummy"})
    assert resp.status_code == 200, resp.text
    data = resp.json()
    print("New user created:", data)
    assert data["user"]["mobile_number"] == "9876543210"
    assert data["user"]["is_verified"] is True
    
    # Verify in DB
    user = repo.get_user_by_mobile("9876543210")
    assert user is not None
    assert user.firebase_uid == "new_firebase_uid_123"

    print("\n--- TEST 2: Existing Firebase UID ---")
    # Same claims, should return the exact same user
    resp = client.post("/auth/firebase", json={"firebase_id_token": "dummy"})
    assert resp.status_code == 200
    data2 = resp.json()
    print("Existing user returned:", data2)
    assert data2["user"]["id"] == data["user"]["id"]
    
    print("\n--- TEST 3: Existing phone, no Firebase UID ---")
    # Create user manually without firebase_uid
    repo.create_user(mobile_number="9000000000", is_verified=False)
    
    claims3 = {"uid": "another_firebase_uid", "phone_number": "+919000000000"}
    override_firebase_dependency(claims3)
    
    resp = client.post("/auth/firebase", json={"firebase_id_token": "dummy"})
    assert resp.status_code == 200
    data3 = resp.json()
    print("Existing user linked:", data3)
    assert data3["user"]["mobile_number"] == "9000000000"
    assert data3["user"]["is_verified"] is True
    
    print("\n--- TEST 4: Missing phone number (Google Sign-In) ---")
    claims4 = {"uid": "google_user_123", "email": "google@example.com", "name": "Google User"}
    override_firebase_dependency(claims4)
    resp = client.post("/auth/firebase", json={"firebase_id_token": "dummy"})
    print("Google Sign-In status:", resp.status_code, resp.json())
    assert resp.status_code == 200
    data4 = resp.json()
    assert data4["user"]["email"] == "google@example.com"
    assert data4["user"]["mobile_number"] is None
    
    print("\n--- TEST 4b: Missing both phone and email ---")
    claims4b = {"uid": "no_phone_no_email_uid"}
    override_firebase_dependency(claims4b)
    resp = client.post("/auth/firebase", json={"firebase_id_token": "dummy"})
    print("Missing phone and email status:", resp.status_code, resp.json())
    assert resp.status_code == 400
    
    print("\n--- TEST 5: Firebase UID Conflict ---")
    # mobile_number 9876543210 is linked to new_firebase_uid_123
    # Try to link it to conflict_uid
    claims5 = {"uid": "conflict_uid", "phone_number": "+919876543210"}
    override_firebase_dependency(claims5)
    resp = client.post("/auth/firebase", json={"firebase_id_token": "dummy"})
    print("Conflict status:", resp.status_code, resp.json())
    assert resp.status_code == 409
    
    print("\nALL TESTS PASSED SUCCESSFULLY!")

if __name__ == "__main__":
    test_sync()
