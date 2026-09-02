package com.recoverai.backend.policy;

import com.recoverai.backend.domain.entity.RecoveryPolicy;
import com.recoverai.backend.domain.enums.PolicyDecisionOutcome;
import com.recoverai.backend.domain.enums.RecoveryActionType;
import com.recoverai.backend.domain.enums.RecoveryCaseStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;

@Service
public class PolicyEngine {

    private static final Set<String> PERMANENT_FAILURE_CODES = Set.of(
        "CARD_EXPIRED",
        "INVALID_ACCOUNT",
        "RISK_REJECTED",
        "INVALID_CARD",
        "ACCOUNT_CLOSED",
        "DO_NOT_HONOR",
        "PERMANENT_FAILURE"
    );

    private static final Set<RecoveryCaseStatus> ELIGIBLE_CASE_STATUSES = Set.of(
        RecoveryCaseStatus.OPEN,
        RecoveryCaseStatus.ANALYZING,
        RecoveryCaseStatus.ACTION_PENDING
    );

    public PolicyEvaluationResult evaluate(PolicyEvaluationContext context) {
        PolicyCheckDetails checks = new PolicyCheckDetails();

        // Rule 0 — Fail-Closed Input Safety Guard
        if (context == null || context.getPolicy() == null) {
            return buildResult(PolicyDecisionOutcome.ACTION_BLOCKED, null, "MISSING_POLICY_CONTEXT", null, null, checks);
        }

        RecoveryPolicy policy = context.getPolicy();
        BigDecimal amount = context.getPaymentAmount();
        Integer retryCount = context.getRetryCount();
        BigDecimal probability = context.getRecoveryProbability();
        RecoveryActionType action = context.getProposedAction();
        RecoveryCaseStatus caseStatus = context.getCaseStatus();

        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0 ||
            retryCount == null || retryCount < 0) {
            return buildResult(PolicyDecisionOutcome.ACTION_BLOCKED, action, "INVALID_INPUT", probability, amount, checks);
        }

        // Rule 1 — Recovery Case Eligibility
        if (caseStatus == null || !ELIGIBLE_CASE_STATUSES.contains(caseStatus)) {
            checks.setRecoveryCaseEligible(false);
            return buildResult(PolicyDecisionOutcome.ACTION_BLOCKED, action, "RECOVERY_CASE_NOT_ELIGIBLE", probability, amount, checks);
        }
        checks.setRecoveryCaseEligible(true);

        // Rule 2 — Permanent Failure Check
        if (isPermanentFailure(context.getFailureCode(), context.getFailureReason())) {
            checks.setPermanentFailureCheckPassed(false);
            return buildResult(PolicyDecisionOutcome.ACTION_BLOCKED, action, "PERMANENT_FAILURE", probability, amount, checks);
        }
        checks.setPermanentFailureCheckPassed(true);

        // Rule 3 — Auto Recovery Enabled
        if (Boolean.FALSE.equals(policy.getAutoRecoveryEnabled())) {
            checks.setAutoRecoveryEnabled(false);
            return buildResult(PolicyDecisionOutcome.HUMAN_APPROVAL_REQUIRED, action, "AUTO_RECOVERY_DISABLED", probability, amount, checks);
        }
        checks.setAutoRecoveryEnabled(true);

        // Rule 4 — Maximum Retry Count
        if (policy.getMaxRetryCount() != null && retryCount >= policy.getMaxRetryCount()) {
            checks.setRetryLimitPassed(false);
            return buildResult(PolicyDecisionOutcome.ACTION_BLOCKED, action, "MAX_RETRY_COUNT_REACHED", probability, amount, checks);
        }
        checks.setRetryLimitPassed(true);

        // Rule 5 — Minimum Recovery Probability Check
        if (probability == null) {
            checks.setProbabilityThresholdPassed(false);
            return buildResult(PolicyDecisionOutcome.ACTION_BLOCKED, action, "MISSING_RECOVERY_PROBABILITY", probability, amount, checks);
        }

        if (policy.getMinRecoveryProbability() != null && probability.compareTo(policy.getMinRecoveryProbability()) < 0) {
            checks.setProbabilityThresholdPassed(false);
            return buildResult(PolicyDecisionOutcome.ACTION_BLOCKED, action, "RECOVERY_PROBABILITY_BELOW_THRESHOLD", probability, amount, checks);
        }
        checks.setProbabilityThresholdPassed(true);

        // Rule 6 — Cooldown Check
        Instant evalTime = context.getEvaluationTime() != null ? context.getEvaluationTime() : Instant.now();
        if (context.getLastAttemptTime() != null && policy.getCooldownMinutes() != null && policy.getCooldownMinutes() > 0) {
            long minutesSinceLastAttempt = Duration.between(context.getLastAttemptTime(), evalTime).toMinutes();
            if (minutesSinceLastAttempt < policy.getCooldownMinutes()) {
                checks.setCooldownPassed(false);
                return buildResult(PolicyDecisionOutcome.ACTION_BLOCKED, action, "COOLDOWN_ACTIVE", probability, amount, checks);
            }
        }
        checks.setCooldownPassed(true);

        // Rule 7 — Monetary Limits / Human Approval Threshold
        if (policy.getHumanApprovalThreshold() != null && amount.compareTo(policy.getHumanApprovalThreshold()) > 0) {
            checks.setHumanApprovalThresholdCheckPassed(false);
            checks.setAmountWithinAutomaticLimit(false);
            return buildResult(PolicyDecisionOutcome.HUMAN_APPROVAL_REQUIRED, action, "AMOUNT_REQUIRES_APPROVAL", probability, amount, checks);
        }
        if (policy.getAutomaticActionLimit() != null && amount.compareTo(policy.getAutomaticActionLimit()) > 0) {
            checks.setAmountWithinAutomaticLimit(false);
            return buildResult(PolicyDecisionOutcome.HUMAN_APPROVAL_REQUIRED, action, "AMOUNT_REQUIRES_APPROVAL", probability, amount, checks);
        }
        checks.setAmountWithinAutomaticLimit(true);
        checks.setHumanApprovalThresholdCheckPassed(true);

        // Rule 8 — Action Eligibility
        if (action == null || !isActionSupported(action)) {
            checks.setActionSupported(false);
            return buildResult(PolicyDecisionOutcome.ACTION_BLOCKED, action, "UNSUPPORTED_ACTION", probability, amount, checks);
        }
        checks.setActionSupported(true);

        // All checks passed
        return buildResult(PolicyDecisionOutcome.ACTION_ALLOWED, action, "ALL_POLICY_CHECKS_PASSED", probability, amount, checks);
    }

    private boolean isPermanentFailure(String failureCode, String failureReason) {
        if (failureCode != null && PERMANENT_FAILURE_CODES.contains(failureCode.toUpperCase())) {
            return true;
        }
        if (failureReason != null) {
            String upperReason = failureReason.toUpperCase();
            for (String code : PERMANENT_FAILURE_CODES) {
                if (upperReason.contains(code)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isActionSupported(RecoveryActionType action) {
        return action == RecoveryActionType.RETRY ||
               action == RecoveryActionType.PAYMENT_LINK ||
               action == RecoveryActionType.NOTIFICATION;
    }

    private PolicyEvaluationResult buildResult(PolicyDecisionOutcome decision,
                                                RecoveryActionType proposedAction,
                                                String reason,
                                                BigDecimal probability,
                                                BigDecimal amount,
                                                PolicyCheckDetails checks) {
        return new PolicyEvaluationResult(decision, proposedAction, reason, probability, amount, checks);
    }
}
