import pytest
import pandas as pd
from training.generate_data import generate_synthetic_dataset
from training.preprocess import get_train_val_test_splits, build_preprocessor

def test_train_val_test_splits():
    df = generate_synthetic_dataset(num_rows=1000, seed=42)
    X_train, y_train, X_val, y_val, X_test, y_test = get_train_val_test_splits(df, seed=42)

    total_rows = len(df)
    assert len(X_train) == int(total_rows * 0.70)
    assert len(X_val) == int(total_rows * 0.15)
    assert len(X_test) == int(total_rows * 0.15)

def test_preprocessor_fitting():
    df = generate_synthetic_dataset(num_rows=200, seed=42)
    X_train, y_train, X_val, y_val, X_test, y_test = get_train_val_test_splits(df, seed=42)

    preprocessor = build_preprocessor()
    X_train_proc = preprocessor.fit_transform(X_train)
    X_test_proc = preprocessor.transform(X_test)

    assert X_train_proc.shape[0] == len(X_train)
    assert X_test_proc.shape[0] == len(X_test)
    assert not pd.isna(X_train_proc).any()
