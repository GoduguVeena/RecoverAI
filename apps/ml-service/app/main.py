from contextlib import asynccontextmanager
from fastapi import FastAPI, HTTPException, status
from fastapi.middleware.cors import CORSMiddleware
from app.schemas import PredictionRequest, PredictionResponse, ErrorResponse, MODEL_VERSION
from app.model_loader import model_manager

@asynccontextmanager
async def lifespan(app: FastAPI):
    # Startup: Load trained model artifact
    model_manager.load_model()
    yield

app = FastAPI(
    title="RecoverAI ML Service",
    description="Machine Learning service predicting payment recovery probability for at-risk transactions.",
    version="1.0.0",
    lifespan=lifespan
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

@app.get("/health", summary="Health Check", description="Returns health and model readiness status.")
def health_check():
    return {
        "status": "UP",
        "service": "recoverai-ml",
        "model_loaded": model_manager.is_loaded
    }

@app.post(
    "/predict",
    response_model=PredictionResponse,
    status_code=status.HTTP_200_OK,
    summary="Predict Recovery Probability",
    description="Predicts the probability of successful payment recovery given transaction failure context.",
    responses={
        422: {"model": ErrorResponse, "description": "Validation error or forbidden data leakage field"},
        503: {"model": ErrorResponse, "description": "Model model artifact unavailable"},
        500: {"model": ErrorResponse, "description": "Internal prediction execution error"}
    }
)
def predict_recovery(request: PredictionRequest):
    if not model_manager.is_loaded:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="Model artifact unavailable. Service cannot serve predictions."
        )

    try:
        probability, label = model_manager.predict(request)
        return PredictionResponse(
            model_version=MODEL_VERSION,
            recovery_probability=probability,
            prediction=label
        )
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Prediction failed: {str(e)}"
        )
