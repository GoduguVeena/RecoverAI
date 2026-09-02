# RecoverAI

> **Razorpay Buildathon — Track 3: AI Revenue Recovery**

RecoverAI is an AI-assisted payment recovery platform. It ingests payment failure events, predicts recovery probability with a trained ML model, generates a recovery recommendation through an AI agent, enforces it through a deterministic Policy Engine, and executes a recovery action only when all safety checks pass — always in dry-run simulation mode.

---

## Architecture

```
                 ┌─────────────────────────────────┐
                 │   React + TypeScript Dashboard   │
                 │   Operator Console (Port 3000)   │
                 └──────────────┬──────────────────┘
                                │ REST
                 ┌──────────────▼──────────────────┐
                 │   Spring Boot Backend (8080)     │
                 │                                  │
                 │  ┌─────────────────────────────┐ │
                 │  │  Razorpay Webhook Ingestion  │ │
                 │  │  HMAC-SHA256 · Idempotent   │ │
                 │  └──────────────┬──────────────┘ │
                 │                 ▼                 │
                 │  ┌─────────────────────────────┐ │
                 │  │    ML Prediction Client      │ │──► FastAPI ML (8000)
                 │  │    Recovery Probability      │ │    Logistic Regression
                 │  └──────────────┬──────────────┘ │    ROC-AUC 0.79
                 │                 ▼                 │
                 │  ┌─────────────────────────────┐ │
                 │  │    AI Recovery Agent         │ │──► Gemini API (optional)
                 │  │    Diagnosis + Recommend     │ │    FakeAgent fallback
                 │  └──────────────┬──────────────┘ │
                 │                 ▼                 │
                 │  ┌─────────────────────────────┐ │
                 │  │  Deterministic Policy Engine │ │  ← HARD SAFETY BOUNDARY
                 │  │  ALLOWED / APPROVAL / BLOCK  │ │
                 │  └──────────────┬──────────────┘ │
                 │                 ▼                 │
                 │  ┌─────────────────────────────┐ │
                 │  │ Dry-Run Execution Adapter    │ │  SIMULATION ONLY
                 │  │ No real financial actions    │ │
                 │  └─────────────────────────────┘ │
                 │                                  │
                 └──────────────┬──────────────────┘
                                │ JDBC
                 ┌──────────────▼──────────────────┐
                 │        PostgreSQL 15              │
                 │        Flyway migrations          │
                 └─────────────────────────────────┘
```

### Safety Principle

> **AI recommends. Policy Engine authorizes. Execution Adapter executes only when authorized — always in dry-run simulation.**

The Policy Engine is deterministic application code, not AI. It enforces hard rules:
- Recovery case eligibility
- Auto-recovery enabled for the merchant
- Retry limits not exceeded
- Recovery probability above threshold
- Payment amount within automated action limit
- Cooldown period respected
- No permanent failure codes

If any check fails → `ACTION_BLOCKED` or `HUMAN_APPROVAL_REQUIRED`. No execution occurs.

---

## Prerequisites

- Docker and Docker Compose v2+
- (Optional) Java 16+ and Maven 3.9+ for local backend development
- (Optional) Python 3.11+ for local ML development
- (Optional) Node 20+ for local frontend development

---

## Environment Setup

```bash
cp .env.example .env
# Edit .env — set GEMINI_API_KEY if you have one (optional)
# RAZORPAY_WEBHOOK_SECRET can be any string for local/demo use
```

---

## Run with Docker Compose

```bash
cd infra
docker compose up --build
```

All four services start in dependency order:
1. PostgreSQL (with health check)
2. ML Service (loads trained model on startup)
3. Spring Boot Backend (waits for PostgreSQL healthy, runs Flyway migrations)
4. React Frontend (served by nginx on port 3000)

### Stop

```bash
docker compose down
# To also remove database volume:
docker compose down -v
```

---

## Services

| Service | URL | Description |
| :--- | :--- | :--- |
| Frontend | http://localhost:3000 | Operator Dashboard |
| Backend | http://localhost:8080 | Spring Boot REST API |
| ML Service | http://localhost:8000 | FastAPI prediction service |
| PostgreSQL | localhost:5432 | PostgreSQL 15 |

---

## API Endpoints

### Health
```
GET  /api/v1/health
```

### Merchants
```
POST /api/v1/merchants
GET  /api/v1/merchants/{id}
GET  /api/v1/merchants
```

### Customers
```
POST /api/v1/customers
GET  /api/v1/customers/{id}
```

### Payments
```
POST /api/v1/payments
GET  /api/v1/payments/{id}
```

### Recovery Cases
```
GET  /api/v1/recovery/cases?merchantId=&status=&page=&size=
GET  /api/v1/recovery/cases/{id}
POST /api/v1/recovery/cases/{id}/analyze   ← ML + AI + Policy
POST /api/v1/recovery/cases/{id}/execute   ← Dry-run execution
```

### Webhooks
```
POST /api/v1/webhooks/razorpay
Headers: X-Razorpay-Signature, X-Merchant-ID
```

### ML Service
```
GET  /health
POST /predict
```

---

## Demo Flow

### 1. Create a Merchant

```bash
curl -X POST http://localhost:8080/api/v1/merchants \
  -H "Content-Type: application/json" \
  -d '{"name":"Demo Merchant","category":"e-commerce","autoRecoveryEnabled":true,"maxRetryAttempts":3,"cooldownMinutes":0,"minRecoveryProbabilityThreshold":0.3,"maxAutomaticRecoveryAmount":10000}'
```

Save the `id` from the response as `MERCHANT_ID`.

### 2. Create a Customer

