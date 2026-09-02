package com.recoverai.backend.policy;

import com.recoverai.backend.domain.entity.RecoveryPolicy;
import com.recoverai.backend.domain.enums.PolicyDecisionOutcome;
import com.recoverai.backend.domain.enums.RecoveryActionType;
import com.recoverai.backend.domain.enums.RecoveryCaseStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

class PolicyEngineTest {

    private PolicyEngine policyEngine;
    private RecoveryPolicy defaultPolicy;

    @BeforeEach
    void setUp() {
        policyEngine = new PolicyEngine();
        defaultPolicy = new RecoveryPolicy();
        defaultPolicy.setMaxRetryCount(3);
        defaultPolicy.setMinRecoveryProbability(new BigDecimal("0.6000"));
        defaultPolicy.setAutomaticActionLimit(new BigDecimal("50000.0000"));
        defaultPolicy.setHumanApprovalThreshold(new BigDecimal("100000.0000"));
        defaultPolicy.setCooldownMinutes(60);
        defaultPolicy.setAutoRecoveryEnabled(true);
    }

    private PolicyEvaluationContext.Builder createValidContextBuilder() {
        return PolicyEvaluationContext.builder()
                .policy(defaultPolicy)
                .caseStatus(RecoveryCaseStatus.OPEN)
                .proposedAction(RecoveryActionType.RETRY)
                .paymentAmount(new BigDecimal("2500.00"))
                .retryCount(1)
                .recoveryProbability(new BigDecimal("0.8500"))
                .failureCode("TRANSIENT_NETWORK_TIMEOUT")
                .evaluationTime(Instant.now());
    }

    // --- Case Eligibility Tests ---

    @Test
    @DisplayName("1. OPEN case can be evaluated and allowed")
    void testOpenCaseCanBeEvaluated() {
        PolicyEvaluationContext context = createValidContextBuilder().caseStatus(RecoveryCaseStatus.OPEN).build();
        PolicyEvaluationResult result = policyEngine.evaluate(context);
        assertEquals(PolicyDecisionOutcome.ACTION_ALLOWED, result.getDecision());
        assertEquals("ALL_POLICY_CHECKS_PASSED", result.getReason());
        assertTrue(result.getChecks().isRecoveryCaseEligible());
    }

    @Test
    @DisplayName("2. RESOLVED case is blocked")
    void testResolvedCaseBlocked() {
        PolicyEvaluationContext context = createValidContextBuilder().caseStatus(RecoveryCaseStatus.RECOVERED).build();
        PolicyEvaluationResult result = policyEngine.evaluate(context);
        assertEquals(PolicyDecisionOutcome.ACTION_BLOCKED, result.getDecision());
        assertEquals("RECOVERY_CASE_NOT_ELIGIBLE", result.getReason());
        assertFalse(result.getChecks().isRecoveryCaseEligible());
    }

    @Test
    @DisplayName("3. PERMANENTLY_FAILED case is blocked")
    void testPermanentlyFailedCaseBlocked() {
        PolicyEvaluationContext context = createValidContextBuilder().caseStatus(RecoveryCaseStatus.FAILED).build();
        PolicyEvaluationResult result = policyEngine.evaluate(context);
        assertEquals(PolicyDecisionOutcome.ACTION_BLOCKED, result.getDecision());
        assertEquals("RECOVERY_CASE_NOT_ELIGIBLE", result.getReason());
    }

    @Test
    @DisplayName("4. STOPPED / ESCALATED case is blocked")
    void testStoppedOrEscalatedCaseBlocked() {
        PolicyEvaluationContext context = createValidContextBuilder().caseStatus(RecoveryCaseStatus.STOPPED).build();
        PolicyEvaluationResult result = policyEngine.evaluate(context);
        assertEquals(PolicyDecisionOutcome.ACTION_BLOCKED, result.getDecision());
        assertEquals("RECOVERY_CASE_NOT_ELIGIBLE", result.getReason());

        context = createValidContextBuilder().caseStatus(RecoveryCaseStatus.ESCALATED).build();
        result = policyEngine.evaluate(context);
        assertEquals(PolicyDecisionOutcome.ACTION_BLOCKED, result.getDecision());
    }

