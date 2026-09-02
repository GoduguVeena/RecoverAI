package com.recoverai.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public class MlPredictionRequest {

    @JsonProperty("merchant_id")
    private String merchantId;

    @JsonProperty("customer_id")
    private String customerId;

    private BigDecimal amount;
    private String currency;

    @JsonProperty("payment_method")
    private String paymentMethod;

    @JsonProperty("failure_type")
    private String failureType;

    @JsonProperty("retry_count")
    private Integer retryCount;

    @JsonProperty("customer_total_transactions")
    private Integer customerTotalTransactions;

    @JsonProperty("customer_successful_transactions")
    private Integer customerSuccessfulTransactions;

    @JsonProperty("customer_failed_transactions")
    private Integer customerFailedTransactions;

    @JsonProperty("customer_success_rate")
    private BigDecimal customerSuccessRate;

    @JsonProperty("customer_total_spend")
    private BigDecimal customerTotalSpend;

    @JsonProperty("days_since_last_success")
    private BigDecimal daysSinceLastSuccess;

    @JsonProperty("checkout_duration_seconds")
    private BigDecimal checkoutDurationSeconds;

    @JsonProperty("hour_of_day")
    private Integer hourOfDay;

    @JsonProperty("day_of_week")
    private Integer dayOfWeek;

    @JsonProperty("merchant_category")
    private String merchantCategory;

    @JsonProperty("customer_segment")
    private String customerSegment;

    public MlPredictionRequest() {
    }

    public String getMerchantId() { return merchantId; }
    public void setMerchantId(String merchantId) { this.merchantId = merchantId; }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getFailureType() { return failureType; }
    public void setFailureType(String failureType) { this.failureType = failureType; }

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

    public BigDecimal getCustomerTotalSpend() { return customerTotalSpend; }
    public void setCustomerTotalSpend(BigDecimal customerTotalSpend) { this.customerTotalSpend = customerTotalSpend; }

    public BigDecimal getDaysSinceLastSuccess() { return daysSinceLastSuccess; }
    public void setDaysSinceLastSuccess(BigDecimal daysSinceLastSuccess) { this.daysSinceLastSuccess = daysSinceLastSuccess; }

    public BigDecimal getCheckoutDurationSeconds() { return checkoutDurationSeconds; }
    public void setCheckoutDurationSeconds(BigDecimal checkoutDurationSeconds) { this.checkoutDurationSeconds = checkoutDurationSeconds; }

    public Integer getHourOfDay() { return hourOfDay; }
    public void setHourOfDay(Integer hourOfDay) { this.hourOfDay = hourOfDay; }

    public Integer getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(Integer dayOfWeek) { this.dayOfWeek = dayOfWeek; }

    public String getMerchantCategory() { return merchantCategory; }
    public void setMerchantCategory(String merchantCategory) { this.merchantCategory = merchantCategory; }

    public String getCustomerSegment() { return customerSegment; }
    public void setCustomerSegment(String customerSegment) { this.customerSegment = customerSegment; }
}
