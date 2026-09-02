package com.recoverai.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recoverai.backend.domain.entity.*;
import com.recoverai.backend.domain.enums.ActorType;
import com.recoverai.backend.domain.enums.PaymentStatus;
import com.recoverai.backend.domain.enums.RecoveryCaseStatus;
import com.recoverai.backend.dto.WebhookIngestionResponse;
import com.recoverai.backend.exception.InvalidRequestException;
import com.recoverai.backend.repository.*;
import com.recoverai.backend.webhook.WebhookSignatureVerifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class WebhookIngestionService {

    private static final Logger log = LoggerFactory.getLogger(WebhookIngestionService.class);

    private final WebhookEventRepository webhookEventRepository;
    private final MerchantRepository merchantRepository;
    private final CustomerRepository customerRepository;
    private final PaymentRepository paymentRepository;
    private final RecoveryCaseRepository recoveryCaseRepository;
    private final WebhookSignatureVerifier signatureVerifier;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;
    private final String webhookSecret;

    public WebhookIngestionService(WebhookEventRepository webhookEventRepository,
                                  MerchantRepository merchantRepository,
                                  CustomerRepository customerRepository,
                                  PaymentRepository paymentRepository,
                                  RecoveryCaseRepository recoveryCaseRepository,
                                  WebhookSignatureVerifier signatureVerifier,
                                  AuditLogService auditLogService,
                                  @Value("${razorpay.webhook.secret:${RAZORPAY_WEBHOOK_SECRET:}}") String webhookSecret) {
        this.webhookEventRepository = webhookEventRepository;
        this.merchantRepository = merchantRepository;
        this.customerRepository = customerRepository;
        this.paymentRepository = paymentRepository;
        this.recoveryCaseRepository = recoveryCaseRepository;
        this.signatureVerifier = signatureVerifier;
        this.auditLogService = auditLogService;
        this.objectMapper = new ObjectMapper();
        this.webhookSecret = webhookSecret;
    }

    @Transactional
    public WebhookIngestionResponse processWebhook(String rawPayload, String signatureHeader, UUID headerMerchantId, String requestId) {
        // 1. Secret & Signature Verification Guard
        if (webhookSecret == null || webhookSecret.trim().isEmpty()) {
            log.error("RAZORPAY_WEBHOOK_SECRET is missing in environment.");
            throw new InvalidRequestException("Webhook processing unavailable: secret unconfigured.");
        }

        if (signatureHeader == null || signatureHeader.trim().isEmpty()) {
            auditLogService.logEvent("WebhookEvent", UUID.randomUUID(), "WEBHOOK_SIGNATURE_INVALID", ActorType.SYSTEM, "WEBHOOK", "VERIFY_SIGNATURE", "Missing X-Razorpay-Signature header");
            throw new InvalidRequestException("Missing X-Razorpay-Signature header.");
        }

        if (!signatureVerifier.verifySignature(rawPayload, signatureHeader, webhookSecret)) {
            auditLogService.logEvent("WebhookEvent", UUID.randomUUID(), "WEBHOOK_SIGNATURE_INVALID", ActorType.SYSTEM, "WEBHOOK", "VERIFY_SIGNATURE", "Invalid HMAC-SHA256 signature");
            throw new InvalidRequestException("Invalid Razorpay webhook signature.");
        }

        // 2. Parse Raw Payload into JSON
        JsonNode root;
        try {
            root = objectMapper.readTree(rawPayload);
        } catch (Exception e) {
            throw new InvalidRequestException("Malformed JSON webhook payload.");
        }

        // 3. Extract Event ID & Type
        String eventType = root.path("event").asText(null);
        if (eventType == null || eventType.trim().isEmpty()) {
            throw new InvalidRequestException("Missing event type in webhook payload.");
        }

        String eventId = root.has("event_id") ? root.path("event_id").asText() :
                         (root.has("id") ? root.path("id").asText() : null);

        JsonNode paymentEntityNode = root.path("payload").path("payment").path("entity");
        String rzpPaymentId = paymentEntityNode.path("id").asText(null);

        if (eventId == null || eventId.trim().isEmpty()) {
            if (rzpPaymentId != null) {
                eventId = eventType + "_" + rzpPaymentId;
            } else {
                throw new InvalidRequestException("Missing event identifier in webhook payload.");
            }
        }

        // 4. Idempotency Check (Database Uniqueness)
        Optional<WebhookEvent> existingEvent = webhookEventRepository.findByRazorpayEventId(eventId);
        if (existingEvent.isPresent()) {
            auditLogService.logEvent("WebhookEvent", existingEvent.get().getId(), "WEBHOOK_DUPLICATE", ActorType.SYSTEM, "WEBHOOK", "PROCESS_WEBHOOK", "Duplicate event ID: " + eventId);
            return new WebhookIngestionResponse(eventId, eventType, true, "Duplicate webhook event already processed.");
        }

        // 5. Persist WebhookEvent Record
        WebhookEvent webhookEvent = new WebhookEvent();
        webhookEvent.setRazorpayEventId(eventId);
        webhookEvent.setEventType(eventType);
        webhookEvent.setPayload(rawPayload);
        webhookEvent.setSignatureValid(true);
        webhookEvent.setProcessed(false);

        try {
            webhookEvent = webhookEventRepository.save(webhookEvent);
        } catch (DataIntegrityViolationException e) {
            return new WebhookIngestionResponse(eventId, eventType, true, "Duplicate webhook event already processed.");
        }

        auditLogService.logEvent("WebhookEvent", webhookEvent.getId(), "WEBHOOK_RECEIVED", ActorType.SYSTEM, "WEBHOOK", "RECEIVE_WEBHOOK", "Received event type: " + eventType);

        // 6. Check Event Type Actionability
        if (!"payment.failed".equalsIgnoreCase(eventType)) {
            webhookEvent.setProcessed(true);
            webhookEvent.setProcessedAt(Instant.now());
            webhookEventRepository.save(webhookEvent);
            auditLogService.logEvent("WebhookEvent", webhookEvent.getId(), "WEBHOOK_IGNORED", ActorType.SYSTEM, "WEBHOOK", "PROCESS_WEBHOOK", "Ignored non-actionable event type: " + eventType);
            return new WebhookIngestionResponse(eventId, eventType, false, "Event type " + eventType + " ignored.");
        }

        // 7. Merchant Resolution
        Merchant merchant = resolveMerchant(headerMerchantId);
        if (merchant == null) {
            auditLogService.logEvent("WebhookEvent", webhookEvent.getId(), "WEBHOOK_PROCESSING_FAILED", ActorType.SYSTEM, "WEBHOOK", "RESOLVE_MERCHANT", "Merchant resolution failed for event: " + eventId);
            throw new InvalidRequestException("Merchant resolution failed. No valid merchant available.");
        }

        // 8. Extract Payment Details from Payload
        if (rzpPaymentId == null || rzpPaymentId.trim().isEmpty()) {
            throw new InvalidRequestException("Missing payment entity ID in payment.failed webhook.");
        }

        String orderId = paymentEntityNode.path("order_id").asText(null);
        long amountPaise = paymentEntityNode.path("amount").asLong(0);
        BigDecimal amountInINR = BigDecimal.valueOf(amountPaise).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        if (amountInINR.compareTo(BigDecimal.ZERO) <= 0) {
            amountInINR = new BigDecimal("2499.00"); // Default fallback if amount absent
        }

        String currency = paymentEntityNode.path("currency").asText("INR");
        String method = paymentEntityNode.path("method").asText("upi");
        String failureCode = paymentEntityNode.path("error_code").asText(
                paymentEntityNode.path("error_reason").asText("TRANSIENT_NETWORK_TIMEOUT")
        );
        String failureReason = paymentEntityNode.path("error_description").asText("Payment failed");

        String extCustomerId = paymentEntityNode.path("customer_id").asText(null);
        String email = paymentEntityNode.path("email").asText(null);
        String phone = paymentEntityNode.path("contact").asText(null);

        // 9. Customer Resolution
        Customer customer = resolveCustomer(merchant, extCustomerId, email, phone, rzpPaymentId);

        // 10. Payment Ingestion
        Optional<Payment> existingPaymentOpt = paymentRepository.findByRazorpayPaymentId(rzpPaymentId);
        Payment payment;
        if (existingPaymentOpt.isPresent()) {
            payment = existingPaymentOpt.get();
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureCode(failureCode);
            payment.setFailureReason(failureReason);
            payment = paymentRepository.save(payment);
        } else {
            payment = new Payment();
            payment.setMerchant(merchant);
            payment.setCustomer(customer);
            payment.setRazorpayPaymentId(rzpPaymentId);
            payment.setRazorpayOrderId(orderId);
            payment.setAmount(amountInINR);
            payment.setCurrency(currency);
            payment.setStatus(PaymentStatus.FAILED);
            payment.setMethod(method);
            payment.setFailureCode(failureCode);
            payment.setFailureReason(failureReason);
            payment.setRetryCount(0);
            payment = paymentRepository.save(payment);
            auditLogService.logEvent("Payment", payment.getId(), "PAYMENT_CREATED_FROM_WEBHOOK", ActorType.SYSTEM, "WEBHOOK", "INGEST_PAYMENT", "Payment record created from webhook");
        }

        // 11. RecoveryCase Creation (Status: OPEN)
        Optional<RecoveryCase> existingCaseOpt = recoveryCaseRepository.findByPaymentId(payment.getId());
        RecoveryCase recoveryCase;
        if (existingCaseOpt.isPresent()) {
            recoveryCase = existingCaseOpt.get();
        } else {
            recoveryCase = new RecoveryCase();
            recoveryCase.setPayment(payment);
            recoveryCase.setStatus(RecoveryCaseStatus.OPEN);
            recoveryCase = recoveryCaseRepository.save(recoveryCase);
            auditLogService.logEvent("RecoveryCase", recoveryCase.getId(), "RECOVERY_CASE_CREATED", ActorType.SYSTEM, "WEBHOOK", "CREATE_CASE", "RecoveryCase opened from payment.failed event");
        }

        // 12. Complete Webhook Event Processing
        webhookEvent.setProcessed(true);
        webhookEvent.setProcessedAt(Instant.now());
        webhookEventRepository.save(webhookEvent);

        auditLogService.logEvent("WebhookEvent", webhookEvent.getId(), "WEBHOOK_PROCESSED", ActorType.SYSTEM, "WEBHOOK", "PROCESS_WEBHOOK", "Successfully ingested payment.failed event for payment " + rzpPaymentId);

        return new WebhookIngestionResponse(eventId, eventType, true, "Webhook payment.failed event ingested. RecoveryCase OPENED.");
    }

    private Merchant resolveMerchant(UUID headerMerchantId) {
        if (headerMerchantId != null) {
            return merchantRepository.findById(headerMerchantId).orElse(null);
        }
        List<Merchant> merchants = merchantRepository.findAll();
        if (!merchants.isEmpty()) {
            return merchants.get(0);
        }
        return null;
    }

    private Customer resolveCustomer(Merchant merchant, String extCustomerId, String email, String phone, String rzpPaymentId) {
        String effectiveExtId = (extCustomerId != null && !extCustomerId.trim().isEmpty()) ? extCustomerId :
                ((email != null && !email.trim().isEmpty()) ? email : "cust_" + rzpPaymentId);

        Optional<Customer> existingCust = customerRepository.findByMerchantIdAndExternalCustomerId(merchant.getId(), effectiveExtId);
        if (existingCust.isPresent()) {
            return existingCust.get();
        }

        Customer customer = new Customer();
        customer.setMerchant(merchant);
        customer.setExternalCustomerId(effectiveExtId);
        customer.setEmail(email != null ? email : "customer@example.com");
        customer.setPhone(phone);
        return customerRepository.save(customer);
    }
}
