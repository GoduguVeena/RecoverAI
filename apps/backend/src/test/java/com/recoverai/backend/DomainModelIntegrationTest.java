package com.recoverai.backend;

import com.recoverai.backend.domain.entity.*;
import com.recoverai.backend.domain.enums.*;
import com.recoverai.backend.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class DomainModelIntegrationTest {

    @Autowired
    private MerchantRepository merchantRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private RecoveryCaseRepository recoveryCaseRepository;

    @Autowired
    private RecoveryAttemptRepository recoveryAttemptRepository;

    @Autowired
    private AgentDecisionRepository agentDecisionRepository;

    @Autowired
    private RecoveryPolicyRepository recoveryPolicyRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private WebhookEventRepository webhookEventRepository;

    @Test
    @Transactional
    public void testEntityPersistenceAndRelationships() {
        // 1. Persist Merchant
        Merchant merchant = new Merchant();
        merchant.setName("Razorpay Test Merchant");
        merchant.setCurrency("INR");
        merchant = merchantRepository.save(merchant);
        assertNotNull(merchant.getId());

        // 2. Persist Customer linked to Merchant
        Customer customer = new Customer();
        customer.setMerchant(merchant);
        customer.setExternalCustomerId("cust_ext_1001");
        customer.setName("John Doe");
        customer.setEmail("john.doe@example.com");
        customer = customerRepository.save(customer);
        assertNotNull(customer.getId());
        assertEquals(merchant.getId(), customer.getMerchant().getId());

        // 3. Persist Payment linked to Merchant & Customer
        BigDecimal paymentAmount = new BigDecimal("12500.7500");
        Payment payment = new Payment();
        payment.setMerchant(merchant);
        payment.setCustomer(customer);
        payment.setAmount(paymentAmount);
        payment.setStatus(PaymentStatus.FAILED);
        payment.setFailureCode("BAD_REQUEST_PAYMENT_FAILED");
        payment.setFailureReason("Insufficient funds in customer account");
        payment = paymentRepository.save(payment);
        assertNotNull(payment.getId());
        assertEquals(paymentAmount, payment.getAmount());

        // 4. Persist RecoveryCase linked to Payment
        RecoveryCase recoveryCase = new RecoveryCase();
        recoveryCase.setPayment(payment);
        recoveryCase.setStatus(RecoveryCaseStatus.OPEN);
        recoveryCase.setRecoveryProbability(new BigDecimal("0.8500"));
        recoveryCase.setDiagnosis("TRANSIENT_FUNDS_FAILURE");
        recoveryCase.setRecommendedAction(RecoveryActionType.RETRY);
        recoveryCase = recoveryCaseRepository.save(recoveryCase);
        assertNotNull(recoveryCase.getId());
        assertEquals(payment.getId(), recoveryCase.getPayment().getId());

        // 5. Persist RecoveryAttempt linked to RecoveryCase
        RecoveryAttempt attempt = new RecoveryAttempt();
        attempt.setRecoveryCase(recoveryCase);
        attempt.setActionType(RecoveryActionType.RETRY);
        attempt.setActionPayload("{\"attempt\": 1}");
        attempt.setOutcome(RecoveryAttemptOutcome.PENDING);
        attempt = recoveryAttemptRepository.save(attempt);
        assertNotNull(attempt.getId());

        // 6. Persist AgentDecision linked to RecoveryCase
        AgentDecision decision = new AgentDecision();
        decision.setRecoveryCase(recoveryCase);
        decision.setModelVersion("v1.0.0");
        decision.setModelProbability(new BigDecimal("0.8500"));
        decision.setDiagnosis("TRANSIENT_FUNDS_FAILURE");
        decision.setSelectedAction(RecoveryActionType.RETRY);
        decision.setReasoningSummary("High recovery probability detected. Payment retry recommended.");
        decision = agentDecisionRepository.save(decision);
        assertNotNull(decision.getId());

        // 7. Persist RecoveryPolicy linked to Merchant
        RecoveryPolicy policy = new RecoveryPolicy();
        policy.setMerchant(merchant);
        policy.setMaxRetryCount(3);
        policy.setMinRecoveryProbability(new BigDecimal("0.6000"));
        policy = recoveryPolicyRepository.save(policy);
        assertNotNull(policy.getId());

        // 8. Persist AuditLog
        AuditLog auditLog = new AuditLog();
        auditLog.setEntityType("RecoveryCase");
        auditLog.setEntityId(recoveryCase.getId());
        auditLog.setEventType("CASE_CREATED");
        auditLog.setActorType(ActorType.SYSTEM);
        auditLog.setAction("CREATE_RECOVERY_CASE");
        auditLog.setReason("Payment failed triggering automatic recovery workflow");
        auditLog = auditLogRepository.save(auditLog);
        assertNotNull(auditLog.getId());

        // 9. Persist WebhookEvent
        WebhookEvent webhookEvent = new WebhookEvent();
        webhookEvent.setRazorpayEventId("evt_test_998877");
        webhookEvent.setEventType("payment.failed");
        webhookEvent.setSignatureValid(true);
        webhookEvent = webhookEventRepository.save(webhookEvent);
        assertNotNull(webhookEvent.getId());
    }

    @Test
    public void testCustomerMerchantUniqueConstraint() {
        Merchant merchant = new Merchant();
        merchant.setName("Constraint Test Merchant");
        merchant = merchantRepository.save(merchant);

        Customer c1 = new Customer();
        c1.setMerchant(merchant);
        c1.setExternalCustomerId("EXT_DUP_001");
        customerRepository.saveAndFlush(c1);

        Customer c2 = new Customer();
        c2.setMerchant(merchant);
        c2.setExternalCustomerId("EXT_DUP_001");

        assertThrows(DataIntegrityViolationException.class, () -> {
            customerRepository.saveAndFlush(c2);
        });
    }

    @Test
    public void testRazorpayEventIdUniqueConstraint() {
        WebhookEvent e1 = new WebhookEvent();
        e1.setRazorpayEventId("evt_unique_123");
        e1.setEventType("payment.failed");
        webhookEventRepository.saveAndFlush(e1);

        WebhookEvent e2 = new WebhookEvent();
        e2.setRazorpayEventId("evt_unique_123");
        e2.setEventType("payment.failed");

        assertThrows(DataIntegrityViolationException.class, () -> {
            webhookEventRepository.saveAndFlush(e2);
        });
    }

    @Test
    public void testMonetaryPrecision() {
        Merchant merchant = new Merchant();
        merchant.setName("Precision Test Merchant");
        merchant = merchantRepository.save(merchant);

        Customer customer = new Customer();
        customer.setMerchant(merchant);
        customer.setExternalCustomerId("cust_precision_1");
        customer = customerRepository.save(customer);

        BigDecimal exactAmount = new BigDecimal("99999999.9999");
        Payment payment = new Payment();
        payment.setMerchant(merchant);
        payment.setCustomer(customer);
        payment.setAmount(exactAmount);
        payment.setStatus(PaymentStatus.PENDING);
        payment = paymentRepository.saveAndFlush(payment);

        Optional<Payment> fetched = paymentRepository.findById(payment.getId());
        assertTrue(fetched.isPresent());
        assertEquals(0, exactAmount.compareTo(fetched.get().getAmount()));
    }
}
