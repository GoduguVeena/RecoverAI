package com.recoverai.backend.dto;

import com.recoverai.backend.policy.PolicyEvaluationResult;
import java.util.UUID;

public class RecoveryExecutionResponse {

    private UUID caseId;
    private UUID attemptId;
    private boolean executed;
    private PolicyEvaluationResult policyDecision;
    private ExecutionResult executionResult;

    public RecoveryExecutionResponse() {
    }

    public RecoveryExecutionResponse(UUID caseId, UUID attemptId, boolean executed, PolicyEvaluationResult policyDecision, ExecutionResult executionResult) {
        this.caseId = caseId;
        this.attemptId = attemptId;
        this.executed = executed;
        this.policyDecision = policyDecision;
        this.executionResult = executionResult;
    }

    public UUID getCaseId() { return caseId; }
    public void setCaseId(UUID caseId) { this.caseId = caseId; }

    public UUID getAttemptId() { return attemptId; }
    public void setAttemptId(UUID attemptId) { this.attemptId = attemptId; }

    public boolean isExecuted() { return executed; }
    public void setExecuted(boolean executed) { this.executed = executed; }

    public PolicyEvaluationResult getPolicyDecision() { return policyDecision; }
    public void setPolicyDecision(PolicyEvaluationResult policyDecision) { this.policyDecision = policyDecision; }

    public ExecutionResult getExecutionResult() { return executionResult; }
    public void setExecutionResult(ExecutionResult executionResult) { this.executionResult = executionResult; }
}
