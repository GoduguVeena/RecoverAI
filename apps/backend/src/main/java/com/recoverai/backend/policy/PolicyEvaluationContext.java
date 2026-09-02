package com.recoverai.backend.policy;

import com.recoverai.backend.domain.entity.RecoveryPolicy;
import com.recoverai.backend.domain.enums.RecoveryActionType;
import com.recoverai.backend.domain.enums.RecoveryCaseStatus;

import java.math.BigDecimal;
import java.time.Instant;

public class PolicyEvaluationContext {

    private RecoveryPolicy policy;
    private BigDecimal recoveryProbability;
    private BigDecimal paymentAmount;
    private Integer retryCount;
    private RecoveryActionType proposedAction;
    private String failureCode;
    private String failureReason;
    private RecoveryCaseStatus caseStatus;
    private Instant lastAttemptTime;
    private Instant evaluationTime;

    public PolicyEvaluationContext() {
        this.evaluationTime = Instant.now();
    }

    public static Builder builder() {
        return new Builder();
    }

    public RecoveryPolicy getPolicy() { return policy; }
    public void setPolicy(RecoveryPolicy policy) { this.policy = policy; }

    public BigDecimal getRecoveryProbability() { return recoveryProbability; }
    public void setRecoveryProbability(BigDecimal recoveryProbability) { this.recoveryProbability = recoveryProbability; }

    public BigDecimal getPaymentAmount() { return paymentAmount; }
    public void setPaymentAmount(BigDecimal paymentAmount) { this.paymentAmount = paymentAmount; }

    public Integer getRetryCount() { return retryCount; }
    public void setRetryCount(Integer retryCount) { this.retryCount = retryCount; }

    public RecoveryActionType getProposedAction() { return proposedAction; }
    public void setProposedAction(RecoveryActionType proposedAction) { this.proposedAction = proposedAction; }

    public String getFailureCode() { return failureCode; }
    public void setFailureCode(String failureCode) { this.failureCode = failureCode; }

    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }

    public RecoveryCaseStatus getCaseStatus() { return caseStatus; }
    public void setCaseStatus(RecoveryCaseStatus caseStatus) { this.caseStatus = caseStatus; }

    public Instant getLastAttemptTime() { return lastAttemptTime; }
    public void setLastAttemptTime(Instant lastAttemptTime) { this.lastAttemptTime = lastAttemptTime; }

    public Instant getEvaluationTime() { return evaluationTime; }
    public void setEvaluationTime(Instant evaluationTime) { this.evaluationTime = evaluationTime; }

    public static class Builder {
        private final PolicyEvaluationContext context = new PolicyEvaluationContext();

        public Builder policy(RecoveryPolicy policy) { context.setPolicy(policy); return this; }
        public Builder recoveryProbability(BigDecimal probability) { context.setRecoveryProbability(probability); return this; }
        public Builder paymentAmount(BigDecimal amount) { context.setPaymentAmount(amount); return this; }
        public Builder retryCount(Integer count) { context.setRetryCount(count); return this; }
        public Builder proposedAction(RecoveryActionType action) { context.setProposedAction(action); return this; }
        public Builder failureCode(String code) { context.setFailureCode(code); return this; }
        public Builder failureReason(String reason) { context.setFailureReason(reason); return this; }
        public Builder caseStatus(RecoveryCaseStatus status) { context.setCaseStatus(status); return this; }
        public Builder lastAttemptTime(Instant time) { context.setLastAttemptTime(time); return this; }
        public Builder evaluationTime(Instant time) { context.setEvaluationTime(time); return this; }

        public PolicyEvaluationContext build() {
            return context;
        }
    }
}