    // --- Auto Recovery Tests ---

    @Test
    @DisplayName("5. Auto-recovery disabled requires human approval")
    void testAutoRecoveryDisabledRequiresHumanApproval() {
        defaultPolicy.setAutoRecoveryEnabled(false);
        PolicyEvaluationContext context = createValidContextBuilder().build();
        PolicyEvaluationResult result = policyEngine.evaluate(context);
        assertEquals(PolicyDecisionOutcome.HUMAN_APPROVAL_REQUIRED, result.getDecision());
        assertEquals("AUTO_RECOVERY_DISABLED", result.getReason());
        assertFalse(result.getChecks().isAutoRecoveryEnabled());
    }

    // --- Retry Limit Tests ---

    @Test
    @DisplayName("6. retry_count below limit continues")
    void testRetryCountBelowLimit() {
        PolicyEvaluationContext context = createValidContextBuilder().retryCount(2).build();
        PolicyEvaluationResult result = policyEngine.evaluate(context);
        assertEquals(PolicyDecisionOutcome.ACTION_ALLOWED, result.getDecision());
        assertTrue(result.getChecks().isRetryLimitPassed());
    }

    @Test
    @DisplayName("7. retry_count at limit is blocked")
    void testRetryCountAtLimit() {
        PolicyEvaluationContext context = createValidContextBuilder().retryCount(3).build();
        PolicyEvaluationResult result = policyEngine.evaluate(context);
        assertEquals(PolicyDecisionOutcome.ACTION_BLOCKED, result.getDecision());
        assertEquals("MAX_RETRY_COUNT_REACHED", result.getReason());
        assertFalse(result.getChecks().isRetryLimitPassed());
    }

    @Test
    @DisplayName("8. retry_count above limit is blocked")
    void testRetryCountAboveLimit() {
        PolicyEvaluationContext context = createValidContextBuilder().retryCount(4).build();
        PolicyEvaluationResult result = policyEngine.evaluate(context);
        assertEquals(PolicyDecisionOutcome.ACTION_BLOCKED, result.getDecision());
        assertEquals("MAX_RETRY_COUNT_REACHED", result.getReason());
    }

    // --- Recovery Probability Tests ---

    @Test
    @DisplayName("9. probability below minimum threshold is blocked")
    void testProbabilityBelowMinimum() {
        PolicyEvaluationContext context = createValidContextBuilder().recoveryProbability(new BigDecimal("0.5900")).build();
        PolicyEvaluationResult result = policyEngine.evaluate(context);
        assertEquals(PolicyDecisionOutcome.ACTION_BLOCKED, result.getDecision());
        assertEquals("RECOVERY_PROBABILITY_BELOW_THRESHOLD", result.getReason());
        assertFalse(result.getChecks().isProbabilityThresholdPassed());
    }

    @Test
    @DisplayName("10. probability exactly at threshold continues")
    void testProbabilityExactlyAtThreshold() {
        PolicyEvaluationContext context = createValidContextBuilder().recoveryProbability(new BigDecimal("0.6000")).build();
        PolicyEvaluationResult result = policyEngine.evaluate(context);
        assertEquals(PolicyDecisionOutcome.ACTION_ALLOWED, result.getDecision());
        assertTrue(result.getChecks().isProbabilityThresholdPassed());
    }

    @Test
    @DisplayName("11. probability above threshold continues")
    void testProbabilityAboveThreshold() {
        PolicyEvaluationContext context = createValidContextBuilder().recoveryProbability(new BigDecimal("0.7500")).build();
        PolicyEvaluationResult result = policyEngine.evaluate(context);
        assertEquals(PolicyDecisionOutcome.ACTION_ALLOWED, result.getDecision());
    }

    // --- Permanent Failure Tests ---

