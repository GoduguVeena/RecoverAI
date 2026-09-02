import pytest
from fastapi.testclient import TestClient
from app.main import app
from app.model_loader import model_manager

@pytest.fixture
def valid_payload():
    return {
        "merchant_id": "merch_1",
        "customer_id": "cust_10",
        "amount": 2499.00,
        "currency": "INR",
        "payment_method": "upi",
        "failure_type": "TRANSIENT_NETWORK_TIMEOUT",
        "retry_count": 1,
        "customer_total_transactions": 10,
        "customer_successful_transactions": 8,
        "customer_failed_transactions": 2,
        "customer_success_rate": 0.8,
        "customer_total_spend": 15000.00,
        "days_since_last_success": 3.5,
        "checkout_duration_seconds": 45.0,
        "hour_of_day": 14,
        "day_of_week": 2,
        "merchant_category": "ecommerce",
        "customer_segment": "regular"
    }

def test_valid_prediction(valid_payload):
    with TestClient(app) as client:
        response = client.post("/predict", json=valid_payload)
        assert response.status_code == 200
        data = response.json()

        assert data["model_version"] == "recovery-logistic-v1"
        assert "recovery_probability" in data
        assert 0.0 <= data["recovery_probability"] <= 1.0
        assert data["prediction"] in ["RECOVERABLE", "UNRECOVERABLE"]

def test_prediction_determinism(valid_payload):
    with TestClient(app) as client:
        probabilities = []
        for _ in range(5):
            response = client.post("/predict", json=valid_payload)
            assert response.status_code == 200
            probabilities.append(response.json()["recovery_probability"])

        # All 5 predictions must be identical
        assert len(set(probabilities)) == 1

def test_invalid_input_missing_field(valid_payload):
    payload = valid_payload.copy()
    del payload["failure_type"] # Missing required field

    with TestClient(app) as client:
        response = client.post("/predict", json=payload)
        assert response.status_code == 422

def test_invalid_input_negative_amount(valid_payload):
    payload = valid_payload.copy()
    payload["amount"] = -100.00

    with TestClient(app) as client:
        response = client.post("/predict", json=payload)
        assert response.status_code == 422

def test_invalid_input_out_of_bounds_success_rate(valid_payload):
    payload = valid_payload.copy()
    payload["customer_success_rate"] = 1.5 # Must be <= 1.0

    with TestClient(app) as client:
        response = client.post("/predict", json=payload)
        assert response.status_code == 422

def test_invalid_input_forbidden_leakage_field(valid_payload):
    payload = valid_payload.copy()
    payload["recovery_attempt_success"] = 1 # Data leakage column forbidden

    with TestClient(app) as client:
        response = client.post("/predict", json=payload)
        assert response.status_code == 422

def test_boundary_values(valid_payload):
    payload = valid_payload.copy()
    payload["amount"] = 0.01
    payload["retry_count"] = 0
    payload["customer_success_rate"] = 0.0
    payload["hour_of_day"] = 0
    payload["day_of_week"] = 6

    with TestClient(app) as client:
        response = client.post("/predict", json=payload)
        assert response.status_code == 200
        assert 0.0 <= response.json()["recovery_probability"] <= 1.0

def test_model_unavailability(valid_payload):
    with TestClient(app) as client:
        original_state = model_manager.is_loaded
        model_manager.is_loaded = False
        try:
            response = client.post("/predict", json=valid_payload)
            assert response.status_code == 503
            assert response.json()["detail"] == "Model artifact unavailable. Service cannot serve predictions."
        finally:
            model_manager.is_loaded = original_state
