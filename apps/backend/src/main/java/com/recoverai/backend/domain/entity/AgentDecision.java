package com.recoverai.backend.domain.entity;

import com.recoverai.backend.domain.enums.RecoveryActionType;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "agent_decisions")
public class AgentDecision {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recovery_case_id", nullable = false)
    private RecoveryCase recoveryCase;

    @Column(name = "model_version", length = 50)
    private String modelVersion;

    @Column(name = "model_probability", precision = 5, scale = 4)
    private BigDecimal modelProbability;

    @Column(columnDefinition = "TEXT")
    private String diagnosis;

    @Column(name = "candidate_actions", columnDefinition = "TEXT")
    private String candidateActions;

    @Enumerated(EnumType.STRING)
    @Column(name = "selected_action", length = 50)
    private RecoveryActionType selectedAction;

    @Column(name = "reasoning_summary", columnDefinition = "TEXT")
    private String reasoningSummary;

    @Column(name = "policy_checks", columnDefinition = "TEXT")
    private String policyChecks;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public AgentDecision() {
    }

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public RecoveryCase getRecoveryCase() { return recoveryCase; }
    public void setRecoveryCase(RecoveryCase recoveryCase) { this.recoveryCase = recoveryCase; }

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
