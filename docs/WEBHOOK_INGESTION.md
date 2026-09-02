# RecoverAI — Webhook Ingestion Specification

> **Phase**: Phase 9 — Razorpay Webhook & Payment Event Ingestion  
> **Package**: `com.recoverai.backend.webhook`, `com.recoverai.backend.service`, `com.recoverai.backend.controller`

---

## 1. Ingestion Architecture

```text
Razorpay Payment Gateway
         │
         ▼
POST /api/v1/webhooks/razorpay
         │
         ▼
Signature Verification (HMAC-SHA256)
    ├── INVALID → HTTP 400 + Audit Log (WEBHOOK_SIGNATURE_INVALID)
    │
    ▼ (VALID ONLY)
WebhookEvent Persisted (DB)
         │
         ▼
Idempotency Check (UNIQUE razorpay_event_id)
    ├── DUPLICATE → Deterministic response (WEBHOOK_DUPLICATE)
    │
    ▼ (NEW EVENT)
Event Type Router
    ├── payment.failed → Process
    ├── Other → Persist + Mark Ignored (WEBHOOK_IGNORED)
    │
    ▼ (payment.failed)
Merchant Resolution (X-Merchant-ID header)
         │
         ▼
Customer Resolution (customer_id / email from payload)
         │
         ▼
Payment Entity Created/Updated (status: FAILED)
         │
         ▼
RecoveryCase Created (status: OPEN)
         │
         ▼
[STOP — Future Recovery Orchestrator]
```

> **Core Principle**: *"A verified webhook establishes an event, not authorization to execute a recovery action."*

---

## 2. Supported Event Types

| Event Type | Supported | Behavior |
| :--- | :---: | :--- |
| `payment.failed` | ✅ YES | Creates/updates Payment, opens RecoveryCase |
| All other events | ⚠️ PERSISTED | Signature validated, event stored, marked as ignored |

### Future Expansion
Additional event types (e.g., `payment.captured`, `refund.created`) can be added by extending the event type router in `WebhookIngestionService` without modifying the signature verification or persistence layers.

---

## 3. Signature Verification

### Algorithm
- **HMAC-SHA256** computed over the raw request body bytes using `RAZORPAY_WEBHOOK_SECRET`.
- Signature provided via `X-Razorpay-Signature` HTTP header.
- Comparison uses **constant-time** `MessageDigest.isEqual()` to prevent timing attacks.

### Raw Payload Verification
- Signature is computed against the **raw HTTP request body** (not deserialized/reserialized JSON).
- This prevents signature invalidation from JSON key reordering or whitespace normalization.

### Fail-Closed Behavior
| Condition | Result |
| :--- | :--- |
| Missing `X-Razorpay-Signature` header | HTTP 400 — No processing |
| Invalid HMAC-SHA256 signature | HTTP 400 — No processing |
| Missing `RAZORPAY_WEBHOOK_SECRET` | HTTP 400 — No processing |
| Malformed JSON payload | HTTP 400 — No processing |

---

## 4. Event Persistence

Every validly signed webhook event is persisted in the existing `webhook_events` table:

| Field | Description |
| :--- | :--- |
| `razorpay_event_id` | Unique Razorpay event identifier (UNIQUE constraint) |
| `event_type` | Event type string (e.g., `payment.failed`) |
| `payload` | Full raw JSON payload |
| `signature_valid` | Boolean — always `true` for persisted events |
| `processed` | Boolean — `true` after successful processing |
| `received_at` | Timestamp of webhook receipt |
| `processed_at` | Timestamp of completed processing |

---

## 5. Idempotency

- **Database-level uniqueness** on `razorpay_event_id` prevents duplicate processing.
- First delivery: Persist → Process → Mark processed.
- Duplicate delivery: Detect via `findByRazorpayEventId()` → Return deterministic duplicate response.
- Concurrent duplicate delivery: Handled via `DataIntegrityViolationException` catch on the unique constraint.

---

## 6. Payment Mapping

For `payment.failed` events, the following fields are extracted from the Razorpay webhook payload:

| Webhook Field | Maps To |
| :--- | :--- |
| `payload.payment.entity.id` | `Payment.razorpayPaymentId` |
| `payload.payment.entity.order_id` | `Payment.razorpayOrderId` |
| `payload.payment.entity.amount` (paise) | `Payment.amount` (converted to INR) |
| `payload.payment.entity.currency` | `Payment.currency` |
| `payload.payment.entity.method` | `Payment.method` |
| `payload.payment.entity.error_code` | `Payment.failureCode` |
| `payload.payment.entity.error_description` | `Payment.failureReason` |

### Payment Idempotency
- If a Payment with the same `razorpayPaymentId` already exists, only failure-related fields are updated.
- No duplicate Payment records are created.
- `Payment.status` is set to `FAILED` — never `SUCCESS` or `CAPTURED`.

---

## 7. Merchant / Customer Resolution

### Merchant Resolution
- Resolved via `X-Merchant-ID` request header (explicit mapping).
- If merchant cannot be resolved: **FAIL CLOSED** — webhook is persisted for investigation but no Payment or RecoveryCase is created.

### Customer Resolution
- Resolved using `customer_id` or `email` from the webhook payload, scoped to the resolved merchant.
- If the customer does not exist, a minimal customer record is created using only the identifiers present in the webhook.
- No fake PII is invented.

---

## 8. RecoveryCase Creation

- A new `RecoveryCase` is created with status **`OPEN`** after successfully ingesting a failed payment.
- If a `RecoveryCase` already exists for the payment, no duplicate is created.
- The webhook handler **NEVER** transitions the case to `ANALYZING`, `ACTION_PENDING`, or `RESOLVED`.

---

## 9. Safety Boundary — Why Webhooks Cannot Execute Recovery

The webhook ingestion layer is **strictly separated** from the recovery execution pipeline:

```text
✅ Webhook Handler CAN:
   - Verify signatures
   - Persist webhook events
   - Create/update Payment records
   - Open RecoveryCase records

❌ Webhook Handler CANNOT:
   - Invoke RecoveryAnalysisService
   - Invoke RecoveryExecutionService
   - Invoke RecoveryExecutionAdapter
   - Call ML prediction service
   - Call AI Recovery Agent
   - Call PolicyEngine
   - Retry payments
   - Create payment links
   - Send customer notifications
```

Recovery analysis and execution remain explicit, separate operations triggered through their own API endpoints (`/analyze`, `/execute`) after the case has been opened.

---

## 10. Audit Events

| Event Type | When |
| :--- | :--- |
| `WEBHOOK_RECEIVED` | Validly signed event persisted |
| `WEBHOOK_SIGNATURE_INVALID` | Signature verification failed |
| `WEBHOOK_DUPLICATE` | Duplicate event ID detected |
| `WEBHOOK_PROCESSED` | Event fully processed |
| `WEBHOOK_IGNORED` | Unsupported event type stored and ignored |
| `WEBHOOK_PROCESSING_FAILED` | Merchant resolution or processing failure |
| `PAYMENT_CREATED_FROM_WEBHOOK` | New Payment record created |
| `RECOVERY_CASE_CREATED` | New RecoveryCase opened |

---

## 11. Security

- `RAZORPAY_WEBHOOK_SECRET` is **never** logged, returned in error messages, or persisted.
- Configured via environment variable `RAZORPAY_WEBHOOK_SECRET`.
- Test suite uses a deterministic test-only secret (`test_webhook_secret_key_12345`) via test `application.properties`.
- No real Razorpay API calls are made. All tests run fully offline.
- Customer payment credentials and authorization headers are never persisted.
