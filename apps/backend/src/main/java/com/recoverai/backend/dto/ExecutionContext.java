package com.recoverai.backend.dto;

import java.util.UUID;

public class ExecutionContext {

    private UUID recoveryCaseId;
    private UUID paymentId;
    private UUID merchantId;
    private String requestId;
    private String executionMode;

    public ExecutionContext() {
        this.executionMode = "DRY_RUN";
    }

    public ExecutionContext(UUID recoveryCaseId, UUID paymentId, UUID merchantId, String requestId, String executionMode) {
        this.recoveryCaseId = recoveryCaseId;
        this.paymentId = paymentId;
        this.merchantId = merchantId;
        this.requestId = requestId;
        this.executionMode = executionMode != null ? executionMode : "DRY_RUN";
    }

    public UUID getRecoveryCaseId() { return recoveryCaseId; }
    public void setRecoveryCaseId(UUID recoveryCaseId) { this.recoveryCaseId = recoveryCaseId; }

    public UUID getPaymentId() { return paymentId; }
    public void setPaymentId(UUID paymentId) { this.paymentId = paymentId; }

    public UUID getMerchantId() { return merchantId; }
    public void setMerchantId(UUID merchantId) { this.merchantId = merchantId; }

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

    public String getExecutionMode() { return executionMode; }
    public void setExecutionMode(String executionMode) { this.executionMode = executionMode; }
}
