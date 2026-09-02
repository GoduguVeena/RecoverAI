# RecoverAI — AI Recovery Agent Specification

> **Phase**: Phase 7 — AI Recovery Agent  
> **Package**: `com.recoverai.backend.agent` & `com.recoverai.backend.service`

---

## 1. Agent Purpose & Architecture

The AI Recovery Agent evaluates failed transaction recovery context, consumes ML-predicted recovery probability, diagnoses payment failure causes, and recommends optimal recovery interventions.

### Hard Safety Execution Flow

```text
Payment Failure Event
        │
        ▼
ML Prediction Service (FastAPI /predict)
  └── P(recovery) Score: 0.8585
        │
        ▼
AI Recovery Agent (Gemini / ADK)
  └── AgentDecision Recommendation: RETRY
        │
        ▼
Agent Output Validation (AgentOutputValidator)
  └── Enforces Schema & Supported Action Enums
        │
        ▼
AgentDecision Entity Persisted (DB)
        │
        ▼
Deterministic PolicyEngine (Authorization Gate)
  └── Checks Max Retries, Cooldown, Limits
        │
        ▼
PolicyDecision: ACTION_ALLOWED / HUMAN_APPROVAL_REQUIRED / ACTION_BLOCKED
        │
        ▼
Future Execution Adapter (Phase 8+)
```

> **Core System Law**: *"The agent recommends; the Policy Engine authorizes; a future execution adapter executes."*

---

## 2. Tool & Execution Boundaries

| Agent Capability | Authorized? | Description |
| :--- | :---: | :--- |
| Read Payment & Customer Context | ✅ YES | Accesses historical transaction counts, failure codes, and merchant category. |
| Consume ML Recovery Probability | ✅ YES | Uses `P(recovery)` from Phase 5 FastAPI ML service. |
| Diagnose Payment Failures | ✅ YES | Generates operational diagnostic summaries. |
| Recommend Recovery Action | ✅ YES | Recommends among supported domain actions (`RETRY`, `PAYMENT_LINK`, `NOTIFICATION`). |
| Execute Financial Actions | ❌ **FORBIDDEN** | Cannot initiate retries, call Razorpay, or issue links. |
| Authorize Actions | ❌ **FORBIDDEN** | PolicyEngine remains sole authorization gate. |
| Mutate Merchant Policy | ❌ **FORBIDDEN** | Policy thresholds belong exclusively to application DB. |
| Access Financial Credentials | ❌ **FORBIDDEN** | Agent holds zero Razorpay or payment provider keys. |

---

## 3. Agent Input & Output Contracts

### Input Context (`AgentRequestContext`)
Contains only pre-decision features available at decision time:
- `recoveryCaseId`, `paymentId`
- `amount`, `currency`, `paymentMethod`
- `failureCode`, `failureReason`
- `retryCount`
- `customerTotalTransactions`, `customerSuccessfulTransactions`, `customerFailedTransactions`
- `customerSuccessRate`, `daysSinceLastSuccess`
- `merchantCategory`, `customerSegment`
- `recoveryProbability`, `modelVersion`

### Structured Output (`AgentRecommendationResponse`)
```json
{
  "modelVersion": "recovery-logistic-v1",
  "recoveryProbability": 0.8585,
  "diagnosis": "Transient gateway timeout with strong customer payment history.",
  "candidateActions": ["RETRY", "PAYMENT_LINK", "NOTIFICATION"],
  "selectedAction": "RETRY",
  "reasoningSummary": "High recovery probability (85.85%) and transient timeout error indicate RETRY is the optimal recovery intervention."
}
```

---

## 4. PolicyEngine Authorization Boundary

After the agent produces a validated `AgentRecommendationResponse`, the Spring Boot orchestrator passes the recommended action and transaction details to the deterministic `PolicyEngine`.

If the agent recommends `RETRY` but `PolicyEngine` evaluates `ACTION_BLOCKED` (e.g. `MAX_RETRY_COUNT_REACHED` or `COOLDOWN_ACTIVE`), the final system outcome **remains blocked**. The AI agent can **never** override `PolicyEngine`.

---

## 5. Persistence & Database Schema

The recommendation is persisted in `agent_decisions` (`AgentDecision` entity):
- `recovery_case_id`: Link to parent `RecoveryCase`
- `model_version`: ML model identifier (`recovery-logistic-v1`)
- `model_probability`: Decimal prediction score (`0.8585`)
- `diagnosis`: Operational diagnostic string
- `candidateActions`: Array of candidate actions (`["RETRY", "PAYMENT_LINK", "NOTIFICATION"]`)
- `selectedAction`: Recommended `RecoveryActionType` (`RETRY`)
- `reasoningSummary`: Concise explanation string
- `policy_checks`: JSON string of PolicyEngine check details

---

## 6. Failure Handling & Offline Execution

- **Gemini API Unavailability / Missing Key**: `GeminiAgentModelClient` falls back safely to deterministic `FakeAgentModelClient`. Automated tests run 100% offline without live credentials.
- **ML Service Unavailability**: Handled safely without fabricating invalid probabilities.
- **Malformed Agent Output**: Rejection by `AgentOutputValidator` throwing `AgentException` (`HTTP 422`).

---

## 7. Analysis REST API Contract

```http
POST /api/v1/recovery/cases/{id}/analyze
```

### Response
```json
{
  "success": true,
  "data": {
    "caseId": "80c6fb3c-4419-480c-9aef-3ec61b98d7ed",
    "agentDecision": {
      "id": "e391b1a7-47b1-419b-9801-1b918afc9812",
      "modelVersion": "recovery-logistic-v1",
      "modelProbability": 0.8585,
      "diagnosis": "Transient gateway timeout with strong customer payment history.",
      "candidateActions": "[RETRY, PAYMENT_LINK, NOTIFICATION]",
      "selectedAction": "RETRY",
      "reasoningSummary": "High recovery probability (85.85%) indicates immediate automated RETRY.",
      "createdAt": "2026-08-30T19:12:07.508Z"
    },
    "policyDecision": {
      "decision": "ACTION_ALLOWED",
      "proposedAction": "RETRY",
      "reason": "ALL_POLICY_CHECKS_PASSED",
      "recoveryProbability": 0.8585,
      "paymentAmount": 2500.0000,
      "checks": {
        "recoveryCaseEligible": true,
        "autoRecoveryEnabled": true,
        "retryLimitPassed": true,
        "probabilityThresholdPassed": true,
        "permanentFailureCheckPassed": true,
        "cooldownPassed": true,
        "amountWithinAutomaticLimit": true,
        "humanApprovalThresholdCheckPassed": true,
        "actionSupported": true
      }
    }
  }
}
```
