# RecoverAI — Deterministic Policy Engine Specification

> **Phase**: Phase 6 — Deterministic Policy Engine  
> **Package**: `com.recoverai.backend.policy`

---

## 1. Purpose & Hard Safety Boundary

The Policy Engine is the mandatory deterministic authorization gate for RecoverAI. 

### Hard Execution Boundary Architecture

```text
+---------------------+       +-------------------------+       +-------------------+       +-----------------------+
|  AI Agent / Gemini  | ----> |  ML Prediction Service  | ----> |   Policy Engine   | ----> | Financial Action /    |
| (Action Proposal)   |       |  (P(recovery) Score)    |       | (Deterministic)   |       | Payment Provider      |
+---------------------+       +-------------------------+       +-------------------+       +-----------------------+
                                                                          |
                                                                   Authorization Gate
                                                                (ALLOWED / BLOCKED / APPROVAL)
```

**Core Security Rules**:
- AI agents, LLMs, and ML models can **NEVER** directly execute or authorize a payment recovery action.
- The Policy Engine is 100% deterministic code with zero network calls, zero LLM dependencies, and zero side effects.
- Given identical inputs and merchant policy parameters, the engine produces identical authorization decisions.

---

## 2. Decision Types

| Decision Outcome | Meaning | Action Taken |
| :--- | :--- | :--- |
| `ACTION_ALLOWED` | Proposed recovery action satisfies all merchant safety rules. | Automated execution permitted. |
| `HUMAN_APPROVAL_REQUIRED` | Action exceeds automatic threshold or auto-recovery is disabled. | Queued for merchant human approval. |
| `ACTION_BLOCKED` | Action violates safety policy (e.g. max retries, low probability, permanent failure, active cooldown). | Action rejected and logged. |

---

## 3. Evaluation Precedence Order

Policy rules are evaluated in strict priority order. Safety-blocking rules take precedence over approval/allow rules:

1. **Rule 0 — Fail-Closed Safety Guard**: Rejects null/invalid inputs (`MISSING_POLICY_CONTEXT` / `INVALID_INPUT` -> `ACTION_BLOCKED`).
2. **Rule 1 — Recovery Case Eligibility**: Case status must be `OPEN`, `ANALYZING`, or `ACTION_PENDING`. Ineligible status (`RESOLVED`, `FAILED`, `STOPPED`, `ESCALATED`) -> `ACTION_BLOCKED` (`RECOVERY_CASE_NOT_ELIGIBLE`).
3. **Rule 2 — Permanent Failure Check**: Rejects unrecoverable codes/reasons (e.g., `CARD_EXPIRED`, `INVALID_ACCOUNT`, `RISK_REJECTED`) -> `ACTION_BLOCKED` (`PERMANENT_FAILURE`).
4. **Rule 3 — Auto Recovery Enabled**: If merchant auto-recovery is disabled -> `HUMAN_APPROVAL_REQUIRED` (`AUTO_RECOVERY_DISABLED`).
5. **Rule 4 — Maximum Retry Count**: If `retry_count >= max_retry_count` -> `ACTION_BLOCKED` (`MAX_RETRY_COUNT_REACHED`).
6. **Rule 5 — Minimum Recovery Probability**: If `recovery_probability < min_recovery_probability` -> `ACTION_BLOCKED` (`RECOVERY_PROBABILITY_BELOW_THRESHOLD`).
7. **Rule 6 — Cooldown Active**: If time since `last_attempt_time < cooldown_minutes` -> `ACTION_BLOCKED` (`COOLDOWN_ACTIVE`).
8. **Rule 7 — Monetary Limits**: If `payment_amount > automatic_action_limit` or `human_approval_threshold` -> `HUMAN_APPROVAL_REQUIRED` (`AMOUNT_REQUIRES_APPROVAL`).
9. **Rule 8 — Action Eligibility**: Action must be supported (`RETRY`, `PAYMENT_LINK`, `NOTIFICATION`). Unsupported -> `ACTION_BLOCKED` (`UNSUPPORTED_ACTION`).
10. **Rule 9 — All Checks Passed**: Returns `ACTION_ALLOWED` (`ALL_POLICY_CHECKS_PASSED`).

---

## 4. Input & Output Contract

### Policy Evaluation Context (`PolicyEvaluationContext`)
- `RecoveryPolicy policy` (Merchant database configuration)
- `BigDecimal recoveryProbability` (ML model prediction score)
- `BigDecimal paymentAmount` (Payment value)
- `Integer retryCount` (Current retry count)
- `RecoveryActionType proposedAction` (`RETRY`, `PAYMENT_LINK`, `NOTIFICATION`)
- `String failureCode` & `String failureReason`
- `RecoveryCaseStatus caseStatus`
- `Instant lastAttemptTime`
- `Instant evaluationTime`

### Policy Evaluation Result (`PolicyEvaluationResult`)
```json
{
  "decision": "ACTION_ALLOWED",
  "proposedAction": "RETRY",
  "reason": "ALL_POLICY_CHECKS_PASSED",
  "recoveryProbability": 0.8500,
  "paymentAmount": 2500.00,
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
```

---

## 5. Fail-Closed Security Policy

If any required context attribute is missing, ambiguous, or invalid (e.g. missing merchant policy, missing recovery probability, negative payment amount, or invalid case state), the Policy Engine **fails closed**, returning `ACTION_BLOCKED` or `HUMAN_APPROVAL_REQUIRED`. It will **never** authorize execution on incomplete context.

---

## 6. Test Coverage

Comprehensive unit tests (`com.recoverai.backend.policy.PolicyEngineTest`):
- 26 tests covering all 21 specification scenarios + fail-closed security edge cases.
- All backend tests passing: **35 total tests, 0 failures**.
