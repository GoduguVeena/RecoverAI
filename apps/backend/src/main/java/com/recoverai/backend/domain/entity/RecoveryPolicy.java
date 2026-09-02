package com.recoverai.backend.domain.entity;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "recovery_policies")
public class RecoveryPolicy {

    @Id
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "merchant_id", nullable = false, unique = true)
    private Merchant merchant;

    @Column(name = "max_retry_count", nullable = false)
    private Integer maxRetryCount = 3;

    @Column(name = "min_recovery_probability", nullable = false, precision = 5, scale = 4)
    private BigDecimal minRecoveryProbability = new BigDecimal("0.6000");

    @Column(name = "automatic_action_limit", nullable = false, precision = 19, scale = 4)
    private BigDecimal automaticActionLimit = new BigDecimal("50000.0000");

    @Column(name = "human_approval_threshold", nullable = false, precision = 19, scale = 4)
    private BigDecimal humanApprovalThreshold = new BigDecimal("100000.0000");

    @Column(name = "cooldown_minutes", nullable = false)
    private Integer cooldownMinutes = 60;

    @Column(name = "auto_recovery_enabled", nullable = false)
    private Boolean autoRecoveryEnabled = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public RecoveryPolicy() {
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

    public Merchant getMerchant() { return merchant; }
    public void setMerchant(Merchant merchant) { this.merchant = merchant; }

    public Integer getMaxRetryCount() { return maxRetryCount; }
    public void setMaxRetryCount(Integer maxRetryCount) { this.maxRetryCount = maxRetryCount; }

    public BigDecimal getMinRecoveryProbability() { return minRecoveryProbability; }
    public void setMinRecoveryProbability(BigDecimal minRecoveryProbability) { this.minRecoveryProbability = minRecoveryProbability; }

    public BigDecimal getAutomaticActionLimit() { return automaticActionLimit; }
    public void setAutomaticActionLimit(BigDecimal automaticActionLimit) { this.automaticActionLimit = automaticActionLimit; }

    public BigDecimal getHumanApprovalThreshold() { return humanApprovalThreshold; }
    public void setHumanApprovalThreshold(BigDecimal humanApprovalThreshold) { this.humanApprovalThreshold = humanApprovalThreshold; }

    public Integer getCooldownMinutes() { return cooldownMinutes; }
    public void setCooldownMinutes(Integer cooldownMinutes) { this.cooldownMinutes = cooldownMinutes; }

    public Boolean getAutoRecoveryEnabled() { return autoRecoveryEnabled; }
    public void setAutoRecoveryEnabled(Boolean autoRecoveryEnabled) { this.autoRecoveryEnabled = autoRecoveryEnabled; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
