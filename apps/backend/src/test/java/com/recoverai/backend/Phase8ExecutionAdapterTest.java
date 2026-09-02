package com.recoverai.backend;

import com.recoverai.backend.domain.entity.*;
import com.recoverai.backend.domain.enums.PaymentStatus;
import com.recoverai.backend.domain.enums.PolicyDecisionOutcome;
import com.recoverai.backend.domain.enums.RecoveryActionType;
import com.recoverai.backend.domain.enums.RecoveryAttemptOutcome;
import com.recoverai.backend.domain.enums.RecoveryCaseStatus;
import com.recoverai.backend.dto.ExecutionContext;
import com.recoverai.backend.dto.ExecutionResult;
import com.recoverai.backend.dto.RecoveryExecutionResponse;
import com.recoverai.backend.exception.InvalidRequestException;
import com.recoverai.backend.execution.DryRunRecoveryExecutionAdapter;
import com.recoverai.backend.policy.PolicyCheckDetails;
import com.recoverai.backend.policy.PolicyEvaluationResult;
import com.recoverai.backend.repository.*;
import com.recoverai.backend.service.RecoveryAnalysisService;
import com.recoverai.backend.service.RecoveryExecutionService;
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
class Phase8ExecutionAdapterTest {

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
    private RecoveryAttemptRepository recoveryAttemptRepository;

    @Autowired
    private RecoveryAnalysisService recoveryAnalysisService;

    @Autowired
    private RecoveryExecutionService recoveryExecutionService;

    @Autowired
    private DryRunRecoveryExecutionAdapter dryRunAdapter;

    private Merchant merchant;
    private Customer customer;
    private Payment payment;
    private RecoveryCase recoveryCase;
    private RecoveryPolicy policy;

    @BeforeEach
    void setUp() {
        merchant = new Merchant();
        merchant.setName("Execution Test Merchant");
        merchant = merchantRepository.save(merchant);

        customer = new Customer();
        customer.setMerchant(merchant);
        customer.setExternalCustomerId("cust_exec_100");
        customer.setEmail("exec100@example.com");
        customer = customerRepository.save(customer);

        payment = new Payment();
        payment.setMerchant(merchant);
        payment.setCustomer(customer);
        payment.setAmount(new BigDecimal("3500.00"));
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

        // Perform analysis first to create AgentDecision
        recoveryAnalysisService.analyzeRecoveryCase(recoveryCase.getId());
    }

    @Test
    @DisplayName("1. ACTION_ALLOWED triggers dry-run execution and creates RecoveryAttempt")
    void testActionAllowedTriggersExecution() {
        RecoveryExecutionResponse response = recoveryExecutionService.executeRecoveryAction(recoveryCase.getId(), "req_test_1");

        assertNotNull(response);
        assertTrue(response.isExecuted());
        assertEquals(PolicyDecisionOutcome.ACTION_ALLOWED, response.getPolicyDecision().getDecision());
        assertNotNull(response.getExecutionResult());
        assertTrue(response.getExecutionResult().isSimulated());

        // Verify RecoveryAttempt persisted in DB
        List<RecoveryAttempt> attempts = recoveryAttemptRepository.findByRecoveryCaseId(recoveryCase.getId());
        assertFalse(attempts.isEmpty());
        assertEquals(RecoveryAttemptOutcome.SUCCESS, attempts.get(0).getOutcome());

        // Verify Payment status remains FAILED (simulation does not falsely mark payment captured)
        Payment updatedPayment = paymentRepository.findById(payment.getId()).orElseThrow();
        assertEquals(PaymentStatus.FAILED, updatedPayment.getStatus());
    }

    @Test
    @DisplayName("2. Policy re-evaluation BLOCKED (e.g. retry limit reached) prevents execution")
    void testPolicyReEvaluationBlockedPreventsExecution() {
        // Change payment retry count to at limit (3) before execution
        payment.setRetryCount(3);
        paymentRepository.save(payment);

        RecoveryExecutionResponse response = recoveryExecutionService.executeRecoveryAction(recoveryCase.getId(), "req_test_2");

        assertFalse(response.isExecuted());
        assertNull(response.getExecutionResult());
        assertEquals(PolicyDecisionOutcome.ACTION_BLOCKED, response.getPolicyDecision().getDecision());
        assertEquals("MAX_RETRY_COUNT_REACHED", response.getPolicyDecision().getReason());

        // Verify blocked RecoveryAttempt persisted in DB
        List<RecoveryAttempt> attempts = recoveryAttemptRepository.findByRecoveryCaseId(recoveryCase.getId());
        assertFalse(attempts.isEmpty());
        assertEquals(RecoveryAttemptOutcome.BLOCKED, attempts.get(0).getOutcome());
    }

    @Test
    @DisplayName("3. Policy HUMAN_APPROVAL_REQUIRED prevents automated execution")
    void testPolicyHumanApprovalPreventsExecution() {
        policy.setAutoRecoveryEnabled(false);
        recoveryPolicyRepository.save(policy);

        RecoveryExecutionResponse response = recoveryExecutionService.executeRecoveryAction(recoveryCase.getId(), "req_test_3");

        assertFalse(response.isExecuted());
        assertNull(response.getExecutionResult());
        assertEquals(PolicyDecisionOutcome.HUMAN_APPROVAL_REQUIRED, response.getPolicyDecision().getDecision());
        assertEquals("AUTO_RECOVERY_DISABLED", response.getPolicyDecision().getReason());
    }

    @Test
    @DisplayName("4. DryRunAdapter directly rejects unauthorized calls (Security Guard)")
    void testAdapterRejectsUnauthorizedCall() {
        PolicyEvaluationResult blockedResult = new PolicyEvaluationResult(
                PolicyDecisionOutcome.ACTION_BLOCKED,
                RecoveryActionType.RETRY,
                "BLOCKED",
                new BigDecimal("0.80"),
                new BigDecimal("3500.00"),
                new PolicyCheckDetails()
        );

        ExecutionContext ctx = new ExecutionContext(recoveryCase.getId(), payment.getId(), merchant.getId(), "req_sec", "DRY_RUN");

        assertThrows(InvalidRequestException.class, () ->
                dryRunAdapter.execute(RecoveryActionType.RETRY, blockedResult, ctx)
        );
    }

    @Test
    @DisplayName("5. POST /api/v1/recovery/cases/{id}/execute endpoint executes authorized action")
    void testExecuteEndpointHttp() throws Exception {
        mockMvc.perform(post("/api/v1/recovery/cases/" + recoveryCase.getId() + "/execute")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.executed").value(true))
                .andExpect(jsonPath("$.data.policyDecision.decision").value("ACTION_ALLOWED"))
                .andExpect(jsonPath("$.data.executionResult.simulated").value(true));
    }

    @Test
    @DisplayName("6. Idempotency test: repeated execution when cooldown is 0 returns existing attempt result")
    void testIdempotentExecution() {
        // Set cooldown to 0 to test idempotency logic when policy passes
        policy.setCooldownMinutes(0);
        recoveryPolicyRepository.save(policy);

        RecoveryExecutionResponse response1 = recoveryExecutionService.executeRecoveryAction(recoveryCase.getId(), "req_idemp_1");
        assertTrue(response1.isExecuted());

        RecoveryExecutionResponse response2 = recoveryExecutionService.executeRecoveryAction(recoveryCase.getId(), "req_idemp_2");
        assertTrue(response2.isExecuted());
        assertEquals(response1.getAttemptId(), response2.getAttemptId());
    }
}
