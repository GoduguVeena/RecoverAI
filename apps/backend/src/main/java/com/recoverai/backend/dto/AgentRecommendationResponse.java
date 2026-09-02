package com.recoverai.backend.dto;

import com.recoverai.backend.domain.enums.RecoveryActionType;

import java.math.BigDecimal;
import java.util.List;

public class AgentRecommendationResponse {

    private String modelVersion;
    private BigDecimal recoveryProbability;
    private String diagnosis;
    private List<RecoveryActionType> candidateActions;
    private RecoveryActionType selectedAction;
    private String reasoningSummary;

    public AgentRecommendationResponse() {
    }

    public AgentRecommendationResponse(String modelVersion,
                                       BigDecimal recoveryProbability,
                                       String diagnosis,
                                       List<RecoveryActionType> candidateActions,
                                       RecoveryActionType selectedAction,
                                       String reasoningSummary) {
        this.modelVersion = modelVersion;
        this.recoveryProbability = recoveryProbability;
        this.diagnosis = diagnosis;
        this.candidateActions = candidateActions;
        this.selectedAction = selectedAction;
        this.reasoningSummary = reasoningSummary;
    }

    public String getModelVersion() { return modelVersion; }
    public void setModelVersion(String modelVersion) { this.modelVersion = modelVersion; }

    public BigDecimal getRecoveryProbability() { return recoveryProbability; }
    public void setRecoveryProbability(BigDecimal recoveryProbability) { this.recoveryProbability = recoveryProbability; }

    public String getDiagnosis() { return diagnosis; }
    public void setDiagnosis(String diagnosis) { this.diagnosis = diagnosis; }

    public List<RecoveryActionType> getCandidateActions() { return candidateActions; }
    public void setCandidateActions(List<RecoveryActionType> candidateActions) { this.candidateActions = candidateActions; }

    public RecoveryActionType getSelectedAction() { return selectedAction; }
    public void setSelectedAction(RecoveryActionType selectedAction) { this.selectedAction = selectedAction; }

    public String getReasoningSummary() { return reasoningSummary; }
    public void setReasoningSummary(String reasoningSummary) { this.reasoningSummary = reasoningSummary; }
}
