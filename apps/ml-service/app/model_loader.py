import joblib
import pandas as pd
from pathlib import Path
from typing import Optional, Tuple
from app.schemas import PredictionRequest, MODEL_VERSION, DEFAULT_THRESHOLD

# Path to trained model artifact
ML_SERVICE_DIR = Path(__file__).resolve().parent.parent
MODEL_PATH = ML_SERVICE_DIR / "models" / "recovery_model.joblib"

class ModelManager:
    def __init__(self, model_path: Path = MODEL_PATH):
        self.model_path = model_path
        self.model = None
        self.is_loaded = False

    def load_model(self) -> bool:
        if self.model_path.exists():
            try:
                self.model = joblib.load(self.model_path)
                self.is_loaded = True
                return True
            except Exception as e:
                print(f"Error loading model from {self.model_path}: {e}")
                self.model = None
                self.is_loaded = False
                return False
        else:
            print(f"Model file not found at {self.model_path}")
            self.model = None
            self.is_loaded = False
            return False

    def predict(self, request: PredictionRequest) -> Tuple[float, str]:
        if not self.is_loaded or self.model is None:
            raise RuntimeError("Model is not loaded")

        # Convert Pydantic request to pandas DataFrame with exact column names
        data_dict = request.model_dump()
        df = pd.DataFrame([data_dict])

        # Predict probability of recovery (class 1)
        probs = self.model.predict_proba(df)
        recovery_prob = float(probs[0][1])
        recovery_prob_rounded = round(recovery_prob, 4)

        prediction_label = "RECOVERABLE" if recovery_prob_rounded >= DEFAULT_THRESHOLD else "UNRECOVERABLE"
        return recovery_prob_rounded, prediction_label

# Global singleton instance
model_manager = ModelManager()
