package com.recoverai.backend.dto;

import com.recoverai.backend.domain.entity.Payment;
import com.recoverai.backend.domain.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class PaymentResponse {

    private UUID id;
    private UUID merchantId;
    private UUID customerId;
    private String razorpayPaymentId;
    private String razorpayOrderId;
    private BigDecimal amount;
    private String currency;
    private PaymentStatus status;
    private String method;
    private String failureCode;
    private String failureReason;
    private Integer retryCount;
    private Instant createdAt;

    public PaymentResponse() {
    }

    public static PaymentResponse from(Payment payment) {
        PaymentResponse dto = new PaymentResponse();
        dto.setId(payment.getId());
        dto.setMerchantId(payment.getMerchant().getId());
        dto.setCustomerId(payment.getCustomer().getId());
        dto.setRazorpayPaymentId(payment.getRazorpayPaymentId());
        dto.setRazorpayOrderId(payment.getRazorpayOrderId());
        dto.setAmount(payment.getAmount());
        dto.setCurrency(payment.getCurrency());
        dto.setStatus(payment.getStatus());
        dto.setMethod(payment.getMethod());
        dto.setFailureCode(payment.getFailureCode());
        dto.setFailureReason(payment.getFailureReason());
        dto.setRetryCount(payment.getRetryCount());
        dto.setCreatedAt(payment.getCreatedAt());
        return dto;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getMerchantId() { return merchantId; }
    public void setMerchantId(UUID merchantId) { this.merchantId = merchantId; }

    public UUID getCustomerId() { return customerId; }
    public void setCustomerId(UUID customerId) { this.customerId = customerId; }

    public String getRazorpayPaymentId() { return razorpayPaymentId; }
    public void setRazorpayPaymentId(String razorpayPaymentId) { this.razorpayPaymentId = razorpayPaymentId; }

    public String getRazorpayOrderId() { return razorpayOrderId; }
    public void setRazorpayOrderId(String razorpayOrderId) { this.razorpayOrderId = razorpayOrderId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public PaymentStatus getStatus() { return status; }
    public void setStatus(PaymentStatus status) { this.status = status; }

    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }

    public String getFailureCode() { return failureCode; }
    public void setFailureCode(String failureCode) { this.failureCode = failureCode; }

    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }

    public Integer getRetryCount() { return retryCount; }
    public void setRetryCount(Integer retryCount) { this.retryCount = retryCount; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
