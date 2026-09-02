import os
from pathlib import Path

# Base Paths
ML_SERVICE_DIR = Path(__file__).resolve().parent.parent
ROOT_DIR = ML_SERVICE_DIR.parent.parent

DATA_DIR = ROOT_DIR / "data" / "synthetic"
DATASET_PATH = DATA_DIR / "recovery_dataset.csv"

MODELS_DIR = ML_SERVICE_DIR / "models"
EXPERIMENTS_DIR = ML_SERVICE_DIR / "experiments"

# Experiment Configuration
RANDOM_SEED = 42
DEFAULT_ROW_COUNT = 10000
RECOVERY_WINDOW_HOURS = 72

# Feature Definitions
CATEGORICAL_FEATURES = [
    "merchant_id",
    "customer_id",
    "currency",
    "payment_method",
    "failure_type",
    "merchant_category",
    "customer_segment"
]

NUMERICAL_FEATURES = [
    "amount",
    "retry_count",
    "customer_total_transactions",
    "customer_successful_transactions",
    "customer_failed_transactions",
    "customer_success_rate",
    "customer_total_spend",
    "days_since_last_success",
    "checkout_duration_seconds",
    "hour_of_day",
    "day_of_week"
]

ALL_FEATURES = CATEGORICAL_FEATURES + NUMERICAL_FEATURES
TARGET_COLUMN = "recovered_within_window"

# Data Leakage Protection Checklist (Forbidden Columns)
FORBIDDEN_LEAKAGE_COLUMNS = [
    "recovery_attempt_success",
    "recovered_amount",
    "final_payment_status",
    "recovery_action_taken",
    "recovery_timestamp"
]
