package com.recoverai.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public class MlPredictionResponse {

    @JsonProperty("model_version")
    private String modelVersion;

    @JsonProperty("recovery_probability")
    private BigDecimal recoveryProbability;

    private String prediction;

    public MlPredictionResponse() {
    }

    public MlPredictionResponse(String modelVersion, BigDecimal recoveryProbability, String prediction) {
        this.modelVersion = modelVersion;
        this.recoveryProbability = recoveryProbability;
        this.prediction = prediction;
    }

    public String getModelVersion() { return modelVersion; }
    public void setModelVersion(String modelVersion) { this.modelVersion = modelVersion; }

    public BigDecimal getRecoveryProbability() { return recoveryProbability; }
    public void setRecoveryProbability(BigDecimal recoveryProbability) { this.recoveryProbability = recoveryProbability; }

    public String getPrediction() { return prediction; }
    public void setPrediction(String prediction) { this.prediction = prediction; }
}
