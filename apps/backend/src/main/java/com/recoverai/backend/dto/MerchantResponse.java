package com.recoverai.backend.dto;

import com.recoverai.backend.domain.entity.Merchant;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class MerchantResponse {

    private UUID id;
    private String name;
    private String currency;
    private Boolean autoRecoveryEnabled;
    private Integer maxRetryCount;
    private BigDecimal minRecoveryProbability;
    private BigDecimal automaticActionLimit;
    private BigDecimal humanApprovalThreshold;
    private Instant createdAt;

    public MerchantResponse() {
    }

    public static MerchantResponse from(Merchant merchant) {
        MerchantResponse dto = new MerchantResponse();
        dto.setId(merchant.getId());
        dto.setName(merchant.getName());
        dto.setCurrency(merchant.getCurrency());
        dto.setAutoRecoveryEnabled(merchant.getAutoRecoveryEnabled());
        dto.setMaxRetryCount(merchant.getMaxRetryCount());
        dto.setMinRecoveryProbability(merchant.getMinRecoveryProbability());
        dto.setAutomaticActionLimit(merchant.getAutomaticActionLimit());
        dto.setHumanApprovalThreshold(merchant.getHumanApprovalThreshold());
        dto.setCreatedAt(merchant.getCreatedAt());
        return dto;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

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

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
