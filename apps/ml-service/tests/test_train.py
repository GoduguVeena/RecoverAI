import pytest
import numpy as np
from training.generate_data import generate_synthetic_dataset
from training.train import train_models

def test_train_models_execution(tmp_path):
    dataset_path = tmp_path / "test_dataset.csv"
    df = generate_synthetic_dataset(num_rows=300, seed=42)
    df.to_csv(dataset_path, index=False)

    results = train_models(dataset_path=dataset_path)

    assert "selected_pipeline" in results
    pipeline = results["selected_pipeline"]
    
    # Test probability prediction output
    sample_raw = df.iloc[:5].drop(columns=["recovered_within_window"])
    probs = pipeline.predict_proba(sample_raw)[:, 1]

    assert len(probs) == 5
    assert (probs >= 0.0).all() and (probs <= 1.0).all()
