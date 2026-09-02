package com.recoverai.backend.dto;

import javax.validation.constraints.NotNull;
import java.util.UUID;

public class RecoveryCaseCreateRequest {

    @NotNull(message = "merchantId is required")
    private UUID merchantId;

    @NotNull(message = "paymentId is required")
    private UUID paymentId;

    public RecoveryCaseCreateRequest() {
    }

    public UUID getMerchantId() { return merchantId; }
    public void setMerchantId(UUID merchantId) { this.merchantId = merchantId; }

    public UUID getPaymentId() { return paymentId; }
    public void setPaymentId(UUID paymentId) { this.paymentId = paymentId; }
}
