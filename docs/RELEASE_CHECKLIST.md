# RecoverAI — Release Checklist

> Use this checklist before every submission, demo, or GitHub push.

---

## 1. Automated Tests

- [ ] **Backend**: `cd apps/backend && mvn clean test` — expect **60 passed, 0 failed**
- [ ] **ML**: `cd apps/ml-service && pytest tests/` — expect **18 passed, 0 failed**
- [ ] **Frontend**: `cd apps/frontend && npm run build` — expect **0 TypeScript errors**

---

## 2. Docker Compose Configuration

- [ ] `cd infra && docker compose config` — validates without errors
- [ ] All four services defined: `postgres`, `ml-service`, `backend`, `frontend`
- [ ] `postgres` has `healthcheck`
- [ ] `backend` has `depends_on: postgres: condition: service_healthy`
- [ ] `backend` has `ML_SERVICE_URL: http://ml-service:8000`
- [ ] `frontend` `VITE_BACKEND_URL` build arg set to `http://localhost:8080`
- [ ] `postgres_data` volume defined

---

## 3. Docker Startup

```bash
cd infra
docker compose up --build -d
```

- [ ] `recoverai-postgres` starts and becomes healthy
- [ ] `recoverai-ml-service` starts and loads model
- [ ] `recoverai-backend` starts after postgres healthy, Flyway migrations succeed
- [ ] `recoverai-frontend` starts

---

## 4. Health Checks

- [ ] `curl http://localhost:8080/api/v1/health` → `{"data":{"status":"UP",...}}`
- [ ] `curl http://localhost:8000/health` → `{"status":"UP","model_loaded":true}`
- [ ] `curl http://localhost:3000` → React dashboard loads
- [ ] Dashboard health indicators show **Online** for both backend and ML

---

## 5. End-to-End Demo

- [ ] Create merchant via API
- [ ] Create customer via API
- [ ] Create failed payment via API
- [ ] Open recovery case via API
- [ ] Analyze case → ML probability returned, AI diagnosis present, Policy decision returned
- [ ] Execute case → `simulated: true`, no real payment made, payment status remains `FAILED`
- [ ] Webhook ingestion → event persisted, RecoveryCase opened (OPEN), no auto-execution

---

## 6. Safety Verification

- [ ] `executionResult.simulated` is `true` in every execute response
- [ ] No real Razorpay API is called (no razorpay SDK in pom.xml, no real key set)
- [ ] `RECOVERAI_EXECUTION_MODE=DRY_RUN` in compose environment
- [ ] Webhook creates case in `OPEN` status only — does NOT call analyze or execute
- [ ] Policy BLOCKED → execute endpoint returns `executed: false`
- [ ] Gemini absent → `FakeAgentModelClient` used, backend still healthy

---

## 7. Secret Audit

- [ ] `.env` is NOT committed (in `.gitignore`)
- [ ] No real API keys in any source file
- [ ] No real database passwords in source files (only defaults for demo)
- [ ] `GEMINI_API_KEY` is empty or set only in local `.env`
- [ ] `git diff --stat` shows no secret files staged

```bash
git status
git diff --stat
# Check for: .env, any file containing real API keys
```

---

## 8. Git Readiness

- [ ] `.gitignore` correctly excludes: `.env`, `target/`, `.venv/`, `node_modules/`, `dist/`, `__pycache__/`
- [ ] `.gitignore` includes `!apps/ml-service/models/*.joblib` (model artifact committed)
- [ ] `git status` shows only expected files as untracked/modified
- [ ] No IDE files (`.idea/`, `.vscode/`) in staging

---

## 9. Files Required in Repository

These files must be present for a clean clone to work:

- [ ] `apps/backend/Dockerfile`
- [ ] `apps/ml-service/Dockerfile`
- [ ] `apps/frontend/Dockerfile`
- [ ] `apps/frontend/nginx.conf`
- [ ] `apps/ml-service/models/recovery_model.joblib`
- [ ] `infra/docker-compose.yml`
- [ ] `.env.example`
- [ ] `README.md`

---

## 10. GitHub Push Readiness

- [ ] All above checks passed
- [ ] Commits are clean and meaningful
- [ ] Repository is on `main` or `master` branch
- [ ] Remote is configured: `git remote -v`
- [ ] **Explicit approval received before pushing**

```bash
git remote -v
git push origin main
```

---

## Commit Template

```
feat: implement recoverai platform (phases 0-9)

- Spring Boot 2.7.18 + Java 16 backend
- PostgreSQL 15 + Flyway schema
- FastAPI ML service with Logistic Regression (ROC-AUC 0.79)
- Deterministic Policy Engine (9 safety checks)
- AI Recovery Agent (Gemini + FakeAgent fallback)
- Dry-Run Execution Adapter
- Razorpay webhook ingestion (HMAC-SHA256)
- 60 backend tests + 18 ML tests passing

feat: add react operator dashboard (phase 10)

- Dark fintech operator console
- Recovery cases table with filtering + pagination
- ML probability visualization
- Backend/ML health indicators
- Case detail route

chore: add dockerfiles, compose, and release preparation

- All three service Dockerfiles
- Fixed docker-compose with health conditions + env vars
- Updated README with demo flow
- Release checklist
```
