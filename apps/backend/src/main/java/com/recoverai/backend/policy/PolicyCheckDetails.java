package com.recoverai.backend.policy;

public class PolicyCheckDetails {

    private boolean recoveryCaseEligible;
    private boolean autoRecoveryEnabled;
    private boolean retryLimitPassed;
    private boolean probabilityThresholdPassed;
    private boolean permanentFailureCheckPassed;
    private boolean cooldownPassed;
    private boolean amountWithinAutomaticLimit;
    private boolean humanApprovalThresholdCheckPassed;
    private boolean actionSupported;

    public PolicyCheckDetails() {
    }

    public boolean isRecoveryCaseEligible() { return recoveryCaseEligible; }
    public void setRecoveryCaseEligible(boolean recoveryCaseEligible) { this.recoveryCaseEligible = recoveryCaseEligible; }

    public boolean isAutoRecoveryEnabled() { return autoRecoveryEnabled; }
    public void setAutoRecoveryEnabled(boolean autoRecoveryEnabled) { this.autoRecoveryEnabled = autoRecoveryEnabled; }

    public boolean isRetryLimitPassed() { return retryLimitPassed; }
    public void setRetryLimitPassed(boolean retryLimitPassed) { this.retryLimitPassed = retryLimitPassed; }

    public boolean isProbabilityThresholdPassed() { return probabilityThresholdPassed; }
    public void setProbabilityThresholdPassed(boolean probabilityThresholdPassed) { this.probabilityThresholdPassed = probabilityThresholdPassed; }

    public boolean isPermanentFailureCheckPassed() { return permanentFailureCheckPassed; }
    public void setPermanentFailureCheckPassed(boolean permanentFailureCheckPassed) { this.permanentFailureCheckPassed = permanentFailureCheckPassed; }

    public boolean isCooldownPassed() { return cooldownPassed; }
    public void setCooldownPassed(boolean cooldownPassed) { this.cooldownPassed = cooldownPassed; }

    public boolean isAmountWithinAutomaticLimit() { return amountWithinAutomaticLimit; }
    public void setAmountWithinAutomaticLimit(boolean amountWithinAutomaticLimit) { this.amountWithinAutomaticLimit = amountWithinAutomaticLimit; }

    public boolean isHumanApprovalThresholdCheckPassed() { return humanApprovalThresholdCheckPassed; }
    public void setHumanApprovalThresholdCheckPassed(boolean humanApprovalThresholdCheckPassed) { this.humanApprovalThresholdCheckPassed = humanApprovalThresholdCheckPassed; }

    public boolean isActionSupported() { return actionSupported; }
    public void setActionSupported(boolean actionSupported) { this.actionSupported = actionSupported; }
}
