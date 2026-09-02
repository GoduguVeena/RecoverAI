import pandas as pd
import numpy as np
from sklearn.model_selection import train_test_split
from sklearn.compose import ColumnTransformer
from sklearn.preprocessing import OneHotEncoder, StandardScaler
from pathlib import Path
import sys

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from training.config import (
    DATASET_PATH,
    CATEGORICAL_FEATURES,
    NUMERICAL_FEATURES,
    ALL_FEATURES,
    TARGET_COLUMN,
    RANDOM_SEED,
    FORBIDDEN_LEAKAGE_COLUMNS
)

def load_and_validate_data(dataset_path: Path = DATASET_PATH) -> pd.DataFrame:
    if not dataset_path.exists():
        raise FileNotFoundError(f"Dataset not found at {dataset_path}. Run generate_data.py first.")
    
    df = pd.read_csv(dataset_path)

    # Check data leakage rule
    for col in FORBIDDEN_LEAKAGE_COLUMNS:
        if col in df.columns:
            raise ValueError(f"DATA LEAKAGE DETECTED: Column '{col}' is forbidden!")

    # Check required columns
    for col in ALL_FEATURES + [TARGET_COLUMN]:
        if col not in df.columns:
            raise ValueError(f"Missing required column '{col}' in dataset.")

    return df

def build_preprocessor():
    """
    Creates a scikit-learn ColumnTransformer for categorical and numerical features.
    """
    preprocessor = ColumnTransformer(
        transformers=[
            ("cat", OneHotEncoder(handle_unknown="ignore", sparse_output=False), CATEGORICAL_FEATURES),
            ("num", StandardScaler(), NUMERICAL_FEATURES)
        ]
    )
    return preprocessor

def get_train_val_test_splits(df: pd.DataFrame, seed: int = RANDOM_SEED):
    """
    Splits dataset into 70% Train, 15% Validation, and 15% Test with stratification.
    """
    X = df[ALL_FEATURES]
    y = df[TARGET_COLUMN]

    # First split: 70% Train, 30% Temp (Val + Test)
    X_train, X_temp, y_train, y_temp = train_test_split(
        X, y, test_size=0.30, random_state=seed, stratify=y
    )

    # Second split: 15% Val, 15% Test
    X_val, X_test, y_val, y_test = train_test_split(
        X_temp, y_temp, test_size=0.50, random_state=seed, stratify=y_temp
    )

    return X_train, y_train, X_val, y_val, X_test, y_test

def preprocess_pipeline(dataset_path: Path = DATASET_PATH, seed: int = RANDOM_SEED):
    df = load_and_validate_data(dataset_path)
    X_train, y_train, X_val, y_val, X_test, y_test = get_train_val_test_splits(df, seed=seed)

    preprocessor = build_preprocessor()

    # Fit preprocessor ONLY on training set to prevent data leakage
    X_train_proc = preprocessor.fit_transform(X_train)
    X_val_proc = preprocessor.transform(X_val)
    X_test_proc = preprocessor.transform(X_test)

    return {
        "preprocessor": preprocessor,
        "X_train_raw": X_train,
        "y_train": y_train,
        "X_val_raw": X_val,
        "y_val": y_val,
        "X_test_raw": X_test,
        "y_test": y_test,
        "X_train_proc": X_train_proc,
        "X_val_proc": X_val_proc,
        "X_test_proc": X_test_proc
    }
