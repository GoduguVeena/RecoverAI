package com.recoverai.backend.policy;

import com.recoverai.backend.domain.enums.PolicyDecisionOutcome;
import com.recoverai.backend.domain.enums.RecoveryActionType;

import java.math.BigDecimal;

public class PolicyEvaluationResult {

    private PolicyDecisionOutcome decision;
    private RecoveryActionType proposedAction;
    private String reason;
    private BigDecimal recoveryProbability;
    private BigDecimal paymentAmount;
    private PolicyCheckDetails checks;

    public PolicyEvaluationResult() {
    }

    public PolicyEvaluationResult(PolicyDecisionOutcome decision,
                                  RecoveryActionType proposedAction,
                                  String reason,
                                  BigDecimal recoveryProbability,
                                  BigDecimal paymentAmount,
                                  PolicyCheckDetails checks) {
        this.decision = decision;
        this.proposedAction = proposedAction;
        this.reason = reason;
        this.recoveryProbability = recoveryProbability;
        this.paymentAmount = paymentAmount;
        this.checks = checks;
    }

    public PolicyDecisionOutcome getDecision() { return decision; }
    public void setDecision(PolicyDecisionOutcome decision) { this.decision = decision; }

    public RecoveryActionType getProposedAction() { return proposedAction; }
    public void setProposedAction(RecoveryActionType proposedAction) { this.proposedAction = proposedAction; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public BigDecimal getRecoveryProbability() { return recoveryProbability; }
    public void setRecoveryProbability(BigDecimal recoveryProbability) { this.recoveryProbability = recoveryProbability; }

    public BigDecimal getPaymentAmount() { return paymentAmount; }
    public void setPaymentAmount(BigDecimal paymentAmount) { this.paymentAmount = paymentAmount; }

    public PolicyCheckDetails getChecks() { return checks; }
    public void setChecks(PolicyCheckDetails checks) { this.checks = checks; }
}
