import sys
import uuid
from datetime import date, timedelta
from fastapi import FastAPI, Depends
from fastapi.testclient import TestClient
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker, Session

from src.core.database import Base, get_db
from src.modules.auth.dependencies import get_current_user
from src.modules.auth.models import User
from src.modules.meter.models import BillingCycle, MeterReading
from src.modules.meter.router import router as meter_router
from src.modules.meter.calculator import calculate_tneb_bill

from sqlalchemy.pool import StaticPool

# Setup test SQLite DB
SQLALCHEMY_DATABASE_URL = "sqlite:///:memory:"
engine = create_engine(
    SQLALCHEMY_DATABASE_URL,
    connect_args={"check_same_thread": False},
    poolclass=StaticPool
)
TestingSessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine, expire_on_commit=False)

Base.metadata.create_all(bind=engine)

test_user = User(
    id=uuid.uuid4(),
    mobile_number="9876543210",
    name="Sanjai Tester",
    username="sanjai_tester",
    is_verified=True
)

def override_get_db():
    db = TestingSessionLocal()
    try:
        yield db
    finally:
        db.close()

def override_get_current_user():
    return test_user

app = FastAPI()
app.include_router(meter_router)
app.dependency_overrides[get_db] = override_get_db
app.dependency_overrides[get_current_user] = override_get_current_user

client = TestClient(app)

