import json
import joblib
import numpy as np
import pandas as pd
from pathlib import Path
import sys
from sklearn.metrics import (
    accuracy_score,
    precision_score,
    recall_score,
    f1_score,
    roc_auc_score,
    confusion_matrix
)

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from training.config import (
    EXPERIMENTS_DIR,
    RANDOM_SEED
)
from training.train import train_models

def evaluate_models():
    train_results = train_models()
    
    lr_model = train_results["lr_model"]
    rf_model = train_results["rf_model"]
    data_dict = train_results["data_dict"]
    selected_name = train_results["selected_model_name"]

    X_test_proc = data_dict["X_test_proc"]
    y_test = data_dict["y_test"]
    preprocessor = data_dict["preprocessor"]

    # Get feature names after OneHotEncoding
    cat_encoder = preprocessor.named_transformers_["cat"]
    cat_feature_names = cat_encoder.get_feature_names_out(data_dict["preprocessor"].transformers[0][2]).tolist()
    num_feature_names = list(data_dict["preprocessor"].transformers[1][2])
    all_transformed_feature_names = cat_feature_names + num_feature_names

    # Helper function to compute metrics
    def compute_metrics(model, X_test, y_test):
        probs = model.predict_proba(X_test)[:, 1]
        preds = (probs >= 0.5).astype(int)
        cm = confusion_matrix(y_test, preds).tolist()
        return {
            "accuracy": round(float(accuracy_score(y_test, preds)), 4),
            "precision": round(float(precision_score(y_test, preds)), 4),
            "recall": round(float(recall_score(y_test, preds)), 4),
            "f1": round(float(f1_score(y_test, preds)), 4),
            "roc_auc": round(float(roc_auc_score(y_test, probs)), 4),
            "confusion_matrix": {
                "true_negative": cm[0][0],
                "false_positive": cm[0][1],
                "false_negative": cm[1][0],
                "true_positive": cm[1][1]
            }
        }

    print("Evaluating models on held-out test set (15% split)...")
    lr_metrics = compute_metrics(lr_model, X_test_proc, y_test)
    rf_metrics = compute_metrics(rf_model, X_test_proc, y_test)

    print("=== TEST SET RESULTS ===")
    print(f"Logistic Regression -> Precision: {lr_metrics['precision']}, Recall: {lr_metrics['recall']}, F1: {lr_metrics['f1']}, ROC-AUC: {lr_metrics['roc_auc']}")
    print(f"Random Forest       -> Precision: {rf_metrics['precision']}, Recall: {rf_metrics['recall']}, F1: {rf_metrics['f1']}, ROC-AUC: {rf_metrics['roc_auc']}")

    # Model comparison JSON
    model_comparison = {
        "LogisticRegression": lr_metrics,
        "RandomForestClassifier": rf_metrics,
        "selected_model": selected_name
    }

    # Extract Feature Importances
    # 1. Logistic Regression Coefficients
    lr_coefs = np.abs(lr_model.coef_[0])
    lr_importance = dict(zip(all_transformed_feature_names, [round(float(c), 4) for c in lr_coefs]))
    sorted_lr_importance = dict(sorted(lr_importance.items(), key=lambda x: x[1], reverse=True)[:15])

    # 2. Random Forest Feature Importances
    rf_importances = rf_model.feature_importances_
    rf_importance = dict(zip(all_transformed_feature_names, [round(float(imp), 4) for imp in rf_importances]))
    sorted_rf_importance = dict(sorted(rf_importance.items(), key=lambda x: x[1], reverse=True)[:15])

    feature_importance_data = {
        "LogisticRegression_top15": sorted_lr_importance,
        "RandomForestClassifier_top15": sorted_rf_importance
    }

    # Metrics JSON for selected model
    selected_metrics = lr_metrics if selected_name == "LogisticRegression" else rf_metrics

    EXPERIMENTS_DIR.mkdir(parents=True, exist_ok=True)

    with open(EXPERIMENTS_DIR / "metrics.json", "w") as f:
        json.dump(selected_metrics, f, indent=2)

    with open(EXPERIMENTS_DIR / "model_comparison.json", "w") as f:
        json.dump(model_comparison, f, indent=2)

    with open(EXPERIMENTS_DIR / "feature_importance.json", "w") as f:
        json.dump(feature_importance_data, f, indent=2)

    print(f"Saved evaluation artifacts to {EXPERIMENTS_DIR}")

if __name__ == "__main__":
    evaluate_models()
