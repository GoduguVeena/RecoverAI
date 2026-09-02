package com.recoverai.backend.dto;

import com.recoverai.backend.domain.entity.Customer;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class CustomerResponse {

    private UUID id;
    private UUID merchantId;
    private String externalCustomerId;
    private String name;
    private String email;
    private String phone;
    private Integer totalTransactions;
    private Integer successfulTransactions;
    private Integer failedTransactions;
    private BigDecimal totalSpend;
    private Instant createdAt;

    public CustomerResponse() {
    }

    public static CustomerResponse from(Customer customer) {
        CustomerResponse dto = new CustomerResponse();
        dto.setId(customer.getId());
        dto.setMerchantId(customer.getMerchant().getId());
        dto.setExternalCustomerId(customer.getExternalCustomerId());
        dto.setName(customer.getName());
        dto.setEmail(customer.getEmail());
        dto.setPhone(customer.getPhone());
        dto.setTotalTransactions(customer.getTotalTransactions());
        dto.setSuccessfulTransactions(customer.getSuccessfulTransactions());
        dto.setFailedTransactions(customer.getFailedTransactions());
        dto.setTotalSpend(customer.getTotalSpend());
        dto.setCreatedAt(customer.getCreatedAt());
        return dto;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getMerchantId() { return merchantId; }
    public void setMerchantId(UUID merchantId) { this.merchantId = merchantId; }

    public String getExternalCustomerId() { return externalCustomerId; }
    public void setExternalCustomerId(String externalCustomerId) { this.externalCustomerId = externalCustomerId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public Integer getTotalTransactions() { return totalTransactions; }
    public void setTotalTransactions(Integer totalTransactions) { this.totalTransactions = totalTransactions; }

    public Integer getSuccessfulTransactions() { return successfulTransactions; }
    public void setSuccessfulTransactions(Integer successfulTransactions) { this.successfulTransactions = successfulTransactions; }

    public Integer getFailedTransactions() { return failedTransactions; }
    public void setFailedTransactions(Integer failedTransactions) { this.failedTransactions = failedTransactions; }

    public BigDecimal getTotalSpend() { return totalSpend; }
    public void setTotalSpend(BigDecimal totalSpend) { this.totalSpend = totalSpend; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