def test_meter_flow():
    print("========================================")
    print("RUNNING BACKEND METER READING TESTS")
    print("========================================")

    # Insert user in DB
    db = TestingSessionLocal()
    db.add(test_user)
    db.commit()
    db.close()

    # 1. Test calculation engine standalone
    print("[1] Testing Tariff Calculator...")
    res_zero = calculate_tneb_bill(0.0)
    assert res_zero["total_estimated_bill"] == 0.0, f"Expected 0.0, got {res_zero}"

    # 132 units (<= 500 units, 200 free -> 0 chargeable)
    res_132 = calculate_tneb_bill(132.0)
    assert res_132["free_units"] == 200.0
    assert res_132["chargeable_units"] == 0.0
    assert res_132["total_estimated_bill"] == 0.0

    # 350 units (<= 500 units, 200 free, 150 @ 4.70 = 705.0)
    res_350 = calculate_tneb_bill(350.0)
    assert res_350["chargeable_units"] == 150.0
    assert res_350["total_estimated_bill"] == 705.0, f"Expected 705.0, got {res_350['total_estimated_bill']}"

    # 600 units (> 500 units, 100 free, 500 chargeable)
    # 300 @ 4.70 (1410) + 100 @ 6.30 (630) + 100 @ 8.40 (840) = 2880.0
    res_600 = calculate_tneb_bill(600.0)
    assert res_600["total_estimated_bill"] == 2880.0, f"Expected 2880.0, got {res_600['total_estimated_bill']}"
    print("  [OK] Calculator verified successfully.")

    # 2. Test Get baseline before connection
    print("[2] Testing Baseline retrieval before connection...")
    resp = client.get("/meter/baseline/current")
    assert resp.status_code == 200
    assert resp.json() is None, "Baseline should initially be null"
    print("  [OK] Baseline is correctly null.")

    # 3. Test Set baseline (Connect Bill)
    print("[3] Testing Baseline setup (Connect TNEB Bill)...")
    baseline_payload = {
        "cycle_id": "BILL-20260901",
        "baseline_date": "2026-09-01",
        "baseline_reading": 12450.0,
        "previous_billed_units": 302.0,
        "tariff_code": "LT-1A",
        "service_connection_number": "09234567890"
    }
    resp = client.post("/meter/baseline", json=baseline_payload)
    assert resp.status_code == 200, resp.text
    base_data = resp.json()
    assert base_data["baseline_reading"] == 12450.0
    assert base_data["cycle_id"] == "BILL-20260901"
    assert base_data["is_active"] is True
    print(f"  [OK] Baseline set: {base_data['baseline_reading']} kWh on {base_data['baseline_date']}")

    # 4. Test Current Consumption before first reading
    print("[4] Testing Consumption before first reading...")
    resp = client.get("/meter/consumption/current")
    assert resp.status_code == 200
    cons = resp.json()
    assert cons["baseline_reading"] == 12450.0
    assert cons["latest_reading"] is None
    assert cons["units_consumed"] == 0.0
    assert cons["is_today_logged"] is False
    print("  [OK] Prior to first reading, units_consumed is 0.0.")

    # 5. Test Invalid Readings (negative, lower than baseline)
    print("[5] Testing Validation of invalid readings...")
    # 5a. Negative reading
    resp = client.post("/meter/readings", json={"reading_value": -5.0})
    assert resp.status_code in [400, 422], f"Expected rejection, got {resp.status_code}"
    # 5b. Lower than baseline
    resp = client.post("/meter/readings", json={"reading_value": 12400.0})
    assert resp.status_code == 400
    assert "cannot be lower than previous bill baseline" in resp.json()["detail"]
    print("  [OK] Negative and lower-than-baseline readings properly rejected.")

    # 6. Test Valid First Reading
    print("[6] Testing Valid First Reading (12,582 kWh)...")
    today_iso = date.today().isoformat()
    resp = client.post("/meter/readings", json={
        "reading_value": 12582.0,
        "reading_date": today_iso,
        "notes": "First reading"
    })
    assert resp.status_code == 201, resp.text
    reading_res = resp.json()
    assert reading_res["reading_value"] == 12582.0
    assert reading_res["daily_consumption"] == 132.0  # 12582 - 12450
    print("  [OK] First reading saved successfully. Daily consumption = 132 kWh.")

    # 7. Test Duplicate Reading on same day
    print("[7] Testing Duplicate same-day reading rejection...")
    resp = client.post("/meter/readings", json={
        "reading_value": 12585.0,
        "reading_date": today_iso
    })
    assert resp.status_code == 409
    assert "already exists" in resp.json()["detail"]
    print("  [OK] Duplicate reading on same day correctly rejected.")

    # 7b. Test Future Date and Pre-Baseline Date Rejection
    print("[7b] Testing Future and Pre-Baseline Date Rejections...")
    future_date = (date.today() + timedelta(days=2)).isoformat()
    resp_future = client.post("/meter/readings", json={
        "reading_value": 12600.0,
        "reading_date": future_date
    })
    assert resp_future.status_code == 400, "Future reading date must be rejected"

    resp_past = client.post("/meter/readings", json={
        "reading_value": 12460.0,
        "reading_date": "2026-08-15"
    })
    assert resp_past.status_code == 400, "Pre-baseline reading date must be rejected"
    print("  [OK] Invalid reading dates properly rejected.")

    # 8. Test Edit / Update Reading by Date
    print("[8] Testing Update Reading by Date...")
    resp = client.put(f"/meter/readings/{today_iso}", json={
        "reading_value": 12584.0,
        "notes": "Corrected today reading"
    })
    assert resp.status_code == 200
    assert resp.json()["reading_value"] == 12584.0
    assert resp.json()["daily_consumption"] == 134.0  # 12584 - 12450
    print("  [OK] Reading updated by date successfully to 12,584 kWh.")

    # 9. Test Consumption & Estimated Bill
    print("[9] Testing Current Consumption metrics...")
    resp = client.get("/meter/consumption/current")
    assert resp.status_code == 200
    c_data = resp.json()
    assert c_data["latest_reading"] == 12584.0
    assert c_data["units_consumed"] == 134.0
    assert c_data["is_today_logged"] is True
    assert c_data["today_reading"] == 12584.0
    assert c_data["avg_units_per_day"] > 0
    print(f"  [OK] Units consumed: {c_data['units_consumed']} kWh, Avg: {c_data['avg_units_per_day']} kWh/day, Est Bill: Rs.{c_data['estimated_bill']}")

    # 10. Test Graph Data
    print("[10] Testing Graph Data...")
    resp = client.get("/meter/graph")
    assert resp.status_code == 200
    graph = resp.json()
    assert graph["total_points"] == 2  # Baseline point + Today's reading point
    assert graph["points"][0]["is_baseline"] is True
    assert graph["points"][0]["reading_value"] == 12450.0
    assert graph["points"][1]["reading_value"] == 12584.0
    print(f"  [OK] Graph returns {graph['total_points']} points anchored at baseline.")

    # 11. Test New Billing Cycle Rollover
    print("[11] Testing New Billing Cycle rollover...")
    new_baseline = {
        "cycle_id": "BILL-20261101",
        "baseline_date": "2026-11-01",
        "baseline_reading": 12900.0,
        "previous_billed_units": 450.0,
        "tariff_code": "LT-1A"
    }
    resp = client.post("/meter/baseline", json=new_baseline)
    assert resp.status_code == 200
    # Current baseline should now be the new one
    resp = client.get("/meter/baseline/current")
    assert resp.json()["cycle_id"] == "BILL-20261101"
    assert resp.json()["baseline_reading"] == 12900.0

    # Current consumption in new cycle is reset to 0 before new readings
    resp = client.get("/meter/consumption/current")
    assert resp.json()["units_consumed"] == 0.0
    assert resp.json()["latest_reading"] is None
    print("  [OK] New billing cycle cleanly reset consumption tracking.")

    print("\nALL BACKEND TESTS PASSED SUCCESSFULLY! [OK]\n")

if __name__ == "__main__":
    run_all_tests()
