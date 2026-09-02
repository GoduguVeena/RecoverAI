import joblib
import numpy as np
import pandas as pd
from pathlib import Path
import sys
from sklearn.linear_model import LogisticRegression
from sklearn.ensemble import RandomForestClassifier
from sklearn.pipeline import Pipeline
from sklearn.metrics import roc_auc_score, f1_score

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from training.config import (
    MODELS_DIR,
    RANDOM_SEED
)
from training.preprocess import preprocess_pipeline

def train_models(dataset_path: Path = None):
    print("Preprocessing data and splitting (70% train, 15% val, 15% test)...")
    data_dict = preprocess_pipeline(seed=RANDOM_SEED)

    X_train_proc = data_dict["X_train_proc"]
    y_train = data_dict["y_train"]
    X_val_proc = data_dict["X_val_proc"]
    y_val = data_dict["y_val"]
    preprocessor = data_dict["preprocessor"]

    print("1. Training Logistic Regression baseline model...")
    lr_model = LogisticRegression(random_state=RANDOM_SEED, max_iter=1000)
    lr_model.fit(X_train_proc, y_train)

    lr_val_probs = lr_model.predict_proba(X_val_proc)[:, 1]
    lr_val_preds = (lr_val_probs >= 0.5).astype(int)
    lr_auc = roc_auc_score(y_val, lr_val_probs)
    lr_f1 = f1_score(y_val, lr_val_preds)
    print(f"   Logistic Regression Validation -> ROC-AUC: {lr_auc:.4f}, F1: {lr_f1:.4f}")

    print("2. Training Random Forest candidate model...")
    rf_model = RandomForestClassifier(n_estimators=100, max_depth=10, random_state=RANDOM_SEED)
    rf_model.fit(X_train_proc, y_train)

    rf_val_probs = rf_model.predict_proba(X_val_proc)[:, 1]
    rf_val_preds = (rf_val_probs >= 0.5).astype(int)
    rf_auc = roc_auc_score(y_val, rf_val_probs)
    rf_f1 = f1_score(y_val, rf_val_preds)
    print(f"   Random Forest Validation       -> ROC-AUC: {rf_auc:.4f}, F1: {rf_f1:.4f}")

    # Model Selection Logic based on validation ROC-AUC
    if rf_auc > lr_auc:
        selected_model_name = "RandomForestClassifier"
        selected_classifier = rf_model
        print(f"Selected Candidate Model: {selected_model_name} (ROC-AUC {rf_auc:.4f} > {lr_auc:.4f})")
    else:
        selected_model_name = "LogisticRegression"
        selected_classifier = lr_model
        print(f"Selected Baseline Model: {selected_model_name} (ROC-AUC {lr_auc:.4f} >= {rf_auc:.4f})")

    # Combine preprocessor + classifier into a scikit-learn Pipeline
    full_pipeline = Pipeline([
        ("preprocessor", preprocessor),
        ("classifier", selected_classifier)
    ])

    MODELS_DIR.mkdir(parents=True, exist_ok=True)
    model_path = MODELS_DIR / "recovery_model.joblib"
    joblib.dump(full_pipeline, model_path)
    print(f"Saved inference pipeline to {model_path}")

    return {
        "lr_model": lr_model,
        "rf_model": rf_model,
        "selected_model_name": selected_model_name,
        "selected_pipeline": full_pipeline,
        "data_dict": data_dict
    }

if __name__ == "__main__":
    train_models()
