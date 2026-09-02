import pytest
import json
from training.evaluate import evaluate_models
from training.config import EXPERIMENTS_DIR

def test_evaluate_models_artifacts():
    evaluate_models()

    assert (EXPERIMENTS_DIR / "metrics.json").exists()
    assert (EXPERIMENTS_DIR / "model_comparison.json").exists()
    assert (EXPERIMENTS_DIR / "feature_importance.json").exists()

    with open(EXPERIMENTS_DIR / "metrics.json") as f:
        metrics = json.load(f)

    assert "precision" in metrics
    assert "recall" in metrics
    assert "f1" in metrics
    assert "roc_auc" in metrics
    assert "confusion_matrix" in metrics