```bash
curl -X POST http://localhost:8080/api/v1/customers \
  -H "Content-Type: application/json" \
  -d '{"merchantId":"<MERCHANT_ID>","name":"Demo Customer","email":"demo@example.com","totalTransactions":10,"successfulTransactions":8,"failedTransactions":2,"totalSpend":50000}'
```

Save `id` as `CUSTOMER_ID`.

### 3. Create a Failed Payment

```bash
curl -X POST http://localhost:8080/api/v1/payments \
  -H "Content-Type: application/json" \
  -d '{"merchantId":"<MERCHANT_ID>","customerId":"<CUSTOMER_ID>","amount":2500,"currency":"INR","status":"FAILED","method":"upi","failureCode":"PAYMENT_FAILED","failureReason":"Bank declined","retryCount":0}'
```

Save `id` as `PAYMENT_ID`.

### 4. Open Recovery Case

```bash
curl -X POST http://localhost:8080/api/v1/recovery/cases \
  -H "Content-Type: application/json" \
  -d '{"paymentId":"<PAYMENT_ID>"}'
```

Save `id` as `CASE_ID`.

### 5. Analyze (ML + AI + Policy)

```bash
curl -X POST http://localhost:8080/api/v1/recovery/cases/<CASE_ID>/analyze
```

Response includes:
- `agentDecision.modelProbability` — ML recovery probability
- `agentDecision.diagnosis` — AI agent diagnosis
- `agentDecision.selectedAction` — recommended action
- `policyDecision.decision` — `ACTION_ALLOWED` / `HUMAN_APPROVAL_REQUIRED` / `ACTION_BLOCKED`
- `policyDecision.checks` — all 9 policy check results

### 6. Execute (Dry-Run, if Policy allows)

```bash
curl -X POST http://localhost:8080/api/v1/recovery/cases/<CASE_ID>/execute
```

Response includes:
- `executed: true/false`
- `executionResult.simulated: true` ← always true — no real payment made
- `executionResult.status` — simulation status
- `policyDecision` — re-evaluated at execution time

### 7. Webhook Flow (Alternative Entry Point)

```bash
# Compute HMAC-SHA256 of payload using your RAZORPAY_WEBHOOK_SECRET
PAYLOAD='{"event":"payment.failed","payload":{"payment":{"entity":{"id":"pay_demo123","order_id":"order_demo456","amount":250000,"currency":"INR","method":"upi","error_code":"BAD_REQUEST_ERROR","error_description":"Bank declined","customer_id":"cust_demo789","email":"demo@example.com"}}}}'

SIGNATURE=$(echo -n "$PAYLOAD" | openssl dgst -sha256 -hmac "recoverai_webhook_secret_demo" -hex | awk '{print $2}')

curl -X POST http://localhost:8080/api/v1/webhooks/razorpay \
  -H "Content-Type: application/json" \
  -H "X-Razorpay-Signature: $SIGNATURE" \
  -H "X-Merchant-ID: <MERCHANT_ID>" \
  -d "$PAYLOAD"
```

Result: webhook persisted → Payment created (FAILED) → RecoveryCase opened (OPEN) → no automatic execution.

---

## Safety Guarantees

| Property | Implementation |
| :--- | :--- |
| AI never executes payments | `GeminiAgentModelClient` returns recommendations only |
| Webhooks never trigger execution | `WebhookIngestionService` creates OPEN cases only |
| Execution requires fresh policy auth | `RecoveryExecutionService` re-evaluates `PolicyEngine` at execution time |
| Execution is always simulated | `DryRunRecoveryExecutionAdapter` — `simulated: true` in every response |
| No real Razorpay API calls | `DryRunRecoveryExecutionAdapter` is the only adapter wired |
| No hardcoded secrets | All secrets via env vars with safe defaults for demo |
| Gemini failure is safe | Falls back to `FakeAgentModelClient` deterministically |
| ML failure is safe | `MlPredictionClient` falls back to 0.75 probability |

---

## Testing

### Backend

```bash
cd apps/backend
mvn clean test
# Expected: 60 tests, 0 failures
```

### ML Service

```bash
cd apps/ml-service
.venv/Scripts/pytest tests/   # Windows
# or
source .venv/bin/activate && pytest tests/   # Linux/macOS
# Expected: 18 tests, 0 failures
```

### Frontend

```bash
cd apps/frontend
npm run build
# Expected: 0 TypeScript errors, successful Vite build
```

---

## Local Development (without Docker)

### PostgreSQL
Start a local PostgreSQL instance:
```bash
# Via Docker only for DB:
docker run -d --name recoverai-postgres \
  -e POSTGRES_DB=recoverai_db \
  -e POSTGRES_USER=recoverai_user \
  -e POSTGRES_PASSWORD=recoverai_password \
  -p 5432:5432 postgres:15-alpine
```

### Backend
```bash
cd apps/backend
mvn spring-boot:run
```

### ML Service
```bash
cd apps/ml-service
.venv/Scripts/activate   # Windows
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

### Frontend
```bash
cd apps/frontend
npm run dev   # http://localhost:3000
```

---

## Environment Variables

See [`.env.example`](.env.example) for all supported configuration options.

Key variables:

| Variable | Default | Required |
| :--- | :--- | :--- |
| `POSTGRES_DB` | `recoverai_db` | For Docker |
| `POSTGRES_USER` | `recoverai_user` | For Docker |
| `POSTGRES_PASSWORD` | `recoverai_password` | For Docker |
| `GEMINI_API_KEY` | _(empty — uses fake agent)_ | Optional |
| `RAZORPAY_WEBHOOK_SECRET` | `recoverai_webhook_secret_demo` | Webhook testing |
| `RECOVERAI_EXECUTION_MODE` | `DRY_RUN` | Do not change |
