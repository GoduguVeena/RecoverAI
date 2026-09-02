# RecoverAI — Safe Recovery Execution Adapter Specification

> **Phase**: Phase 8 — Safe Recovery Execution Adapter  
> **Package**: `com.recoverai.backend.execution` & `com.recoverai.backend.service`

---

## 1. Execution Architecture & Hard Authorization Boundary

The Recovery Execution Adapter represents the financial action boundary for RecoverAI. In this phase, execution occurs **exclusively in DRY-RUN / SIMULATION mode** to verify end-to-end authorization flow without initiating live transactions or invoking external payment providers.

```text
Recovery Case
      │
      ▼
ML Prediction (FastAPI /predict)
      │
      ▼
AI Recovery Agent (Gemini / ADK)
      │
      ▼
AgentDecision (Recommendation)
      │
      ▼
Mandatory Pre-Execution PolicyRe-Evaluation (PolicyEngine)
      │
      ├── BLOCKED / HUMAN_APPROVAL_REQUIRED ──> [Execution Stopped & Logged]
      │
      ▼ (ACTION_ALLOWED ONLY)
DryRunRecoveryExecutionAdapter (Dry-Run Simulation)
      │
      ▼
RecoveryAttempt & AuditLog Persisted
```

### Critical Security Boundaries
1. **Agent Recommendation is NOT Authorization**: The adapter directly rejects calls not accompanied by a fresh `PolicyEvaluationResult` with decision `ACTION_ALLOWED`.
2. **Mandatory Pre-Execution Policy Re-Evaluation**: Immediately prior to invoking the adapter, `RecoveryExecutionService` re-runs `PolicyEngine.evaluate(...)` against real-time payment state (retry count, cooldown timers, status). If policy blocks or requires human approval, execution stops immediately.
3. **No Financial Side Effects**: Simulated execution creates a `RecoveryAttempt` record with simulated payload but **NEVER** mutates `Payment.status` to `CAPTURED` or `SUCCESS`.

> *"Policy authorization is necessary but not equivalent to successful payment recovery."*  
> *"Dry-run execution never represents a real financial transaction."*

---

## 2. Dry-Run Execution Behavior

| Action Type | Simulated Behavior | Simulated Payload Output |
| :--- | :--- | :--- |
| `RETRY` | Simulates payment gateway retry dispatch. | `{"mode":"SIMULATED_RETRY","caseId":"...","paymentId":"..."}` |
| `PAYMENT_LINK` | Simulates customer recovery link generation. | `{"mode":"SIMULATED_PAYMENT_LINK","url":"https://simulation.recoverai.internal/link/..."}` |
| `NOTIFICATION` | Simulates customer SMS/Email alert dispatch. | `{"mode":"SIMULATED_NOTIFICATION","channel":"SMS_EMAIL","caseId":"..."}` |

---

## 3. Idempotency & Failure Handling

- **Idempotency Guard**: If an identical action attempt has already been successfully simulated for a recovery case, `RecoveryExecutionService` returns the existing simulated result without creating uncontrolled duplicate executions.
- **Fail-Closed Security**: Rejects missing policy context, invalid execution modes (only `DRY_RUN` supported), missing cases, or missing actions.

---

## 4. Execution API Contract

```http
POST /api/v1/recovery/cases/{id}/execute
```

### Response (Authorized Execution)
```json
{
  "success": true,
  "data": {
    "caseId": "80c6fb3c-4419-480c-9aef-3ec61b98d7ed",
    "attemptId": "f948a201-1823-4211-9302-881b49fc1102",
    "executed": true,
    "policyDecision": {
      "decision": "ACTION_ALLOWED",
      "proposedAction": "RETRY",
      "reason": "ALL_POLICY_CHECKS_PASSED"
    },
    "executionResult": {
      "executionId": "b1049c28-9842-410a-b891-381c00fa1102",
      "action": "RETRY",
      "status": "SIMULATED",
      "message": "[SIMULATION] Dry-run payment retry simulated successfully.",
      "simulated": true,
      "simulatedPayload": "{\"mode\":\"SIMULATED_RETRY\",\"caseId\":\"80c6fb3c-4419-480c-9aef-3ec61b98d7ed\",\"paymentId\":\"3a19e201-8811-419b-9801-1b918afc9812\"}"
    }
  }
}
```

### Response (Blocked Execution)
```json
{
  "success": true,
  "data": {
    "caseId": "80c6fb3c-4419-480c-9aef-3ec61b98d7ed",
    "attemptId": "e1902091-8811-4211-9302-881b49fc9901",
    "executed": false,
    "policyDecision": {
      "decision": "ACTION_BLOCKED",
      "proposedAction": "RETRY",
      "reason": "MAX_RETRY_COUNT_REACHED"
    },
    "executionResult": null
  }
}
```
