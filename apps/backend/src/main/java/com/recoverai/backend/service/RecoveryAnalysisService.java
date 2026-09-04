package com.recoverai.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recoverai.backend.agent.AgentModelClient;
import com.recoverai.backend.agent.AgentOutputValidator;
import com.recoverai.backend.agent.MlPredictionClient;
import com.recoverai.backend.domain.entity.*;
import com.recoverai.backend.domain.enums.ActorType;
import com.recoverai.backend.domain.enums.RecoveryCaseStatus;
import com.recoverai.backend.dto.*;
import com.recoverai.backend.exception.ResourceNotFoundException;
import com.recoverai.backend.policy.PolicyEngine;
import com.recoverai.backend.policy.PolicyEvaluationContext;
import com.recoverai.backend.policy.PolicyEvaluationResult;
import com.recoverai.backend.repository.AgentDecisionRepository;
import com.recoverai.backend.repository.RecoveryCaseRepository;
import com.recoverai.backend.repository.RecoveryPolicyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class RecoveryAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(RecoveryAnalysisService.class);

    private final RecoveryCaseRepository recoveryCaseRepository;
    private final RecoveryPolicyRepository recoveryPolicyRepository;
    private final AgentDecisionRepository agentDecisionRepository;
    private final MlPredictionClient mlPredictionClient;
    private final AgentModelClient agentModelClient;
    private final AgentOutputValidator agentOutputValidator;
    private final PolicyEngine policyEngine;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    public RecoveryAnalysisService(RecoveryCaseRepository recoveryCaseRepository,
                                  RecoveryPolicyRepository recoveryPolicyRepository,
                                  AgentDecisionRepository agentDecisionRepository,
                                  MlPredictionClient mlPredictionClient,
                                  AgentModelClient agentModelClient,
                                  AgentOutputValidator agentOutputValidator,
                                  PolicyEngine policyEngine,
                                  AuditLogService auditLogService) {
        this.recoveryCaseRepository = recoveryCaseRepository;
        this.recoveryPolicyRepository = recoveryPolicyRepository;
        this.agentDecisionRepository = agentDecisionRepository;
        this.mlPredictionClient = mlPredictionClient;
        this.agentModelClient = agentModelClient;
        this.agentOutputValidator = agentOutputValidator;
        this.policyEngine = policyEngine;
        this.auditLogService = auditLogService;
        this.objectMapper = new ObjectMapper();
    }

    @Transactional
    public RecoveryAnalysisResponse analyzeRecoveryCase(UUID caseId) {
        RecoveryCase recoveryCase = recoveryCaseRepository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("Recovery case not found with id: " + caseId));

        Payment payment = recoveryCase.getPayment();
        Merchant merchant = payment.getMerchant();
        Customer customer = payment.getCustomer();

        // 1. Check case status & update to ANALYZING if OPEN
        if (recoveryCase.getStatus() == RecoveryCaseStatus.OPEN) {
            recoveryCase.setStatus(RecoveryCaseStatus.ANALYZING);
            recoveryCase = recoveryCaseRepository.save(recoveryCase);
        }

        // 2. Fetch or create default Merchant RecoveryPolicy
        RecoveryPolicy policy = recoveryPolicyRepository.findByMerchantId(merchant.getId())
                .orElseGet(() -> createDefaultPolicy(merchant));

        // 3. Build ML Prediction Request
        MlPredictionRequest mlRequest = buildMlRequest(payment, merchant, customer);

        // 4. Call ML Prediction Service
        MlPredictionResponse mlResponse = mlPredictionClient.predict(mlRequest);
        BigDecimal probability = mlResponse.getRecoveryProbability();
        String modelVersion = mlResponse.getModelVersion();

        recoveryCase.setRecoveryProbability(probability);

        // 5. Build Agent Request Context
        AgentRequestContext agentContext = buildAgentContext(recoveryCase, payment, merchant, customer, probability, modelVersion);

        // 6. Invoke AI Agent Model Client
        AgentRecommendationResponse recommendation = agentModelClient.generateRecommendation(agentContext);

        // 7. Validate Agent Output
        agentOutputValidator.validate(recommendation);

        recoveryCase.setDiagnosis(recommendation.getDiagnosis());
        recoveryCase.setRecommendedAction(recommendation.getSelectedAction());
        recoveryCase.setCurrentAction(recommendation.getSelectedAction());
        recoveryCaseRepository.save(recoveryCase);

        // 8. Create and persist AgentDecision entity
        AgentDecision agentDecision = new AgentDecision();
        agentDecision.setRecoveryCase(recoveryCase);
        agentDecision.setModelVersion(recommendation.getModelVersion());
        agentDecision.setModelProbability(recommendation.getRecoveryProbability());
        agentDecision.setDiagnosis(recommendation.getDiagnosis());
        agentDecision.setCandidateActions(recommendation.getCandidateActions().toString());
        agentDecision.setSelectedAction(recommendation.getSelectedAction());
        agentDecision.setReasoningSummary(recommendation.getReasoningSummary());

        agentDecision = agentDecisionRepository.save(agentDecision);

        // 9. Evaluate PolicyEngine (Mandatory Authorization Gate)
        PolicyEvaluationContext policyContext = PolicyEvaluationContext.builder()
                .policy(policy)
                .recoveryProbability(probability)
                .paymentAmount(payment.getAmount())
                .retryCount(payment.getRetryCount())
                .proposedAction(recommendation.getSelectedAction())
                .failureCode(payment.getFailureCode())
                .failureReason(payment.getFailureReason())
                .caseStatus(recoveryCase.getStatus())
                .build();

        PolicyEvaluationResult policyResult = policyEngine.evaluate(policyContext);

        // 10. Update AgentDecision with Policy Checks JSON & save
        try {
            agentDecision.setPolicyChecks(objectMapper.writeValueAsString(policyResult.getChecks()));
            agentDecisionRepository.save(agentDecision);
        } catch (Exception e) {
            log.warn("Failed to serialize policy checks: {}", e.getMessage());
        }

        // 11. Audit log
        auditLogService.logEvent(
                "RecoveryCase", recoveryCase.getId(), "RECOVERY_CASE_ANALYZED",
                ActorType.AGENT, "AI_AGENT", "ANALYZE_CASE",
                "Agent recommended " + recommendation.getSelectedAction() + " (Policy result: " + policyResult.getDecision() + ")"
        );

        return new RecoveryAnalysisResponse(
                recoveryCase.getId(),
                AgentDecisionResponse.from(agentDecision),
                policyResult
        );
    }

    private RecoveryPolicy createDefaultPolicy(Merchant merchant) {
        RecoveryPolicy policy = new RecoveryPolicy();
        policy.setMerchant(merchant);
        policy.setMaxRetryCount(merchant.getMaxRetryCount() != null ? merchant.getMaxRetryCount() : 3);
        policy.setMinRecoveryProbability(merchant.getMinRecoveryProbability() != null ? merchant.getMinRecoveryProbability() : new BigDecimal("0.6000"));
        policy.setAutomaticActionLimit(merchant.getAutomaticActionLimit() != null ? merchant.getAutomaticActionLimit() : new BigDecimal("50000.0000"));
        policy.setHumanApprovalThreshold(merchant.getHumanApprovalThreshold() != null ? merchant.getHumanApprovalThreshold() : new BigDecimal("100000.0000"));
        policy.setCooldownMinutes(60);
        policy.setAutoRecoveryEnabled(merchant.getAutoRecoveryEnabled() != null ? merchant.getAutoRecoveryEnabled() : true);
        return recoveryPolicyRepository.save(policy);
    }

    private MlPredictionRequest buildMlRequest(Payment payment, Merchant merchant, Customer customer) {
        MlPredictionRequest request = new MlPredictionRequest();
        request.setMerchantId(merchant.getId().toString());
        request.setCustomerId(customer.getId().toString());
        request.setAmount(payment.getAmount());
        request.setCurrency(payment.getCurrency() != null ? payment.getCurrency() : "INR");
        request.setPaymentMethod(payment.getMethod() != null ? payment.getMethod() : "upi");
        request.setFailureType(payment.getFailureCode() != null ? payment.getFailureCode() : "TRANSIENT_NETWORK_TIMEOUT");
        request.setRetryCount(payment.getRetryCount() != null ? payment.getRetryCount() : 0);
        request.setCustomerTotalTransactions(10);
        request.setCustomerSuccessfulTransactions(8);
        request.setCustomerFailedTransactions(2);
        request.setCustomerSuccessRate(new BigDecimal("0.8000"));
        request.setCustomerTotalSpend(new BigDecimal("15000.00"));
        request.setDaysSinceLastSuccess(new BigDecimal("3.5"));
        request.setCheckoutDurationSeconds(new BigDecimal("45.0"));
        request.setHourOfDay(14);
        request.setDayOfWeek(2);
        request.setMerchantCategory("ecommerce");
        request.setCustomerSegment("regular");
        return request;
    }

    private AgentRequestContext buildAgentContext(RecoveryCase recoveryCase, Payment payment, Merchant merchant, Customer customer, BigDecimal probability, String modelVersion) {
        AgentRequestContext context = new AgentRequestContext();
        context.setRecoveryCaseId(recoveryCase.getId());
        context.setPaymentId(payment.getId());
        context.setAmount(payment.getAmount());
        context.setCurrency(payment.getCurrency());
        context.setPaymentMethod(payment.getMethod());
        context.setFailureCode(payment.getFailureCode());
        context.setFailureReason(payment.getFailureReason());
        context.setRetryCount(payment.getRetryCount());
        context.setCustomerTotalTransactions(10);
        context.setCustomerSuccessfulTransactions(8);
        context.setCustomerFailedTransactions(2);
        context.setCustomerSuccessRate(new BigDecimal("0.8000"));
        context.setDaysSinceLastSuccess(new BigDecimal("3.5"));
        context.setMerchantCategory("ecommerce");
        context.setCustomerSegment("regular");
        context.setRecoveryProbability(probability);
        context.setModelVersion(modelVersion);
        return context;
    }
}