    @Test
    @DisplayName("12. permanent failure code is blocked")
    void testPermanentFailureBlocked() {
        PolicyEvaluationContext context = createValidContextBuilder().failureCode("CARD_EXPIRED").build();
        PolicyEvaluationResult result = policyEngine.evaluate(context);
        assertEquals(PolicyDecisionOutcome.ACTION_BLOCKED, result.getDecision());
        assertEquals("PERMANENT_FAILURE", result.getReason());
        assertFalse(result.getChecks().isPermanentFailureCheckPassed());
    }

    // --- Monetary Limit Tests ---

    @Test
    @DisplayName("13. amount below automatic limit continues")
    void testAmountBelowAutomaticLimit() {
        PolicyEvaluationContext context = createValidContextBuilder().paymentAmount(new BigDecimal("49999.00")).build();
        PolicyEvaluationResult result = policyEngine.evaluate(context);
        assertEquals(PolicyDecisionOutcome.ACTION_ALLOWED, result.getDecision());
    }

    @Test
    @DisplayName("14. amount exactly at automatic limit continues")
    void testAmountExactlyAtLimit() {
        PolicyEvaluationContext context = createValidContextBuilder().paymentAmount(new BigDecimal("50000.0000")).build();
        PolicyEvaluationResult result = policyEngine.evaluate(context);
        assertEquals(PolicyDecisionOutcome.ACTION_ALLOWED, result.getDecision());
    }

    @Test
    @DisplayName("15. amount above automatic limit requires human approval")
    void testAmountAboveLimitRequiresApproval() {
        PolicyEvaluationContext context = createValidContextBuilder().paymentAmount(new BigDecimal("50001.00")).build();
        PolicyEvaluationResult result = policyEngine.evaluate(context);
        assertEquals(PolicyDecisionOutcome.HUMAN_APPROVAL_REQUIRED, result.getDecision());
        assertEquals("AMOUNT_REQUIRES_APPROVAL", result.getReason());
        assertFalse(result.getChecks().isAmountWithinAutomaticLimit());
    }

    // --- Cooldown Tests ---

    @Test
    @DisplayName("16. no previous attempt continues")
    void testNoPreviousAttempt() {
        PolicyEvaluationContext context = createValidContextBuilder().lastAttemptTime(null).build();
        PolicyEvaluationResult result = policyEngine.evaluate(context);
        assertEquals(PolicyDecisionOutcome.ACTION_ALLOWED, result.getDecision());
        assertTrue(result.getChecks().isCooldownPassed());
    }

    @Test
    @DisplayName("17. previous attempt inside cooldown is blocked")
    void testPreviousAttemptInsideCooldown() {
        Instant now = Instant.now();
        Instant lastAttempt = now.minus(30, ChronoUnit.MINUTES); // 30 mins ago vs 60 mins cooldown
        PolicyEvaluationContext context = createValidContextBuilder()
                .lastAttemptTime(lastAttempt)
                .evaluationTime(now)
                .build();
        PolicyEvaluationResult result = policyEngine.evaluate(context);
        assertEquals(PolicyDecisionOutcome.ACTION_BLOCKED, result.getDecision());
        assertEquals("COOLDOWN_ACTIVE", result.getReason());
        assertFalse(result.getChecks().isCooldownPassed());
    }

    @Test
    @DisplayName("18. previous attempt outside cooldown continues")
    void testPreviousAttemptOutsideCooldown() {
        Instant now = Instant.now();
        Instant lastAttempt = now.minus(61, ChronoUnit.MINUTES); // 61 mins ago vs 60 mins cooldown
        PolicyEvaluationContext context = createValidContextBuilder()
                .lastAttemptTime(lastAttempt)
                .evaluationTime(now)
                .build();
        PolicyEvaluationResult result = policyEngine.evaluate(context);
        assertEquals(PolicyDecisionOutcome.ACTION_ALLOWED, result.getDecision());
        assertTrue(result.getChecks().isCooldownPassed());
    }

    // --- Action Eligibility Tests ---

