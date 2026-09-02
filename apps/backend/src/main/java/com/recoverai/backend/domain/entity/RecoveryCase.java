package com.recoverai.backend.domain.entity;

import com.recoverai.backend.domain.enums.RecoveryActionType;
import com.recoverai.backend.domain.enums.RecoveryCaseStatus;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "recovery_cases")
public class RecoveryCase {

    @Id
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_id", nullable = false, unique = true)
    private Payment payment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private RecoveryCaseStatus status = RecoveryCaseStatus.OPEN;

    @Column(name = "recovery_probability", precision = 5, scale = 4)
    private BigDecimal recoveryProbability;

    @Column(columnDefinition = "TEXT")
    private String diagnosis;

    @Column(name = "expected_recovery_value", precision = 19, scale = 4)
    private BigDecimal expectedRecoveryValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "recommended_action", length = 50)
    private RecoveryActionType recommendedAction;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_action", length = 50)
    private RecoveryActionType currentAction;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @OneToMany(mappedBy = "recoveryCase", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<RecoveryAttempt> attempts = new ArrayList<>();

    @OneToMany(mappedBy = "recoveryCase", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<AgentDecision> agentDecisions = new ArrayList<>();

    public RecoveryCase() {
    }

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Payment getPayment() { return payment; }
    public void setPayment(Payment payment) { this.payment = payment; }

    public RecoveryCaseStatus getStatus() { return status; }
    public void setStatus(RecoveryCaseStatus status) { this.status = status; }

    public BigDecimal getRecoveryProbability() { return recoveryProbability; }
    public void setRecoveryProbability(BigDecimal recoveryProbability) { this.recoveryProbability = recoveryProbability; }

    public String getDiagnosis() { return diagnosis; }
    public void setDiagnosis(String diagnosis) { this.diagnosis = diagnosis; }

    public BigDecimal getExpectedRecoveryValue() { return expectedRecoveryValue; }
    public void setExpectedRecoveryValue(BigDecimal expectedRecoveryValue) { this.expectedRecoveryValue = expectedRecoveryValue; }

    public RecoveryActionType getRecommendedAction() { return recommendedAction; }
    public void setRecommendedAction(RecoveryActionType recommendedAction) { this.recommendedAction = recommendedAction; }

    public RecoveryActionType getCurrentAction() { return currentAction; }
    public void setCurrentAction(RecoveryActionType currentAction) { this.currentAction = currentAction; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public Instant getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(Instant resolvedAt) { this.resolvedAt = resolvedAt; }

    public List<RecoveryAttempt> getAttempts() { return attempts; }
    public void setAttempts(List<RecoveryAttempt> attempts) { this.attempts = attempts; }

    public List<AgentDecision> getAgentDecisions() { return agentDecisions; }
    public void setAgentDecisions(List<AgentDecision> agentDecisions) { this.agentDecisions = agentDecisions; }
}
