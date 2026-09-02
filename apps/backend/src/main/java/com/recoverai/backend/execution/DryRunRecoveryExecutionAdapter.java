package com.recoverai.backend.execution;

import com.recoverai.backend.domain.enums.PolicyDecisionOutcome;
import com.recoverai.backend.domain.enums.RecoveryActionType;
import com.recoverai.backend.dto.ExecutionContext;
import com.recoverai.backend.dto.ExecutionResult;
import com.recoverai.backend.exception.InvalidRequestException;
import com.recoverai.backend.policy.PolicyEvaluationResult;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class DryRunRecoveryExecutionAdapter implements RecoveryExecutionAdapter {

    @Override
    public ExecutionResult execute(RecoveryActionType action, PolicyEvaluationResult authorizationResult, ExecutionContext context) {
        // Fail-closed safety boundary: Enforce PolicyEngine authorization
        if (authorizationResult == null || authorizationResult.getDecision() != PolicyDecisionOutcome.ACTION_ALLOWED) {
            String decisionStr = authorizationResult != null ? authorizationResult.getDecision().name() : "NULL";
            throw new InvalidRequestException("Execution adapter rejected unauthorized call. Policy decision is " + decisionStr);
        }

        if (action == null) {
            throw new InvalidRequestException("Execution action is missing.");
        }

        if (context == null || !"DRY_RUN".equalsIgnoreCase(context.getExecutionMode())) {
            String modeStr = context != null ? context.getExecutionMode() : "NULL";
            throw new InvalidRequestException("Invalid or unsupported execution mode: " + modeStr + ". Only DRY_RUN is supported.");
        }

        UUID executionId = UUID.randomUUID();
        String message;
        String simulatedPayload;

        switch (action) {
            case RETRY:
                message = "[SIMULATION] Dry-run payment retry simulated successfully.";
                simulatedPayload = "{\"mode\":\"SIMULATED_RETRY\",\"caseId\":\"" + context.getRecoveryCaseId() + "\",\"paymentId\":\"" + context.getPaymentId() + "\"}";
                break;
            case PAYMENT_LINK:
                message = "[SIMULATION] Dry-run payment link creation simulated successfully.";
                simulatedPayload = "{\"mode\":\"SIMULATED_PAYMENT_LINK\",\"url\":\"https://simulation.recoverai.internal/link/" + UUID.randomUUID() + "\"}";
                break;
            case NOTIFICATION:
                message = "[SIMULATION] Dry-run customer notification simulated successfully.";
                simulatedPayload = "{\"mode\":\"SIMULATED_NOTIFICATION\",\"channel\":\"SMS_EMAIL\",\"caseId\":\"" + context.getRecoveryCaseId() + "\"}";
                break;
            default:
                throw new InvalidRequestException("Unsupported recovery action type: " + action);
        }

        return new ExecutionResult(executionId, action, "SIMULATED", message, true, simulatedPayload);
    }
}
