# RecoverAI — Architecture & Safety Boundaries Specification

## Hard Execution Boundary

```text
AI Agent
   ↓
Policy Engine
   ↓
Action Authorization
   ↓
Payment Provider
```

### Critical Rule
The AI Agent must **NEVER** directly execute a financial or payment action.

The **Policy Engine** is deterministic application code and serves as the mandatory, un-bypassable authorization layer before any recovery intervention is initiated.

---

## Eventual Recovery Lifecycle

Every revenue recovery workflow follows a 9-stage sequence:

```text
1. Revenue-at-risk detection
       ↓
2. Failure diagnosis
       ↓
3. Recovery probability prediction
       ↓
4. Candidate intervention generation
       ↓
5. Expected recovery evaluation
       ↓
6. Deterministic policy validation
       ↓
7. Action execution
       ↓
8. Outcome verification
       ↓
9. Audit + metrics
```

---

## Core Safety Concepts

1. **Maximum Retry Count**: Enforces a hard numerical cap on payment retry attempts per transaction to prevent customer harassment or card network penalties.
2. **Minimum Recovery Probability**: Requires predicted recovery probability \(P(\text{recovery} \mid \text{context})\) to meet a minimum merchant-configured threshold prior to triggering automated retries.
3. **Automatic Action Amount Limit**: High-value transactions exceeding the automated execution cap are automatically diverted to manual review.
4. **Human Approval Threshold**: Requires merchant intervention for cases flagged as high risk, high value, or low confidence.
5. **Permanent-Failure Restrictions**: Hard-blocks automated retries for unrecoverable failure reasons (e.g., stolen cards, invalid accounts, fraudulent attempts).
6. **Duplicate-Action Prevention**: Prevents concurrent or duplicate recovery attempts on the same case.
7. **Stopping and Escalation Rules**: Defines explicit conditions under which recovery attempts cease or escalate to merchant operators.
8. **Auditability**: Every decision, diagnosis, policy check result, and execution state produces an immutable audit record.
9. **Idempotency**: All financial actions, API requests, and webhooks require idempotency keys to ensure processing exactly once.
