package com.recoverai.backend;

import com.recoverai.backend.domain.entity.*;
import com.recoverai.backend.domain.enums.PaymentStatus;
import com.recoverai.backend.domain.enums.RecoveryCaseStatus;
import com.recoverai.backend.dto.WebhookIngestionResponse;
import com.recoverai.backend.repository.*;
import com.recoverai.backend.service.WebhookIngestionService;
import com.recoverai.backend.webhook.WebhookSignatureVerifier;
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

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class Phase9WebhookIngestionTest {

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
    private WebhookEventRepository webhookEventRepository;

    @Autowired
    private AgentDecisionRepository agentDecisionRepository;

    @Autowired
    private RecoveryAttemptRepository recoveryAttemptRepository;

    @Autowired
    private WebhookIngestionService webhookIngestionService;

    @Autowired
    private WebhookSignatureVerifier verifier;

    private final String testSecret = "test_webhook_secret_key_12345";
    private Merchant merchant;

    @BeforeEach
    void setUp() {
        merchant = new Merchant();
        merchant.setName("Webhook Test Merchant");
        merchant = merchantRepository.save(merchant);
    }

    private String samplePaymentFailedPayload(String eventId, String paymentId) {
        return "{\n" +
                "  \"entity\": \"event\",\n" +
                "  \"event_id\": \"" + eventId + "\",\n" +
                "  \"event\": \"payment.failed\",\n" +
                "  \"payload\": {\n" +
                "    \"payment\": {\n" +
                "      \"entity\": {\n" +
                "        \"id\": \"" + paymentId + "\",\n" +
                "        \"amount\": 249900,\n" +
                "        \"currency\": \"INR\",\n" +
                "        \"status\": \"failed\",\n" +
                "        \"method\": \"upi\",\n" +
                "        \"error_code\": \"BAD_REQUEST_ERROR\",\n" +
                "        \"error_description\": \"Bank timeout error\",\n" +
                "        \"customer_id\": \"cust_wb_999\",\n" +
                "        \"email\": \"customer999@example.com\"\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}";
    }

    @Test
    @DisplayName("1. Valid payment.failed webhook creates Payment, RecoveryCase in OPEN status & WebhookEvent record")
    void testValidPaymentFailedWebhook() {
        String payload = samplePaymentFailedPayload("evt_test_001", "pay_test_001");
        String signature = verifier.calculateSignature(payload, testSecret);

        WebhookIngestionResponse response = webhookIngestionService.processWebhook(payload, signature, merchant.getId(), "req_wb_1");

        assertNotNull(response);
        assertTrue(response.isProcessed());
        assertEquals("evt_test_001", response.getEventId());

        // Verify WebhookEvent in DB
        Optional<WebhookEvent> eventOpt = webhookEventRepository.findByRazorpayEventId("evt_test_001");
        assertTrue(eventOpt.isPresent());
        assertTrue(eventOpt.get().getSignatureValid());
        assertTrue(eventOpt.get().getProcessed());

        // Verify Payment created
        Optional<Payment> paymentOpt = paymentRepository.findByRazorpayPaymentId("pay_test_001");
        assertTrue(paymentOpt.isPresent());
        assertEquals(PaymentStatus.FAILED, paymentOpt.get().getStatus());

        // Verify RecoveryCase created in OPEN status
        Optional<RecoveryCase> caseOpt = recoveryCaseRepository.findByPaymentId(paymentOpt.get().getId());
        assertTrue(caseOpt.isPresent());
        assertEquals(RecoveryCaseStatus.OPEN, caseOpt.get().getStatus());

        // Safety verification: No AgentDecision or RecoveryAttempt created automatically!
        List<AgentDecision> decisions = agentDecisionRepository.findByRecoveryCaseId(caseOpt.get().getId());
        assertTrue(decisions.isEmpty(), "Webhook processing MUST NOT invoke AI agent or create decisions!");

        List<RecoveryAttempt> attempts = recoveryAttemptRepository.findByRecoveryCaseId(caseOpt.get().getId());
        assertTrue(attempts.isEmpty(), "Webhook processing MUST NOT execute recovery attempts!");
    }

    @Test
    @DisplayName("2. Duplicate webhook payload returns idempotent response without creating duplicate entities")
    void testIdempotentWebhookDelivery() {
        String payload = samplePaymentFailedPayload("evt_test_002", "pay_test_002");
        String signature = verifier.calculateSignature(payload, testSecret);

        WebhookIngestionResponse r1 = webhookIngestionService.processWebhook(payload, signature, merchant.getId(), "req_wb_2a");
        assertTrue(r1.isProcessed());

        WebhookIngestionResponse r2 = webhookIngestionService.processWebhook(payload, signature, merchant.getId(), "req_wb_2b");
        assertTrue(r2.isProcessed());
        assertTrue(r2.getMessage().contains("Duplicate"));

        // Verify only 1 Payment and 1 RecoveryCase created
        List<Payment> payments = paymentRepository.findAll();
        long count = payments.stream().filter(p -> "pay_test_002".equals(p.getRazorpayPaymentId())).count();
        assertEquals(1, count);
    }

    @Test
    @DisplayName("3. Unsupported signed webhook event (e.g. refund.created) is persisted/ignored without creating RecoveryCase")
    void testUnsupportedSignedEventIgnored() {
        String payload = "{\n" +
                "  \"event_id\": \"evt_refund_100\",\n" +
                "  \"event\": \"refund.created\",\n" +
                "  \"payload\": {\"refund\": {\"entity\": {\"id\": \"rfnd_100\"}}}\n" +
                "}";
        String signature = verifier.calculateSignature(payload, testSecret);

        WebhookIngestionResponse response = webhookIngestionService.processWebhook(payload, signature, merchant.getId(), "req_wb_3");

        assertTrue(response.getEventId().equals("evt_refund_100"));
        assertFalse(response.isProcessed()); // Ignored

        // Verify WebhookEvent stored
        Optional<WebhookEvent> eventOpt = webhookEventRepository.findByRazorpayEventId("evt_refund_100");
        assertTrue(eventOpt.isPresent());
        assertTrue(eventOpt.get().getSignatureValid());
    }

    @Test
    @DisplayName("4. POST /api/v1/webhooks/razorpay endpoint processes valid webhook over HTTP")
    void testWebhookEndpointHttp() throws Exception {
        String payload = samplePaymentFailedPayload("evt_http_300", "pay_http_300");
        String signature = verifier.calculateSignature(payload, testSecret);

        mockMvc.perform(post("/api/v1/webhooks/razorpay")
                        .header("X-Razorpay-Signature", signature)
                        .header("X-Merchant-ID", merchant.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.eventId").value("evt_http_300"))
                .andExpect(jsonPath("$.data.processed").value(true));
    }

    @Test
    @DisplayName("5. Invalid signature returns 400 BAD_REQUEST / fails closed")
    void testInvalidSignatureFailsClosed() throws Exception {
        String payload = samplePaymentFailedPayload("evt_bad_sig", "pay_bad_sig");
        String invalidSignature = "0000000000000000000000000000000000000000000000000000000000000000";

        mockMvc.perform(post("/api/v1/webhooks/razorpay")
                        .header("X-Razorpay-Signature", invalidSignature)
                        .header("X-Merchant-ID", merchant.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
    }
}
