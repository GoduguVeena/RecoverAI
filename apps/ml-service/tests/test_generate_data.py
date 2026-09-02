import pytest
import pandas as pd
from training.generate_data import generate_synthetic_dataset
from training.config import (
    ALL_FEATURES,
    TARGET_COLUMN,
    FORBIDDEN_LEAKAGE_COLUMNS
)

def test_generate_synthetic_dataset_row_count():
    df = generate_synthetic_dataset(num_rows=500, seed=123)
    assert len(df) == 500

def test_generate_synthetic_dataset_seed_determinism():
    df1 = generate_synthetic_dataset(num_rows=200, seed=42)
    df2 = generate_synthetic_dataset(num_rows=200, seed=42)
    pd.testing.assert_frame_equal(df1, df2)

def test_generate_synthetic_dataset_required_columns():
    df = generate_synthetic_dataset(num_rows=100, seed=42)
    for col in ALL_FEATURES + [TARGET_COLUMN]:
        assert col in df.columns, f"Missing required column: {col}"

def test_generate_synthetic_dataset_no_leakage_columns():
    df = generate_synthetic_dataset(num_rows=100, seed=42)
    for forbidden_col in FORBIDDEN_LEAKAGE_COLUMNS:
        assert forbidden_col not in df.columns, f"Forbidden leakage column found: {forbidden_col}"

def test_generate_synthetic_dataset_binary_target():
    df = generate_synthetic_dataset(num_rows=500, seed=42)
    unique_targets = set(df[TARGET_COLUMN].unique())
    assert unique_targets.issubset({0, 1})
    assert len(unique_targets) == 2, "Target must contain both 0 and 1 classes"