    @Test
    @DisplayName("19. supported action continues")
    void testSupportedAction() {
        PolicyEvaluationContext context = createValidContextBuilder().proposedAction(RecoveryActionType.PAYMENT_LINK).build();
        PolicyEvaluationResult result = policyEngine.evaluate(context);
        assertEquals(PolicyDecisionOutcome.ACTION_ALLOWED, result.getDecision());

        context = createValidContextBuilder().proposedAction(RecoveryActionType.NOTIFICATION).build();
        result = policyEngine.evaluate(context);
        assertEquals(PolicyDecisionOutcome.ACTION_ALLOWED, result.getDecision());
    }

    @Test
    @DisplayName("20. unsupported action is blocked")
    void testUnsupportedActionBlocked() {
        PolicyEvaluationContext context = createValidContextBuilder().proposedAction(RecoveryActionType.STOP).build();
        PolicyEvaluationResult result = policyEngine.evaluate(context);
        assertEquals(PolicyDecisionOutcome.ACTION_BLOCKED, result.getDecision());
        assertEquals("UNSUPPORTED_ACTION", result.getReason());

        context = createValidContextBuilder().proposedAction(RecoveryActionType.ESCALATE).build();
        result = policyEngine.evaluate(context);
        assertEquals(PolicyDecisionOutcome.ACTION_BLOCKED, result.getDecision());
    }

    // --- Determinism Test ---

    @Test
    @DisplayName("21. same input + same policy yields identical result")
    void testDeterminism() {
        PolicyEvaluationContext context = createValidContextBuilder().build();
        PolicyEvaluationResult result1 = policyEngine.evaluate(context);
        PolicyEvaluationResult result2 = policyEngine.evaluate(context);

        assertEquals(result1.getDecision(), result2.getDecision());
        assertEquals(result1.getReason(), result2.getReason());
        assertEquals(result1.getChecks().isRecoveryCaseEligible(), result2.getChecks().isRecoveryCaseEligible());
    }

    // --- Edge Cases / Security Fail-Closed Tests ---

    @Test
    @DisplayName("Edge case: null context fails closed")
    void testNullContextFailsClosed() {
        PolicyEvaluationResult result = policyEngine.evaluate(null);
        assertEquals(PolicyDecisionOutcome.ACTION_BLOCKED, result.getDecision());
        assertEquals("MISSING_POLICY_CONTEXT", result.getReason());
    }

    @Test
    @DisplayName("Edge case: null policy fails closed")
    void testNullPolicyFailsClosed() {
        PolicyEvaluationContext context = createValidContextBuilder().policy(null).build();
        PolicyEvaluationResult result = policyEngine.evaluate(context);
        assertEquals(PolicyDecisionOutcome.ACTION_BLOCKED, result.getDecision());
        assertEquals("MISSING_POLICY_CONTEXT", result.getReason());
    }

    @Test
    @DisplayName("Edge case: null probability fails closed")
    void testNullProbabilityFailsClosed() {
        PolicyEvaluationContext context = createValidContextBuilder().recoveryProbability(null).build();
        PolicyEvaluationResult result = policyEngine.evaluate(context);
        assertEquals(PolicyDecisionOutcome.ACTION_BLOCKED, result.getDecision());
        assertEquals("MISSING_RECOVERY_PROBABILITY", result.getReason());
    }

    @Test
    @DisplayName("Edge case: negative amount fails closed")
    void testNegativeAmountFailsClosed() {
        PolicyEvaluationContext context = createValidContextBuilder().paymentAmount(new BigDecimal("-100.00")).build();
        PolicyEvaluationResult result = policyEngine.evaluate(context);
        assertEquals(PolicyDecisionOutcome.ACTION_BLOCKED, result.getDecision());
        assertEquals("INVALID_INPUT", result.getReason());
    }

    @Test
    @DisplayName("Edge case: negative retry count fails closed")
    void testNegativeRetryCountFailsClosed() {
        PolicyEvaluationContext context = createValidContextBuilder().retryCount(-1).build();
        PolicyEvaluationResult result = policyEngine.evaluate(context);
        assertEquals(PolicyDecisionOutcome.ACTION_BLOCKED, result.getDecision());
        assertEquals("INVALID_INPUT", result.getReason());
    }
}
