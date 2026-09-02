package com.recoverai.backend.dto;

import com.recoverai.backend.domain.enums.RecoveryActionType;
import java.util.UUID;

public class ExecutionResult {

    private UUID executionId;
    private RecoveryActionType action;
    private String status;
    private String message;
    private boolean simulated;
    private String simulatedPayload;

    public ExecutionResult() {
    }

    public ExecutionResult(UUID executionId, RecoveryActionType action, String status, String message, boolean simulated, String simulatedPayload) {
        this.executionId = executionId;
        this.action = action;
        this.status = status;
        this.message = message;
        this.simulated = simulated;
        this.simulatedPayload = simulatedPayload;
    }

    public UUID getExecutionId() { return executionId; }
    public void setExecutionId(UUID executionId) { this.executionId = executionId; }

    public RecoveryActionType getAction() { return action; }
    public void setAction(RecoveryActionType action) { this.action = action; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public boolean isSimulated() { return simulated; }
    public void setSimulated(boolean simulated) { this.simulated = simulated; }

    public String getSimulatedPayload() { return simulatedPayload; }
    public void setSimulatedPayload(String simulatedPayload) { this.simulatedPayload = simulatedPayload; }
}
