package com.recoverai.backend.domain.entity;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "merchants")
public class Merchant {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 10)
    private String currency = "INR";

    @Column(name = "auto_recovery_enabled", nullable = false)
    private Boolean autoRecoveryEnabled = true;

    @Column(name = "max_retry_count", nullable = false)
    private Integer maxRetryCount = 3;

    @Column(name = "min_recovery_probability", nullable = false, precision = 5, scale = 4)
    private BigDecimal minRecoveryProbability = new BigDecimal("0.6000");

    @Column(name = "automatic_action_limit", nullable = false, precision = 19, scale = 4)
    private BigDecimal automaticActionLimit = new BigDecimal("50000.0000");

    @Column(name = "human_approval_threshold", nullable = false, precision = 19, scale = 4)
    private BigDecimal humanApprovalThreshold = new BigDecimal("100000.0000");

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "merchant", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Customer> customers = new ArrayList<>();

    @OneToMany(mappedBy = "merchant", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Payment> payments = new ArrayList<>();

    @OneToOne(mappedBy = "merchant", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private RecoveryPolicy recoveryPolicy;

    public Merchant() {
    }

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
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

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public List<Customer> getCustomers() { return customers; }
    public void setCustomers(List<Customer> customers) { this.customers = customers; }

    public List<Payment> getPayments() { return payments; }
    public void setPayments(List<Payment> payments) { this.payments = payments; }

    public RecoveryPolicy getRecoveryPolicy() { return recoveryPolicy; }
    public void setRecoveryPolicy(RecoveryPolicy recoveryPolicy) { this.recoveryPolicy = recoveryPolicy; }
}
