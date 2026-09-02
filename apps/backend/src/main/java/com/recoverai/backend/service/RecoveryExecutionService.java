package com.recoverai.backend.service;

import com.recoverai.backend.domain.entity.*;
import com.recoverai.backend.domain.enums.ActorType;
import com.recoverai.backend.domain.enums.PolicyDecisionOutcome;
import com.recoverai.backend.domain.enums.RecoveryActionType;
import com.recoverai.backend.domain.enums.RecoveryAttemptOutcome;
import com.recoverai.backend.domain.enums.RecoveryCaseStatus;
import com.recoverai.backend.dto.ExecutionContext;
import com.recoverai.backend.dto.ExecutionResult;
import com.recoverai.backend.dto.RecoveryExecutionResponse;
import com.recoverai.backend.exception.InvalidRequestException;
import com.recoverai.backend.exception.ResourceNotFoundException;
import com.recoverai.backend.execution.RecoveryExecutionAdapter;
import com.recoverai.backend.policy.PolicyEngine;
import com.recoverai.backend.policy.PolicyEvaluationContext;
import com.recoverai.backend.policy.PolicyEvaluationResult;
import com.recoverai.backend.repository.AgentDecisionRepository;
import com.recoverai.backend.repository.RecoveryAttemptRepository;
import com.recoverai.backend.repository.RecoveryCaseRepository;
import com.recoverai.backend.repository.RecoveryPolicyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class RecoveryExecutionService {

    private static final Logger log = LoggerFactory.getLogger(RecoveryExecutionService.class);

    private static final Set<RecoveryCaseStatus> EXECUTABLE_CASE_STATUSES = Set.of(
            RecoveryCaseStatus.OPEN,
            RecoveryCaseStatus.ANALYZING,
            RecoveryCaseStatus.ACTION_PENDING
    );

    private final RecoveryCaseRepository recoveryCaseRepository;
    private final RecoveryPolicyRepository recoveryPolicyRepository;
    private final AgentDecisionRepository agentDecisionRepository;
    private final RecoveryAttemptRepository recoveryAttemptRepository;
    private final RecoveryExecutionAdapter recoveryExecutionAdapter;
    private final PolicyEngine policyEngine;
    private final AuditLogService auditLogService;

    public RecoveryExecutionService(RecoveryCaseRepository recoveryCaseRepository,
                                   RecoveryPolicyRepository recoveryPolicyRepository,
                                   AgentDecisionRepository agentDecisionRepository,
                                   RecoveryAttemptRepository recoveryAttemptRepository,
                                   RecoveryExecutionAdapter recoveryExecutionAdapter,
                                   PolicyEngine policyEngine,
                                   AuditLogService auditLogService) {
        this.recoveryCaseRepository = recoveryCaseRepository;
        this.recoveryPolicyRepository = recoveryPolicyRepository;
        this.agentDecisionRepository = agentDecisionRepository;
        this.recoveryAttemptRepository = recoveryAttemptRepository;
        this.recoveryExecutionAdapter = recoveryExecutionAdapter;
        this.policyEngine = policyEngine;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public RecoveryExecutionResponse executeRecoveryAction(UUID caseId, String requestId) {
        RecoveryCase recoveryCase = recoveryCaseRepository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("Recovery case not found with id: " + caseId));

        if (!EXECUTABLE_CASE_STATUSES.contains(recoveryCase.getStatus())) {
            throw new InvalidRequestException("Recovery case in status " + recoveryCase.getStatus() + " is not eligible for execution.");
        }

        Payment payment = recoveryCase.getPayment();
        Merchant merchant = payment.getMerchant();

        // 1. Derive proposed action from latest AgentDecision (Client cannot inject arbitrary actions)
        List<AgentDecision> decisions = agentDecisionRepository.findByRecoveryCaseId(caseId);
        if (decisions.isEmpty()) {
            throw new InvalidRequestException("No agent decision found for case " + caseId + ". Perform recovery analysis prior to execution.");
        }

        AgentDecision latestDecision = decisions.stream()
                .max(Comparator.comparing(AgentDecision::getCreatedAt))
                .orElseThrow();

        RecoveryActionType proposedAction = latestDecision.getSelectedAction();
        if (proposedAction == null) {
            throw new InvalidRequestException("Agent decision contains no selected action.");
        }

        // 2. Fetch Merchant Policy
        RecoveryPolicy policy = recoveryPolicyRepository.findByMerchantId(merchant.getId())
                .orElseGet(() -> createDefaultPolicy(merchant));

        // 3. Find latest attempt time for cooldown check
        List<RecoveryAttempt> existingAttempts = recoveryAttemptRepository.findByRecoveryCaseId(caseId);
        Instant lastAttemptTime = existingAttempts.stream()
                .map(RecoveryAttempt::getExecutedAt)
                .filter(t -> t != null)
                .max(Instant::compareTo)
                .orElse(null);

        // 4. MANDATORY PRE-EXECUTION POLICY RE-EVALUATION
        BigDecimal probability = recoveryCase.getRecoveryProbability() != null ?
                recoveryCase.getRecoveryProbability() : latestDecision.getModelProbability();

        PolicyEvaluationContext policyContext = PolicyEvaluationContext.builder()
                .policy(policy)
                .recoveryProbability(probability)
                .paymentAmount(payment.getAmount())
                .retryCount(payment.getRetryCount())
                .proposedAction(proposedAction)
                .failureCode(payment.getFailureCode())
                .failureReason(payment.getFailureReason())
                .caseStatus(recoveryCase.getStatus())
                .lastAttemptTime(lastAttemptTime)
                .build();

        PolicyEvaluationResult policyResult = policyEngine.evaluate(policyContext);

        // 5. IF POLICY BLOCKED OR HUMAN APPROVAL REQUIRED -> DO NOT EXECUTE
        if (policyResult.getDecision() != PolicyDecisionOutcome.ACTION_ALLOWED) {
            RecoveryAttempt blockedAttempt = new RecoveryAttempt();
            blockedAttempt.setRecoveryCase(recoveryCase);
            blockedAttempt.setActionType(proposedAction);
            blockedAttempt.setPolicyResult(policyResult.getDecision().name() + ": " + policyResult.getReason());
            blockedAttempt.setApprovalRequired(policyResult.getDecision() == PolicyDecisionOutcome.HUMAN_APPROVAL_REQUIRED);
            blockedAttempt.setExecutedAt(Instant.now());
            blockedAttempt.setOutcome(policyResult.getDecision() == PolicyDecisionOutcome.HUMAN_APPROVAL_REQUIRED ?
                    RecoveryAttemptOutcome.PENDING : RecoveryAttemptOutcome.BLOCKED);

            blockedAttempt = recoveryAttemptRepository.save(blockedAttempt);

            String eventType = policyResult.getDecision() == PolicyDecisionOutcome.HUMAN_APPROVAL_REQUIRED ?
                    "EXECUTION_APPROVAL_REQUIRED" : "EXECUTION_BLOCKED";

            auditLogService.logEvent(
                    "RecoveryCase", recoveryCase.getId(), eventType,
                    ActorType.SYSTEM, "POLICY_ENGINE", "EXECUTE_RECOVERY",
                    "Policy decision rejected execution: " + policyResult.getReason()
            );

            return new RecoveryExecutionResponse(
                    recoveryCase.getId(),
                    blockedAttempt.getId(),
                    false,
                    policyResult,
                    null
            );
        }

        // 6. Idempotency Check: Return existing attempt if identical action was already simulated recently
        Optional<RecoveryAttempt> recentIdenticalAttempt = existingAttempts.stream()
                .filter(a -> a.getActionType() == proposedAction && a.getOutcome() == RecoveryAttemptOutcome.SUCCESS)
                .findFirst();

        if (recentIdenticalAttempt.isPresent()) {
            log.info("Idempotency: returning existing simulated attempt for case {}", caseId);
            RecoveryAttempt existingAttempt = recentIdenticalAttempt.get();
            ExecutionResult existingResult = new ExecutionResult(
                    existingAttempt.getId(),
                    proposedAction,
                    "SIMULATED",
                    "[SIMULATION] Already simulated (Idempotent response)",
                    true,
                    existingAttempt.getActionPayload()
            );
            return new RecoveryExecutionResponse(
                    recoveryCase.getId(),
                    existingAttempt.getId(),
                    true,
                    policyResult,
                    existingResult
            );
        }

        // 7. Invoke DryRunRecoveryExecutionAdapter
        ExecutionContext executionContext = new ExecutionContext(
                recoveryCase.getId(),
                payment.getId(),
                merchant.getId(),
                requestId,
                "DRY_RUN"
        );

        ExecutionResult executionResult = recoveryExecutionAdapter.execute(proposedAction, policyResult, executionContext);

        // 8. Persist RecoveryAttempt
        RecoveryAttempt attempt = new RecoveryAttempt();
        attempt.setRecoveryCase(recoveryCase);
        attempt.setActionType(proposedAction);
        attempt.setActionPayload(executionResult.getSimulatedPayload());
        attempt.setPolicyResult(policyResult.getDecision().name() + ": " + policyResult.getReason());
        attempt.setApprovalRequired(false);
        attempt.setExecutedAt(Instant.now());
        attempt.setOutcome(RecoveryAttemptOutcome.SUCCESS);

        attempt = recoveryAttemptRepository.save(attempt);

        // 9. Update case status to ACTION_PENDING
        recoveryCase.setStatus(RecoveryCaseStatus.ACTION_PENDING);
        recoveryCaseRepository.save(recoveryCase);

        // 10. Write AuditLog
        auditLogService.logEvent(
                "RecoveryCase", recoveryCase.getId(), "EXECUTION_SIMULATED",
                ActorType.SYSTEM, "EXECUTION_ADAPTER", "EXECUTE_RECOVERY",
                "Simulated action " + proposedAction + " (Execution ID: " + executionResult.getExecutionId() + ")"
        );

        return new RecoveryExecutionResponse(
                recoveryCase.getId(),
                attempt.getId(),
                true,
                policyResult,
                executionResult
        );
    }

    private RecoveryPolicy createDefaultPolicy(Merchant merchant) {
        RecoveryPolicy policy = new RecoveryPolicy();
        policy.setMerchant(merchant);
        policy.setMaxRetryCount(3);
        policy.setMinRecoveryProbability(new BigDecimal("0.6000"));
        policy.setAutomaticActionLimit(new BigDecimal("50000.0000"));
        policy.setHumanApprovalThreshold(new BigDecimal("100000.0000"));
        policy.setCooldownMinutes(60);
        policy.setAutoRecoveryEnabled(true);
        return recoveryPolicyRepository.save(policy);
    }
}
