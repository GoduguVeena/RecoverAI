import argparse
import math
import numpy as np
import pandas as pd
from pathlib import Path
import sys

# Ensure training package is importable
sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from training.config import (
    DATASET_PATH,
    DATA_DIR,
    RANDOM_SEED,
    DEFAULT_ROW_COUNT,
    TARGET_COLUMN,
    FORBIDDEN_LEAKAGE_COLUMNS
)

def generate_synthetic_dataset(num_rows: int = 10000, seed: int = 42) -> pd.DataFrame:
    """
    Generates a realistic, reproducible synthetic dataset for payment recovery probability prediction.
    Target: recovered_within_window (0 or 1).
    """
    np.random.seed(seed)

    # Categories
    merchants = [f"merch_{i}" for i in range(1, 21)]
    merchant_categories = ["ecommerce", "saas", "education", "gaming", "utility"]
    customers = [f"cust_{i}" for i in range(1, 1001)]
    customer_segments = ["vip", "regular", "new", "at_risk"]
    payment_methods = ["upi", "card", "netbanking", "wallet", "emi"]
    failure_types = [
        "TRANSIENT_NETWORK_TIMEOUT",
        "INSUFFICIENT_FUNDS",
        "AUTHENTICATION_FAILED",
        "CARD_EXPIRED",
        "INVALID_ACCOUNT",
        "RISK_REJECTED"
    ]

    # Generate core columns
    merchant_ids = np.random.choice(merchants, size=num_rows)
    customer_ids = np.random.choice(customers, size=num_rows)
    merchant_cats = np.random.choice(merchant_categories, size=num_rows)
    cust_segs = np.random.choice(customer_segments, size=num_rows, p=[0.15, 0.50, 0.25, 0.10])
    methods = np.random.choice(payment_methods, size=num_rows, p=[0.45, 0.30, 0.12, 0.08, 0.05])
    failures = np.random.choice(failure_types, size=num_rows, p=[0.35, 0.30, 0.15, 0.10, 0.06, 0.04])
    currencies = ["INR"] * num_rows

    # Numerical features
    amounts = np.round(np.random.exponential(scale=3500.0, size=num_rows) + 50.0, 2)
    retry_counts = np.random.choice([0, 1, 2, 3], size=num_rows, p=[0.55, 0.25, 0.12, 0.08])

    total_txns = np.random.randint(1, 51, size=num_rows)
    success_rates = np.random.beta(a=5, b=2, size=num_rows) # Skewed towards higher success
    success_txns = np.round(total_txns * success_rates).astype(int)
    failed_txns = total_txns - success_txns
    total_spends = np.round(success_txns * np.random.uniform(500.0, 5000.0, size=num_rows), 2)

    days_since_success = np.round(np.random.exponential(scale=14.0, size=num_rows), 1)
    checkout_durations = np.round(np.random.gamma(shape=3.0, scale=15.0, size=num_rows) + 5.0, 1)
    hours = np.random.randint(0, 24, size=num_rows)
    days_of_week = np.random.randint(0, 7, size=num_rows)

    # Probabilistic Target Calculation (Log-odds sigmoid sampler)
    # z = base + failure_weight + method_weight + segment_weight + success_rate_weight + retry_penalty + amount_penalty
    base_log_odds = -0.3

    failure_weights = {
        "TRANSIENT_NETWORK_TIMEOUT": 1.8,
        "INSUFFICIENT_FUNDS": 0.8,
        "AUTHENTICATION_FAILED": 0.2,
        "CARD_EXPIRED": -1.5,
        "INVALID_ACCOUNT": -3.2,
        "RISK_REJECTED": -4.0
    }

    method_weights = {
        "upi": 0.5,
        "card": 0.3,
        "wallet": 0.2,
        "netbanking": 0.0,
        "emi": -0.4
    }

    segment_weights = {
        "vip": 0.6,
        "regular": 0.2,
        "new": -0.1,
        "at_risk": -0.7
    }

    z = np.full(num_rows, base_log_odds)

    for i in range(num_rows):
        z[i] += failure_weights[failures[i]]
        z[i] += method_weights[methods[i]]
        z[i] += segment_weights[cust_segs[i]]
        z[i] += 1.8 * (success_rates[i] - 0.5)
        z[i] -= 0.6 * retry_counts[i]
        z[i] -= 0.00003 * amounts[i]

    # Add Gaussian stochastic noise to prevent deterministic leakage
    noise = np.random.normal(loc=0.0, scale=0.5, size=num_rows)
    z_noisy = z + noise

    # Sigmoid function
    probabilities = 1.0 / (1.0 + np.exp(-z_noisy))

    # Bernoulli trial sample
    targets = (np.random.uniform(0.0, 1.0, size=num_rows) < probabilities).astype(int)

    df = pd.DataFrame({
        "merchant_id": merchant_ids,
        "customer_id": customer_ids,
        "amount": amounts,
        "currency": currencies,
        "payment_method": methods,
        "failure_type": failures,
        "retry_count": retry_counts,
        "customer_total_transactions": total_txns,
        "customer_successful_transactions": success_txns,
        "customer_failed_transactions": failed_txns,
        "customer_success_rate": np.round(success_rates, 4),
        "customer_total_spend": total_spends,
        "days_since_last_success": days_since_success,
        "checkout_duration_seconds": checkout_durations,
        "hour_of_day": hours,
        "day_of_week": days_of_week,
        "merchant_category": merchant_cats,
        "customer_segment": cust_segs,
        TARGET_COLUMN: targets
    })

    # Assert no forbidden data leakage features exist
    for forbidden_col in FORBIDDEN_LEAKAGE_COLUMNS:
        assert forbidden_col not in df.columns, f"Data leakage error: {forbidden_col} found in dataset!"

    return df

def main():
    parser = argparse.ArgumentParser(description="Generate synthetic payment recovery dataset.")
    parser.add_argument("--rows", type=int, default=DEFAULT_ROW_COUNT, help="Number of rows to generate")
    parser.add_argument("--seed", type=int, default=RANDOM_SEED, help="Random seed for reproducibility")
    args = parser.parse_args()

    print(f"Generating synthetic dataset with {args.rows} rows (seed: {args.seed})...")
    df = generate_synthetic_dataset(num_rows=args.rows, seed=args.seed)

    DATA_DIR.mkdir(parents=True, exist_ok=True)
    df.to_csv(DATASET_PATH, index=False)

    total_records = len(df)
    recovered_records = int(df[TARGET_COLUMN].sum())
    non_recovered_records = total_records - recovered_records
    recovery_pct = (recovered_records / total_records) * 100

    print(f"Dataset successfully created at: {DATASET_PATH}")
    print(f"Total Records: {total_records}")
    print(f"Recovered Records (1): {recovered_records} ({recovery_pct:.2f}%)")
    print(f"Non-Recovered Records (0): {non_recovered_records} ({100 - recovery_pct:.2f}%)")

if __name__ == "__main__":
    main()
