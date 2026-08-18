import sys
from fastapi.testclient import TestClient
from src.main import app  # Assuming src.main contains the FastAPI app, wait let's check
import json

try:
    from src.api.server import app
except ImportError:
    try:
        from src.main import app
    except ImportError:
        print("Could not find the FastAPI app.")
        sys.exit(1)

def test_firebase():
    print("Testing /auth/firebase endpoint...")
    
    with TestClient(app) as client:
        # 1. Test invalid header
        resp = client.post("/auth/firebase", headers={"Authorization": "Bearer invalid_token"})
        print("Header test status:", resp.status_code)
        print("Header test response:", resp.json())
        
        # 2. Test invalid body
        resp = client.post("/auth/firebase", json={"firebase_id_token": "invalid_token"})
        print("Body test status:", resp.status_code)
        print("Body test response:", resp.json())
        
        # 3. Open API docs test
        resp = client.get("/openapi.json")
        if resp.status_code == 200:
            docs = resp.json()
            if "/auth/firebase" in docs["paths"]:
                print("/auth/firebase is present in OpenAPI docs.")
            else:
                print("/auth/firebase is MISSING in OpenAPI docs.")
        else:
            print("Could not fetch OpenAPI docs.")

if __name__ == "__main__":
    test_firebase()
