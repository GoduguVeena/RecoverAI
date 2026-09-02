package com.recoverai.backend.execution;

import com.recoverai.backend.domain.enums.RecoveryActionType;
import com.recoverai.backend.dto.ExecutionContext;
import com.recoverai.backend.dto.ExecutionResult;
import com.recoverai.backend.policy.PolicyEvaluationResult;

public interface RecoveryExecutionAdapter {
    ExecutionResult execute(RecoveryActionType action, PolicyEvaluationResult authorizationResult, ExecutionContext context);
}
