from pydantic import BaseModel, Field, ConfigDict
from typing import Literal

MODEL_VERSION = "recovery-logistic-v1"
DEFAULT_THRESHOLD = 0.5

class PredictionRequest(BaseModel):
    merchant_id: str = Field(..., description="Unique merchant identifier", json_schema_extra={"example": "merch_1"})
    customer_id: str = Field(..., description="Unique customer identifier", json_schema_extra={"example": "cust_1"})
    amount: float = Field(..., gt=0, description="Transaction amount in INR", json_schema_extra={"example": 2499.00})
    currency: str = Field("INR", description="Currency code", json_schema_extra={"example": "INR"})
    payment_method: str = Field(..., description="Payment method used (e.g., upi, card, netbanking, wallet, emi)", json_schema_extra={"example": "upi"})
    failure_type: str = Field(..., description="Failure classification (e.g., TRANSIENT_NETWORK_TIMEOUT, INSUFFICIENT_FUNDS)", json_schema_extra={"example": "TRANSIENT_NETWORK_TIMEOUT"})
    retry_count: int = Field(..., ge=0, description="Number of retries attempted prior to decision", json_schema_extra={"example": 1})
    customer_total_transactions: int = Field(..., ge=0, description="Customer historical total transaction count", json_schema_extra={"example": 10})
    customer_successful_transactions: int = Field(..., ge=0, description="Customer historical successful transaction count", json_schema_extra={"example": 8})
    customer_failed_transactions: int = Field(..., ge=0, description="Customer historical failed transaction count", json_schema_extra={"example": 2})
    customer_success_rate: float = Field(..., ge=0.0, le=1.0, description="Customer historical success rate (0.0 to 1.0)", json_schema_extra={"example": 0.8})
    customer_total_spend: float = Field(..., ge=0.0, description="Customer total spend in INR", json_schema_extra={"example": 15000.00})
    days_since_last_success: float = Field(..., ge=0.0, description="Days elapsed since last successful payment", json_schema_extra={"example": 3.5})
    checkout_duration_seconds: float = Field(..., ge=0.0, description="Time spent in checkout in seconds", json_schema_extra={"example": 45.0})
    hour_of_day: int = Field(..., ge=0, le=23, description="Hour when failure occurred (0 to 23)", json_schema_extra={"example": 14})
    day_of_week: int = Field(..., ge=0, le=6, description="Day of week when failure occurred (0 to 6)", json_schema_extra={"example": 2})
    merchant_category: str = Field(..., description="Merchant business category", json_schema_extra={"example": "ecommerce"})
    customer_segment: str = Field(..., description="Customer segment (e.g., vip, regular, new, at_risk)", json_schema_extra={"example": "regular"})

    model_config = ConfigDict(
        extra="forbid" # Explicitly forbid data leakage or unexpected extra fields
    )

class PredictionResponse(BaseModel):
    model_version: str = Field(MODEL_VERSION, description="Version of the inference model used")
    recovery_probability: float = Field(..., ge=0.0, le=1.0, description="Predicted probability of recovery (0.0 to 1.0)")
    prediction: Literal["RECOVERABLE", "UNRECOVERABLE"] = Field(..., description="Binary recovery prediction label based on 0.5 threshold")

class ErrorResponse(BaseModel):
    detail: str = Field(..., description="Error message detail")
