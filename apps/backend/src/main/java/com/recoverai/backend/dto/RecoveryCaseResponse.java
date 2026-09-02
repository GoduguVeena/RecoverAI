package com.recoverai.backend.dto;

import com.recoverai.backend.domain.entity.RecoveryCase;
import com.recoverai.backend.domain.enums.RecoveryActionType;
import com.recoverai.backend.domain.enums.RecoveryCaseStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class RecoveryCaseResponse {

    private UUID id;
    private UUID paymentId;
    private UUID merchantId;
    private RecoveryCaseStatus status;
    private BigDecimal recoveryProbability;
    private String diagnosis;
    private BigDecimal expectedRecoveryValue;
    private RecoveryActionType recommendedAction;
    private RecoveryActionType currentAction;
    private Instant createdAt;
    private Instant resolvedAt;

    public RecoveryCaseResponse() {
    }

    public static RecoveryCaseResponse from(RecoveryCase recoveryCase) {
        RecoveryCaseResponse dto = new RecoveryCaseResponse();
        dto.setId(recoveryCase.getId());
        dto.setPaymentId(recoveryCase.getPayment().getId());
        dto.setMerchantId(recoveryCase.getPayment().getMerchant().getId());
        dto.setStatus(recoveryCase.getStatus());
        dto.setRecoveryProbability(recoveryCase.getRecoveryProbability());
        dto.setDiagnosis(recoveryCase.getDiagnosis());
        dto.setExpectedRecoveryValue(recoveryCase.getExpectedRecoveryValue());
        dto.setRecommendedAction(recoveryCase.getRecommendedAction());
        dto.setCurrentAction(recoveryCase.getCurrentAction());
        dto.setCreatedAt(recoveryCase.getCreatedAt());
        dto.setResolvedAt(recoveryCase.getResolvedAt());
        return dto;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getPaymentId() { return paymentId; }
    public void setPaymentId(UUID paymentId) { this.paymentId = paymentId; }

    public UUID getMerchantId() { return merchantId; }
    public void setMerchantId(UUID merchantId) { this.merchantId = merchantId; }

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

    public Instant getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(Instant resolvedAt) { this.resolvedAt = resolvedAt; }
}
