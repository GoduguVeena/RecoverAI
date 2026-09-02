package com.recoverai.backend.dto;

import com.recoverai.backend.domain.entity.AgentDecision;
import com.recoverai.backend.domain.enums.RecoveryActionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class AgentDecisionResponse {

    private UUID id;
    private UUID recoveryCaseId;
    private String modelVersion;
    private BigDecimal modelProbability;
    private String diagnosis;
    private String candidateActions;
    private RecoveryActionType selectedAction;
    private String reasoningSummary;
    private String policyChecks;
    private Instant createdAt;

    public AgentDecisionResponse() {
    }

    public static AgentDecisionResponse from(AgentDecision decision) {
        AgentDecisionResponse response = new AgentDecisionResponse();
        response.setId(decision.getId());
        response.setRecoveryCaseId(decision.getRecoveryCase().getId());
        response.setModelVersion(decision.getModelVersion());
        response.setModelProbability(decision.getModelProbability());
        response.setDiagnosis(decision.getDiagnosis());
        response.setCandidateActions(decision.getCandidateActions());
        response.setSelectedAction(decision.getSelectedAction());
        response.setReasoningSummary(decision.getReasoningSummary());
        response.setPolicyChecks(decision.getPolicyChecks());
        response.setCreatedAt(decision.getCreatedAt());
        return response;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getRecoveryCaseId() { return recoveryCaseId; }
    public void setRecoveryCaseId(UUID recoveryCaseId) { this.recoveryCaseId = recoveryCaseId; }

    public String getModelVersion() { return modelVersion; }
    public void setModelVersion(String modelVersion) { this.modelVersion = modelVersion; }

    public BigDecimal getModelProbability() { return modelProbability; }
    public void setModelProbability(BigDecimal modelProbability) { this.modelProbability = modelProbability; }

    public String getDiagnosis() { return diagnosis; }
    public void setDiagnosis(String diagnosis) { this.diagnosis = diagnosis; }

    public String getCandidateActions() { return candidateActions; }
    public void setCandidateActions(String candidateActions) { this.candidateActions = candidateActions; }

    public RecoveryActionType getSelectedAction() { return selectedAction; }
    public void setSelectedAction(RecoveryActionType selectedAction) { this.selectedAction = selectedAction; }

    public String getReasoningSummary() { return reasoningSummary; }
    public void setReasoningSummary(String reasoningSummary) { this.reasoningSummary = reasoningSummary; }

    public String getPolicyChecks() { return policyChecks; }
    public void setPolicyChecks(String policyChecks) { this.policyChecks = policyChecks; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
