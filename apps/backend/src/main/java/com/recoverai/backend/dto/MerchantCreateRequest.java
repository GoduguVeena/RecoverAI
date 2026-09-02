package com.recoverai.backend.dto;

import javax.validation.constraints.NotBlank;
import java.math.BigDecimal;

public class MerchantCreateRequest {

    @NotBlank(message = "Merchant name is required")
    private String name;

    private String currency = "INR";
    private Boolean autoRecoveryEnabled = true;
    private Integer maxRetryCount = 3;
    private BigDecimal minRecoveryProbability = new BigDecimal("0.6000");
    private BigDecimal automaticActionLimit = new BigDecimal("50000.0000");
    private BigDecimal humanApprovalThreshold = new BigDecimal("100000.0000");

    public MerchantCreateRequest() {
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public Boolean getAutoRecoveryEnabled() { return autoRecoveryEnabled; }
    public void setAutoRecoveryEnabled(Boolean autoRecoveryEnabled) { this.autoRecoveryEnabled = autoRecoveryEnabled; }

    public Integer getMaxRetryCount() { return maxRetryCount; }
    public void setMaxRetryCount(Integer maxRetryCount) { this.maxRetryCount = maxRetryCount; }

    public BigDecimal getMinRecoveryProbability() { return minRecoveryProbability; }
    public void setMinRecoveryProbability(BigDecimal minRecoveryProbability) { this.minRecoveryProbability = minRecoveryProbability; }

    public BigDecimal getAutomaticActionLimit() { return automaticActionLimit; }
    public void setAutomaticActionLimit(BigDecimal automaticActionLimit) { this.automaticActionLimit = automaticActionLimit; }

    public BigDecimal getHumanApprovalThreshold() { return humanApprovalThreshold; }
    public void setHumanApprovalThreshold(BigDecimal humanApprovalThreshold) { this.humanApprovalThreshold = humanApprovalThreshold; }
}
