package com.recoverai.backend.dto;

import java.math.BigDecimal;
import java.util.UUID;

public class AgentRequestContext {

    private UUID recoveryCaseId;
    private UUID paymentId;
    private BigDecimal amount;
    private String currency;
    private String paymentMethod;
    private String failureCode;
    private String failureReason;
    private Integer retryCount;
    private Integer customerTotalTransactions;
    private Integer customerSuccessfulTransactions;
    private Integer customerFailedTransactions;
    private BigDecimal customerSuccessRate;
    private BigDecimal daysSinceLastSuccess;
    private String merchantCategory;
    private String customerSegment;
    private BigDecimal recoveryProbability;
    private String modelVersion;

    public AgentRequestContext() {
    }

    public UUID getRecoveryCaseId() { return recoveryCaseId; }
    public void setRecoveryCaseId(UUID recoveryCaseId) { this.recoveryCaseId = recoveryCaseId; }

    public UUID getPaymentId() { return paymentId; }
    public void setPaymentId(UUID paymentId) { this.paymentId = paymentId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getFailureCode() { return failureCode; }
    public void setFailureCode(String failureCode) { this.failureCode = failureCode; }

    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }

    public Integer getRetryCount() { return retryCount; }
    public void setRetryCount(Integer retryCount) { this.retryCount = retryCount; }

    public Integer getCustomerTotalTransactions() { return customerTotalTransactions; }
    public void setCustomerTotalTransactions(Integer customerTotalTransactions) { this.customerTotalTransactions = customerTotalTransactions; }

    public Integer getCustomerSuccessfulTransactions() { return customerSuccessfulTransactions; }
    public void setCustomerSuccessfulTransactions(Integer customerSuccessfulTransactions) { this.customerSuccessfulTransactions = customerSuccessfulTransactions; }

    public Integer getCustomerFailedTransactions() { return customerFailedTransactions; }
    public void setCustomerFailedTransactions(Integer customerFailedTransactions) { this.customerFailedTransactions = customerFailedTransactions; }

    public BigDecimal getCustomerSuccessRate() { return customerSuccessRate; }
    public void setCustomerSuccessRate(BigDecimal customerSuccessRate) { this.customerSuccessRate = customerSuccessRate; }

    public BigDecimal getDaysSinceLastSuccess() { return daysSinceLastSuccess; }
    public void setDaysSinceLastSuccess(BigDecimal daysSinceLastSuccess) { this.daysSinceLastSuccess = daysSinceLastSuccess; }

    public String getMerchantCategory() { return merchantCategory; }
    public void setMerchantCategory(String merchantCategory) { this.merchantCategory = merchantCategory; }

    public String getCustomerSegment() { return customerSegment; }
    public void setCustomerSegment(String customerSegment) { this.customerSegment = customerSegment; }

    public BigDecimal getRecoveryProbability() { return recoveryProbability; }
    public void setRecoveryProbability(BigDecimal recoveryProbability) { this.recoveryProbability = recoveryProbability; }

    public String getModelVersion() { return modelVersion; }
    public void setModelVersion(String modelVersion) { this.modelVersion = modelVersion; }
}
