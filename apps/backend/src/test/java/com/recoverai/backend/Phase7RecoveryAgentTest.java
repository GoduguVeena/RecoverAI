package com.recoverai.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recoverai.backend.domain.entity.*;
import com.recoverai.backend.domain.enums.PaymentStatus;
import com.recoverai.backend.domain.enums.PolicyDecisionOutcome;
import com.recoverai.backend.domain.enums.RecoveryActionType;
import com.recoverai.backend.domain.enums.RecoveryCaseStatus;
import com.recoverai.backend.dto.RecoveryAnalysisResponse;
import com.recoverai.backend.repository.*;
import com.recoverai.backend.service.RecoveryAnalysisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class Phase7RecoveryAgentTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MerchantRepository merchantRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private RecoveryCaseRepository recoveryCaseRepository;

    @Autowired
    private RecoveryPolicyRepository recoveryPolicyRepository;

    @Autowired
    private AgentDecisionRepository agentDecisionRepository;

    @Autowired
    private RecoveryAnalysisService recoveryAnalysisService;

    private Merchant merchant;
    private Customer customer;
    private Payment payment;
    private RecoveryCase recoveryCase;
    private RecoveryPolicy policy;

    @BeforeEach
    void setUp() {
        merchant = new Merchant();
        merchant.setName("Test Merchant");
        merchant = merchantRepository.save(merchant);

        customer = new Customer();
        customer.setMerchant(merchant);
        customer.setExternalCustomerId("cust_ext_100");
        customer.setEmail("cust100@example.com");
        customer = customerRepository.save(customer);

        payment = new Payment();
        payment.setMerchant(merchant);
        payment.setCustomer(customer);
        payment.setAmount(new BigDecimal("2500.00"));
        payment.setStatus(PaymentStatus.FAILED);
        payment.setFailureCode("TRANSIENT_NETWORK_TIMEOUT");
        payment.setFailureReason("Bank gateway timeout");
        payment.setRetryCount(1);
        payment = paymentRepository.save(payment);

        recoveryCase = new RecoveryCase();
        recoveryCase.setPayment(payment);
        recoveryCase.setStatus(RecoveryCaseStatus.OPEN);
        recoveryCase = recoveryCaseRepository.save(recoveryCase);

        policy = new RecoveryPolicy();
        policy.setMerchant(merchant);
        policy.setMaxRetryCount(3);
        policy.setMinRecoveryProbability(new BigDecimal("0.6000"));
        policy.setAutomaticActionLimit(new BigDecimal("50000.0000"));
        policy.setHumanApprovalThreshold(new BigDecimal("100000.0000"));
        policy.setCooldownMinutes(60);
        policy.setAutoRecoveryEnabled(true);
        recoveryPolicyRepository.save(policy);
    }

    @Test
    @DisplayName("1. Agent analyzes open recovery case, recommends RETRY, and persists AgentDecision")
    void testAnalyzeOpenRecoveryCase() {
        RecoveryAnalysisResponse response = recoveryAnalysisService.analyzeRecoveryCase(recoveryCase.getId());

        assertNotNull(response);
        assertEquals(recoveryCase.getId(), response.getCaseId());

        // Verify AgentDecision
        assertNotNull(response.getAgentDecision());
        assertEquals(RecoveryActionType.RETRY, response.getAgentDecision().getSelectedAction());
        assertNotNull(response.getAgentDecision().getReasoningSummary());
        assertNotNull(response.getAgentDecision().getDiagnosis());

        // Verify AgentDecision is persisted in Database
        List<AgentDecision> decisions = agentDecisionRepository.findByRecoveryCaseId(recoveryCase.getId());
        assertEquals(1, decisions.size());
        assertEquals(RecoveryActionType.RETRY, decisions.get(0).getSelectedAction());

        // Verify PolicyDecision
        assertNotNull(response.getPolicyDecision());
        assertEquals(PolicyDecisionOutcome.ACTION_ALLOWED, response.getPolicyDecision().getDecision());
        assertEquals("ALL_POLICY_CHECKS_PASSED", response.getPolicyDecision().getReason());
    }

    @Test
    @DisplayName("2. PolicyEngine BLOCKED result remains blocked even if Agent recommended RETRY (Safety Boundary)")
    void testPolicyEngineBlockedOverridesAgent() {
        // Exceed retry limit in Policy
        policy.setMaxRetryCount(1);
        recoveryPolicyRepository.save(policy);

        RecoveryAnalysisResponse response = recoveryAnalysisService.analyzeRecoveryCase(recoveryCase.getId());

        // Agent still recommended RETRY
        assertEquals(RecoveryActionType.RETRY, response.getAgentDecision().getSelectedAction());

        // But Policy Engine BLOCKED the action!
        assertEquals(PolicyDecisionOutcome.ACTION_BLOCKED, response.getPolicyDecision().getDecision());
        assertEquals("MAX_RETRY_COUNT_REACHED", response.getPolicyDecision().getReason());
    }

    @Test
    @DisplayName("3. POST /api/v1/recovery/cases/{id}/analyze HTTP endpoint returns analysis response")
    void testAnalyzeEndpointHttp() throws Exception {
        mockMvc.perform(post("/api/v1/recovery/cases/" + recoveryCase.getId() + "/analyze")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.caseId").value(recoveryCase.getId().toString()))
                .andExpect(jsonPath("$.data.agentDecision.selectedAction").value("RETRY"))
                .andExpect(jsonPath("$.data.policyDecision.decision").value("ACTION_ALLOWED"));
    }

    @Test
    @DisplayName("4. Analyzing non-existent case returns 404 NOT_FOUND")
    void testAnalyzeNonExistentCaseReturns404() throws Exception {
        UUID nonExistentId = UUID.randomUUID();
        mockMvc.perform(post("/api/v1/recovery/cases/" + nonExistentId + "/analyze")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));
    }
}
